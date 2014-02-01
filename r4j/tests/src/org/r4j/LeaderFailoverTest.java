package org.r4j;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import org.r4j.example.RespondingCommitHandler;
import org.r4j.executor.SequentialExecutor;
import org.r4j.protocol.AppendRequest;
import org.r4j.protocol.AppendResponse;
import org.r4j.protocol.ClientRequest;
import org.r4j.protocol.ClientResponse;
import org.r4j.protocol.MessageChannel;
import org.r4j.protocol.RequestForVote;
import org.r4j.protocol.VoteGranted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class LeaderFailoverTest {

	Logger logger;
	
	private int delay = 25;
	
	private ScheduledExecutorService executor = new SequentialExecutor(new ScheduledThreadPoolExecutor(10));

	private Random random = new Random();
	List<Member> members = new ArrayList<>();
	private AtomicInteger successCounter = new AtomicInteger();
	private AtomicInteger errCounter = new AtomicInteger();
	long nextFail = System.currentTimeMillis() + random.nextInt(2000);
	
	@Test
	public void test() throws InterruptedException {
		System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
		System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss.SSS");
		System.setProperty("org.slf4j.simpleLogger.logFile", "raft.log");
		logger = LoggerFactory.getLogger(getClass());
		for (int i = 0; i < 3; i++) {
			Raft r = new Raft();
			r.setId("" + i);
			r.setCommitHandler(new HashSetCommits());
			RaftEventQueue q = new RaftEventQueue(r, executor);
			Member ms = new Member(q);
			members.add(ms);
		}
		
		for (Member m : members) {
			List<ClusterMember> remoteMembers = new ArrayList<>();
			for (Member remoteMember : members) {
				if (m != remoteMember) {
					RemoteStub ms = new RemoteStub(remoteMember.raftListener);
					remoteMembers.add(ms);
					m.remotes.add(ms);
				}
			}
			m.raftListener.setMembers(remoteMembers);
		}
		for (Member m : members) {
			for (RemoteStub rs : m.remotes) {
				for (Member rm : members) {
					if (m == rm) {
						continue;
					}
					if (rm.raftListener == rs.raftListener) {
						for (RemoteStub rrs : rm.remotes) {
							if (rrs.raftListener == m.raftListener) {
								rs.routeBack = rrs;
							}
						}
						
					}
					
				}
			}
		}
		for (Member m : members) {
			m.raftListener.init();
		}

		Member leader = null;
		List<Request> requests = new ArrayList<>();
		int i = 0;
		for (i = 0; i < 1; i++) {
			leader = getLeader(leader);
			
			if (leader == null) {
				i--;
				Thread.sleep(1);
				continue;
			}
			Request r = new Request();
			requests.add(r);
			leader.raftListener.handleClientRequest(r, new ClientRequest(r.payload));
			Thread.sleep(leader.raftListener.getQueueSize()+1);
			if (i % 100 == 0) {
				System.out.println("Done: " + i + " " + successCounter.get() + " " + errCounter.get());				
			}
		}
		
		Member oldLeader = null;
		for (Member m : members) {
			if (m.raftListener.getRaft().getRole() == Role.LEADER) {
				m.raftListener.stop();
				oldLeader = m;
			}
		}
		Member curLeader = getLeader(null);
		while (curLeader == null || curLeader == oldLeader) {
			Thread.sleep(1000);
			curLeader = getLeader(null);
		}
		System.out.println("Done: " + i + " " + successCounter.get() + " " + errCounter.get());				
		leader = getLeader(null);
		HashSetCommits commits = (HashSetCommits) ((LogImpl)leader.raftListener.getRaft().getLog()).getHandler();
		for (Request r : requests) {
			assert  r.err != 0 || commits.commitSet.contains(r.payload);
		}
	}

	private class Request implements MessageChannel {

		Object payload = new Object();
		int err = -1;
		
		@Override
		public void send(Raft r, Object o) {
			ClientResponse resp = (ClientResponse)o;
			err = resp.getErrCode();
			if (err == 0) {
				successCounter.incrementAndGet();
			} else {
				errCounter.incrementAndGet();
			}
		}
		
	}
	
	private static class HashSetCommits extends RespondingCommitHandler {
		private Set<Object> commitSet = new HashSet<>();

		@Override
		protected Object commitImpl(AppendRequest entry) {
			commitSet.add(entry.getPayload());
			return super.commitImpl(entry);
		}
		
	}
	
	private Member getLeader(Member leader) {
		if (leader != null && leader.raftListener.getRaft().getRole() == Role.LEADER) {
			return leader;
		}
		for (Member m : members) {
			if(m.raftListener.getRaft().getRole() == Role.LEADER) {
				return m;
			}
		}
		return null;
	}

	private class RemoteStub implements ClusterMember {

		private long lastCmd = System.currentTimeMillis();
		private RemoteStub routeBack;

		private RaftEventQueue raftListener;
		
		private MessageChannel channel = new ChannelStub();

		private long index;
		private long match;
		
		public RemoteStub(RaftEventQueue raftListener) {
			super();
			this.raftListener = raftListener;
		}

		@Override
		public long getLastCommandReceived() {
			return lastCmd;
		}
		@Override
		public void setLastcommandReceived(long time) {
			this.lastCmd = time;
		}

		@Override
		public MessageChannel getChannel() {
			return channel;
		}

		@Override
		public long getNextIndex() {
			return index;
		}

		@Override
		public void setNextIndex(long index) {
			this.index = index;
		}

		@Override
		public long getMatchIndex() {
			return match;
		}

		@Override
		public void setMatchIndex(long matchIndex) {
			this.match = matchIndex;
		}
		
		private class ChannelStub implements MessageChannel {

			@Override
			public void send(final Raft source, final Object o) {
				logger.info ("Sending: " + o + " to " + raftListener.getRaft());
				
				if (o instanceof AppendRequest) {
					AppendRequest r  = (AppendRequest) o;
					final AppendRequest req = new AppendRequest(r.getLogTerm(), r.getIndex(),
							r.getLeaderTerm(), r.getPreviousIndex(),
							r.getPreviousTerm(), r.getCommitIndex(),
							r.getPayload());
					executor.schedule(new Runnable() {
						@Override
						public void run() {
							raftListener.append(routeBack, req);
						}
					}, random.nextInt(delay) + 1, TimeUnit.MILLISECONDS);
				} else if (o instanceof AppendResponse) {
					AppendResponse resp = (AppendResponse) o;
					raftListener.appendResponse(routeBack, resp);
				} else if (o instanceof RequestForVote) {
					executor.schedule(new Runnable() {
						@Override
						public void run() {
							raftListener.vote(routeBack, (RequestForVote) o);
						}
					}, random.nextInt(delay) + 1, TimeUnit.MILLISECONDS);					
				} else if (o instanceof VoteGranted) {
					VoteGranted r = (VoteGranted) o;
					raftListener.voteReceived(routeBack, r);
				} else {
					System.err.println("Unknown cmd: " + o);
				}
			}
			
		}
		
	}
	private class Member {
		private RaftEventQueue raftListener;
		private List<RemoteStub> remotes = new ArrayList<>(); 
		public Member(RaftEventQueue queue) {
			super();
			this.raftListener = queue;
		}
		
	}
	
}
