package org.r4j.protocol;

public class ClientResponse {

	private int errCode;
	
	private Object payload;

	public ClientResponse(int errCode, Object payload) {
		super();
		this.errCode = errCode;
		this.payload = payload;
	}

	public ClientResponse() {
		super();
	}

	public int getErrCode() {
		return errCode;
	}

	public Object getPayload() {
		return payload;
	}

	@Override
	public String toString() {
		return "ClientResponse [errCode=" + errCode + ", payload=" + payload
				+ "]";
	}
	
}
