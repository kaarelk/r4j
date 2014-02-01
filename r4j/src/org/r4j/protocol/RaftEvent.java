package org.r4j.protocol;

import org.r4j.ClusterMember;

public class RaftEvent {

	public enum EventType {
		APPEND,
		APPEND_RESPONSE,
		REQUEST_VOTE,
		VOTE_GRANTED,
		LOOP, 
		CLIENT_REQUEST,
		;
	}
	
	private ClusterMember source;
	
	private MessageChannel clientSource;
	
	private EventType type;
	
	private Object event;

	public RaftEvent(ClusterMember source, EventType type, Object event) {
		super();
		this.source = source;
		this.type = type;
		this.event = event;
	}

	public RaftEvent(MessageChannel clientSource, EventType type, Object event) {
		super();
		this.clientSource = clientSource;
		this.type = type;
		this.event = event;
	}



	public ClusterMember getSource() {
		return source;
	}

	public EventType getType() {
		return type;
	}

	public Object getEvent() {
		return event;
	}

	public MessageChannel getClientSource() {
		return clientSource;
	}

	@Override
	public String toString() {
		return "RaftEvent ["
				+ (source != null ? "source=" + source + ", " : "")
				+ (clientSource != null ? "clientSource=" + clientSource + ", "
						: "") + (type != null ? "type=" + type + ", " : "")
				+ (event != null ? "event=" + event : "") + "]";
	}


	
}
