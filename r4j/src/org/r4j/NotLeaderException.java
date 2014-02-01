package org.r4j;

public class NotLeaderException extends RuntimeException {

	private static final long serialVersionUID = -9199331764286009275L;
	
	//used for redirecting
	private ClusterMember actualLeader;

	public NotLeaderException(ClusterMember actualLeader) {
		super();
		this.actualLeader = actualLeader;
	}

	public NotLeaderException() {
		super();
	}

	public NotLeaderException(String message, Throwable cause,
			boolean enableSuppression, boolean writableStackTrace) {
		super(message, cause, enableSuppression, writableStackTrace);
	}

	public NotLeaderException(String message, Throwable cause) {
		super(message, cause);
	}

	public NotLeaderException(String message) {
		super(message);
	}

	public NotLeaderException(Throwable cause) {
		super(cause);
	}

	public ClusterMember getActualLeader() {
		return actualLeader;
	}
	
	
}
