package org.r4j.rest.cluster;

public class Delete {

	private String key;

	public Delete(String key) {
		super();
		this.key = key;
	}

	public Delete() {
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
		return "Delete [key=" + key + "]";
	}
	
	
	
}
