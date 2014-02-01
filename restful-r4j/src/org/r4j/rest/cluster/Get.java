package org.r4j.rest.cluster;

public class Get {

	private String key;

	
	public Get(String key) {
		super();
		this.key = key;
	}

	public Get() {
		super();
	}

	public String getKey() {
		return key;
	}

	public void setKey(String key) {
		this.key = key;
	}

	@Override
	public String toString() {
		return "Get [key=" + key + "]";
	}
	
	
	
}
