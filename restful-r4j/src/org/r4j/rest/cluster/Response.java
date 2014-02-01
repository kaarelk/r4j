package org.r4j.rest.cluster;

public class Response {

	private String response;
	
	private int err = 0;
	
	private String redirectUri;

	
	public Response() {
		super();
	}

	public Response(String response, int err, String redirectUri) {
		super();
		this.response = response;
		this.err = err;
		this.redirectUri = redirectUri;
	}

	public Response(String response) {
		super();
		this.response = response;
	}

	public String getResponse() {
		return response;
	}

	public void setResponse(String response) {
		this.response = response;
	}

	public int getErr() {
		return err;
	}

	public void setErr(int err) {
		this.err = err;
	}

	public String getRedirectUri() {
		return redirectUri;
	}

	public void setRedirectUri(String redirectUri) {
		this.redirectUri = redirectUri;
	}
	
	
	
}
