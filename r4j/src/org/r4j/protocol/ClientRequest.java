package org.r4j.protocol;

public class ClientRequest {

	/**
	 * Payload of object, including reference to client MessageChannel
	 */
	private Object payload;

	
	public ClientRequest() {
		super();
	}

	public ClientRequest(Object payload) {
		super();
		this.payload = payload;
	}

	public Object getPayload() {
		return payload;
	}
	
	
}
