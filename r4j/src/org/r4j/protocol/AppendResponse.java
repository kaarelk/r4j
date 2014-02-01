package org.r4j.protocol;

public class AppendResponse {
	
	private long currentTerm;
	
	private boolean success;

	private long entryIndex;
	
	private long entryTerm; 
	
	
	public AppendResponse(long currentTerm, boolean success, long entryIndex,
			long entryTerm) {
		super();
		this.currentTerm = currentTerm;
		this.success = success;
		this.entryIndex = entryIndex;
		this.entryTerm = entryTerm;
	}

	public AppendResponse() {
		super();
	}

	public long getCurrentTerm() {
		return currentTerm;
	}

	public void setCurrentTerm(long term) {
		this.currentTerm = term;
	}

	public boolean isSuccess() {
		return success;
	}

	public void setSuccess(boolean success) {
		this.success = success;
	}

	public long getEntryIndex() {
		return entryIndex;
	}

	public long getEntryTerm() {
		return entryTerm;
	}

	@Override
	public String toString() {
		return "AppendResponse [currentTerm=" + currentTerm + ", success="
				+ success + ", entryIndex=" + entryIndex + ", entryTerm="
				+ entryTerm + "]";
	}
	

	

}
