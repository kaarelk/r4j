package org.r4j;

import java.util.Random;

import org.r4j.protocol.RequestForVote;
import org.r4j.protocol.VoteGranted;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class ElectionLogic {
	
	public static long DEFAULT_LEADER_TIMEOUT = 10000L;
	
	public static long DEFAULT_ELECTION_TIMEOUT = 10000L;
	
	public static long PING_LOOP = 1000L;
	
	public static long DEFAULT_STALE_MEMBER_TIMEOUT = DEFAULT_LEADER_TIMEOUT;
	
	private Random random = new Random();
	
	private Logger logger = LoggerFactory.getLogger(Raft.class);
	
	private Election election;

	protected long votedForTerm = -1;

	private long leaderTimestamp = System.currentTimeMillis();
	
	private long leaderTimeout = DEFAULT_LEADER_TIMEOUT;
	
    private long electionTime = -1L;
    
    private long electionEnd = -1L;

    private Raft raft;
    
    public ElectionLogic(Raft raft) {
		super();
		this.raft = raft;
	}


	public void voteReceived(VoteGranted vote) {
		logger.info("vote received: " + vote + " " + raft);
		if (raft.getRole() != Role.CANDIDATE) {
			return;
		}
		if (vote.getTerm() == raft.getTerm().getCurrent()) {
			election.incrementVotes();
			if (election.hasWon()) {
				election = null;
				raft.changeRole(Role.LEADER);
				logger.info("Becoming leader: " + raft + " in term=" + raft.getTerm().getCurrent());
				System.out.println("Becoming leader: " + raft + " in term=" + raft.getTerm().getCurrent());
				initLeader();
			}
		} else {
			logger.info("Vote received for old election: " + vote);
		}
	}
	

	public void setElectionTime(long electionTime) {
		logger.info("Setting new electiontime: " + electionTime + " " + raft);				
		this.electionTime = electionTime;
	}


	void setFollower() {
		setLeaderTimestamp(System.currentTimeMillis());
		electionEnd = -1L;
		setElectionTime(-1L);
		election = null;
		raft.changeRole(Role.FOLLOWER);
	}


	private void initLeader() {
		for (ClusterMember m : raft.getMembers()) {
			m.setNextIndex(raft.getLog().getLastIndex() + 1);
			m.setMatchIndex(0L);
			m.setLastcommandReceived(System.currentTimeMillis());//new leader didn't receive any command, don't fill backlog write away
		}
		raft.leaderLoop();//starts sending log
	}


	public long followLoop() {
		if (electionTime > 0L) {
			return tryStartElection();
		}
		long time = System.currentTimeMillis();
		if (leaderTimeout < (time - leaderTimestamp)) {
			raft.changeRole(Role.CANDIDATE);
			electionEnd = -1L;
			long rdiff = random.nextInt((int)DEFAULT_ELECTION_TIMEOUT) + 1;
			setElectionTime(time + rdiff);
			return electionTime - time;
		}
		return -1L;
	}
	private long tryStartElection() {
		long time = System.currentTimeMillis();
		if (electionTime <= time) {
			setElectionTime(-1L);
			this.raft.getTerm().newTerm();
			this.election = new Election(raft.getMembers().size() + 1, this.raft.getTerm().getCurrent());
			for (ClusterMember m : raft.getMembers()) {
				m.getChannel().send(raft, new RequestForVote(this.raft.getTerm().getCurrent(), raft.getLog().getLastIndex(), raft.getLog().getLastTerm()));
			}
			if (vote(null, new RequestForVote(this.raft.getTerm().getCurrent(), raft.getLog().getLastIndex(), raft.getLog().getLastTerm()))) {
				voteReceived(new VoteGranted(this.raft.getTerm().getCurrent()));				
			}
			this.electionEnd = time + DEFAULT_ELECTION_TIMEOUT;
			logger.info("Starting election for: " + this.raft.getTerm().getCurrent());
			return DEFAULT_ELECTION_TIMEOUT;
		} else {
			return electionTime - time;
		}
	}

	public long candidateLoop() {
		long time = System.currentTimeMillis();
		if (electionEnd > -1 && electionEnd <= time) {
			//timeout
			setFollower();
		} else {
			if (electionEnd < 0) {
				return tryStartElection();				
			}
		}
		return -1L;
	}

	public boolean vote(ClusterMember clusterMember, RequestForVote vote) {
		long currentTerm = raft.getTerm().getCurrent();
		if (vote.getTerm() > currentTerm) {
			raft.getTerm().setCurrent(vote.getTerm());
			if (raft.getRole() != Role.FOLLOWER) {
				setFollower();
			}
		}
		if (vote.getTerm() < currentTerm) {
			logger.info("vote.getTerm() < raft.getTerm().getCurrent(): "  + raft);
			return false;
		}
		if (votedForTerm >= vote.getTerm()) {
			logger.info("votedForTerm >= vote.getTerm(): "  + raft);
			return false;
		}
		if (vote.getLastLogTerm() < raft.getLog().getLastTerm()) {
			logger.info("vote.getLastLogTerm() < raft.getLog().getLastTerm(): "  + raft);
			return false;
		}
		if (vote.getLastLogIndex() < raft.getLog().getLastIndex()) {
			logger.info("vote.getLastLogIndex() < raft.getLog().getLastIndex(): "  + raft);
			return false;
		}
		this.votedForTerm = vote.getTerm();
		logger.info("setting voted for: " + this.votedForTerm + ": " + raft);
		return true;
	}

	

	public void setLeaderTimestamp(long time) {
		this.leaderTimestamp = time;
		
	}

}
