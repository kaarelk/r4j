package org.r4j;

public class Election {

	private final int members;
	
	private int votes = 0;
	
	private final long term;

	public Election(int members, long term) {
		super();
		this.members = members;
		this.term = term;
	}

	public int getMembers() {
		return members;
	}


	public int getVotes() {
		return votes;
	}

	public long getTerm() {
		return term;
	}

	public void incrementVotes() {
		votes++;
	}

	public boolean hasWon() {
		return members/2 < votes;
	}
	
	
	
}
