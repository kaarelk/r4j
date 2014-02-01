package org.r4j;

import java.util.List;
import java.util.concurrent.Executor;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.r4j.executor.Handler;
import org.r4j.executor.QueueTask;
import org.r4j.protocol.AppendRequest;
import org.r4j.protocol.AppendResponse;
import org.r4j.protocol.ClientRequest;
import org.r4j.protocol.ClientResponse;
import org.r4j.protocol.MessageChannel;
import org.r4j.protocol.RaftEvent;
import org.r4j.protocol.RequestForVote;
import org.r4j.protocol.VoteGranted;
import org.r4j.protocol.RaftEvent.EventType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class RaftEventQueue implements RaftListener, Handler<RaftEvent> {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private Raft raft;

	private ScheduledExecutorService executor;

	private QueueTask<RaftEvent> queue = new QueueTask<>(this);

	private LoopEvent loopEvent = new LoopEvent();
	
	private boolean running = true;

	public RaftEventQueue(Raft raft, ScheduledExecutorService executor) {
		super();
		this.raft = raft;
		this.executor = executor;
	}

	public void setMembers(List<ClusterMember> remoteMembers) {
		raft.setMembers(remoteMembers);
	}

	public void init() {
		running = true;
		loop(1L);
	}

	private void loop(long l) {
		if (running) {
			if (loopEvent.scheduleCounter.get() < 1) {
				loopEvent.scheduleCounter.incrementAndGet();
				executor.schedule(loopEvent, l, TimeUnit.MILLISECONDS);				
			} else {
				logger.error("did not schedule loop for: " + raft);
			}
		}
	}

	@Override
	public void handleEvent(RaftEvent e) {
		if (!running) {
			if (e.getType() == EventType.CLIENT_REQUEST) {
				e.getClientSource().send(raft, new ClientResponse(301, null));
			}
			return;
		}
		switch (e.getType()) {
		case APPEND:
			AppendRequest ar = (AppendRequest) e.getEvent();
			boolean appended = raft.append(ar);
			if (ar.getPayload() != null) {
				logger.info("appended: " + appended + " i=" + ar.getIndex() + " t=" + ar.getLogTerm() + " " + raft);
				e.getSource().getChannel()
					.send(raft, new AppendResponse(raft.getTerm().getCurrent(), appended, ar.getIndex(), ar.getLogTerm()));
			} else {
				logger.info("ping received: i=" + ar.getIndex() + " t=" + ar.getLeaderTerm() + " " + raft);
			}
			break;
		case APPEND_RESPONSE:
			raft.handleResponse(e.getSource(), (AppendResponse)e.getEvent());
			logger.info("append responded: " + e.getEvent() + " " + raft);
			break;
		case REQUEST_VOTE:
			RequestForVote rfv = (RequestForVote) e.getEvent();
			if (raft.getElection().vote(e.getSource(), rfv)) {
				logger.info("Voted for: " + rfv);
				e.getSource().getChannel().send(raft, new VoteGranted(rfv.getTerm()));
			} else {
				logger.info("Vote declined: " + rfv);
			}
			break;
		case VOTE_GRANTED:
			raft.getElection().voteReceived((VoteGranted) e.getEvent());
			break;
		case LOOP:
			long l = raft.loop();
			if (l < 1) {
				l = ElectionLogic.PING_LOOP;
			} else if (l > ElectionLogic.PING_LOOP) {
				l = ElectionLogic.PING_LOOP;
			}
			loop(l);
			break;
		case CLIENT_REQUEST:
			try {
				raft.handleClientRequest(e.getClientSource(), e.getEvent());
			} catch (NotLeaderException ex) {
				logger.warn("Not a leader: " + e + ": " + ex, ex);
				e.getClientSource().send(raft, new ClientResponse(301, null));
			}
			break;
		}
	}

	
	@Override
	public void voteReceived(ClusterMember member, VoteGranted vote) {
		queue.getQueue().add(
				new RaftEvent(member, EventType.VOTE_GRANTED, vote));
		executor.execute(queue);
	}

	@Override
	public void append(ClusterMember source, AppendRequest entry) {
		queue.getQueue().add(new RaftEvent(source, EventType.APPEND, entry));
		executor.execute(queue);

	}

	@Override
	public void vote(ClusterMember clusterMember, RequestForVote vote) {
		queue.getQueue().add(
				new RaftEvent(clusterMember, EventType.REQUEST_VOTE, vote));
		executor.execute(queue);
	}


	public void handleClientRequest(MessageChannel client, ClientRequest req)  {
		//TODO 2 queues needed, otherwise client requests can do DOS for cluster
		queue.getQueue().add(new RaftEvent(client, EventType.CLIENT_REQUEST, req.getPayload()));
		executor.execute(queue);
	}
	
	
	
	@Override
	public void appendResponse(ClusterMember source, AppendResponse resp) {
		queue.getQueue().add(new RaftEvent(source, EventType.APPEND_RESPONSE, resp));
		executor.execute(queue);		
	}

	public void stop() {
		running = false;
	}

	public boolean isRunning() {
		return running;
	}

	public Raft getRaft() {
		return raft;
	}

	public int getQueueSize() {
		return queue.getQueue().size();
	}


	
	@Override
	public String getName() {
		return "(" + raft + ")" ;
	}



	private class LoopEvent implements Runnable {
		
		private AtomicInteger scheduleCounter = new AtomicInteger();
		
		@Override
		public void run() {
			scheduleCounter.decrementAndGet();
			queue.getQueue().add(new RaftEvent((ClusterMember)null, EventType.LOOP, null));
			executor.execute(queue);
		}
		
	}

}
