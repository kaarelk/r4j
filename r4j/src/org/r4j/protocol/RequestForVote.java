package org.r4j.protocol;

public class RequestForVote {

	private long term;
	
	private long lastLogIndex;
	
	private long lastLogTerm;
	
	public RequestForVote(long term, long lastLogIndex, long lastLogTerm) {
		super();
		this.term = term;
		this.lastLogIndex = lastLogIndex;
		this.lastLogTerm = lastLogTerm;
	}

	public RequestForVote() {
		super();
	}

	public long getTerm() {
		return term;
	}

	public void setTerm(long term) {
		this.term = term;
	}

	public long getLastLogIndex() {
		return lastLogIndex;
	}

	public void setLastLogIndex(long lastLogIndex) {
		this.lastLogIndex = lastLogIndex;
	}

	public long getLastLogTerm() {
		return lastLogTerm;
	}

	public void setLastLogTerm(long lastLogTerm) {
		this.lastLogTerm = lastLogTerm;
	}

	@Override
	public String toString() {
		return "RequestForVote [term=" + term + ", lastLogIndex="
				+ lastLogIndex + ", lastLogTerm=" + lastLogTerm + "]";
	}

}
