package org.r4j.protocol;

import com.fasterxml.jackson.annotation.JsonIgnore;

public class AppendRequest {

	private long index;
	private long leaderTerm;
	private long logTerm;
	private long previousIndex;
	private long previousTerm;
	private long commitIndex;
	private Object payload;
	private transient MessageChannel clientChannel;
	
	public AppendRequest() {
		super();
	}

	public AppendRequest(long logTerm, long index, long leaderTerm, long previousIndex,
			long previousTerm, long commitIndex, Object payload) {
		super();
		this.logTerm = logTerm;
		this.index = index;
		this.leaderTerm = leaderTerm;
		this.previousIndex = previousIndex;
		this.previousTerm = previousTerm;
		this.commitIndex = commitIndex;
		this.payload = payload;
	}

	public AppendRequest(long logTerm, long index, long leaderTerm, long previousIndex,
			long previousTerm, long commitIndex, Object payload,
			MessageChannel clientChannel) {
		super();
		this.logTerm = logTerm;
		this.index = index;
		this.leaderTerm = leaderTerm;
		this.previousIndex = previousIndex;
		this.previousTerm = previousTerm;
		this.commitIndex = commitIndex;
		this.payload = payload;
		this.clientChannel = clientChannel;
	}

	public long getIndex() {
		return index;
	}

	public long getLeaderTerm() {
		return leaderTerm;
	}

	public long getLogTerm() {
		return logTerm;
	}

	public long getPreviousIndex() {
		return previousIndex;
	}

	public long getPreviousTerm() {
		return previousTerm;
	}

	public long getCommitIndex() {
		return commitIndex;
	}

	public Object getPayload() {
		return payload;
	}

	@JsonIgnore
	public MessageChannel getClientChannel() {
		return clientChannel;
	}

	@Override
	public String toString() {
		return "AppendRequest [index=" + index + ", term=" + leaderTerm
				+ ", previousIndex=" + previousIndex + ", previousTerm="
				+ previousTerm + ", commitIndex=" + commitIndex + ", "
				+ (payload != null ? "payload=" + payload : "") + "]";
	}

	
}
