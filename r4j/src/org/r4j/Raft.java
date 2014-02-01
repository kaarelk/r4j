package org.r4j;

import java.util.ArrayList;
import java.util.List;

import org.r4j.protocol.AppendRequest;
import org.r4j.protocol.AppendResponse;
import org.r4j.protocol.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class Raft {

	
	private Logger logger = LoggerFactory.getLogger(getClass());

	private List<ClusterMember> members = new ArrayList<ClusterMember>();

	private Role role = Role.FOLLOWER;
	
	private Term term = new Term();

	private Log log = new LogImpl(term);
	
	private ElectionLogic election = new ElectionLogic(this);

	private String id;
	
    public void setMembers(List<? extends ClusterMember> m) {
    	this.members.clear();
    	this.members.addAll(m);
    }
    
	
	/**
	 * Maintenance loop. Returns the time the loop should be executed again
	 * @return timeout for next run or -1L for default timeout
	 */
	public long loop() {
		switch (role) {
		case CANDIDATE:
			return getElection().candidateLoop();
		case FOLLOWER:
			return getElection().followLoop();
		case LEADER:
			return leaderLoop();
		}
		return -1L;
	}

	long leaderLoop() {
		for (ClusterMember m : members) {
			if (m.getMatchIndex() < log.getLastIndex() && System.currentTimeMillis() - m.getLastCommandReceived() > ElectionLogic.DEFAULT_STALE_MEMBER_TIMEOUT) {
				logger.info("Filling backlog for: " + m + " " + m.getNextIndex() + " " + log.getLastIndex());
				m.setNextIndex(log.getLastIndex());
				m.getChannel().send(this, copy(log.get(m.getNextIndex())));
				m.setLastcommandReceived(System.currentTimeMillis());//start backlog process only once per timeout
			} else {
				//ping
				if (log.getLastIndex() < 0) {
					m.getChannel().send(this, new AppendRequest(0L, 0L, term.getCurrent(), 0L, 0L, 0L, null));
				} else {
					AppendRequest ar  = log.get(log.getLastIndex());
					m.getChannel().send(this, 
							new AppendRequest(ar.getLogTerm(), ar.getIndex(), term.getCurrent(),
									ar.getPreviousIndex(),
									ar.getPreviousTerm(), log
											.getLastCommitIndex(), null));
					
				}
			}
		}
		return -1L;
	}


	private AppendRequest copy (AppendRequest ar) {
		return new AppendRequest(ar.getLogTerm(), ar.getIndex(), term.getCurrent(),
				ar.getPreviousIndex(), ar.getPreviousTerm(), log.getLastCommitIndex(), ar.getPayload());
	}
	
	void changeRole(Role role) {
		logger.info("New role: " + role);
		this.role = role;
	}
	
	public boolean append(AppendRequest entry) {
		logger.info("append: " + entry);				
		if (role == Role.FOLLOWER) {
			this.getElection().setLeaderTimestamp(System.currentTimeMillis());
			return log.append(entry);
		} else if (role == Role.LEADER && entry.getLeaderTerm() > term.getCurrent()) {
			getElection().setFollower();
		} else if (role == Role.CANDIDATE && entry.getLeaderTerm() >= term.getCurrent()) {
			getElection().setFollower();
		}
		if (term.getCurrent() < entry.getLeaderTerm()) {
			term.setCurrent(entry.getLeaderTerm());
		}
		return false;//TODO do we need to respond in this case?
	}
	

	public Term getTerm() {
		return term;
	}

	public void handleClientRequest(MessageChannel channel, Object event) {
		if (role == Role.LEADER) {
			AppendRequest req = new AppendRequest(term.getCurrent(),
					log.getLastIndex()+1, 
					term.getCurrent(), 
					log.getLastIndex(), 
					log.getLastTerm(), 
					log.getLastCommitIndex(), 
					event, 
					channel);
			
			if (!log.append(req)) {
				//problem with statemachine
				throw new IllegalStateException("Log didn't accept new entry: " + req + ": " + log);
			}
			for (ClusterMember m : members) {
				if (m.getNextIndex() == req.getIndex()) {
					m.setNextIndex(m.getNextIndex()+1);//increment optimistically to minimize retries
					m.getChannel().send(this, req);
				}
			}
		} else {
			throw new NotLeaderException();//TODO add redirect leader
		}
	}

	public void handleResponse(ClusterMember member, AppendResponse event) {
		if (role != Role.LEADER) {
			return;
		}
		if (term.getCurrent() < event.getCurrentTerm()) {
			term.setCurrent(event.getCurrentTerm());//TODO is it needed?
			election.setFollower();
			return;
		}
		
		member.setLastcommandReceived(System.currentTimeMillis());
		
		if (event.isSuccess()) {
			if (member.getNextIndex() <= event.getEntryIndex()) {
				member.setNextIndex(event.getEntryIndex() + 1);	
			}
			if (member.getMatchIndex() < event.getEntryIndex()) {
				member.setMatchIndex(event.getEntryIndex());
				
				if (event.getEntryTerm() == term.getCurrent()) {
					if (log.getLastCommitIndex() < event.getEntryIndex())  {
						//TODO optimize to gather info using O(N) instead of O(N*N)
						long newCommit = getNextCommitIndex(log.getLastCommitIndex());
						if (newCommit > log.getLastCommitIndex()) {
							log.commit(newCommit);
						}
					}
				} 
			}
			if (member.getNextIndex() <= log.getLastIndex()) {
				member.getChannel().send(this, copy(log.get(member.getNextIndex())));				
			}
		} else {
			member.setNextIndex(member.getNextIndex() - 1);
			if (member.getNextIndex() < 0L) {
				member.setNextIndex(0L);
			}
			if (member.getNextIndex() <= log.getLastIndex()) {
				member.getChannel().send(this, copy(log.get(member.getNextIndex())));
			}
		}
	}

	private long getNextCommitIndex(long currentAgreed) {
		int votesForNext = 0;
		votesForNext++;//my vote
		for (ClusterMember m : members) {
			if (m.getMatchIndex() > currentAgreed) {
				votesForNext++;
			}
		}
		if ((members.size() + 1)/2 < votesForNext) {
			return getNextCommitIndex(currentAgreed+1);
		}
		return currentAgreed;
	}

	
	
	public String getId() {
		return id;
	}


	public void setId(String id) {
		this.id = id;
	}


	public void setCommitHandler(CommitHandler cm) {
		log.setCommitHandler(cm);
	}

	public Role getRole() {
		return role;
	}

	public Log getLog() {
		return log;
	}

	public List<ClusterMember> getMembers() {
		return members;
	}


	public ElectionLogic getElection() {
		return election;
	}


	@Override
	public String toString() {
		return "Raft [" + (role != null ? "role=" + role + ", " : "")
				+ (id != null ? "id=" + id : "") + "]";
	}

	
}
