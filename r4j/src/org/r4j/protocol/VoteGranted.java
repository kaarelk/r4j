package org.r4j.protocol;

public class VoteGranted {

	private long term;
	
	public VoteGranted() {
		super();
	}
	
	public VoteGranted(long term) {
		super();
		this.term = term;
	}

	public long getTerm() {
		return term;
	}
	
}
