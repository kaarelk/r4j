package org.r4j.rest.cluster.conf;

import java.util.UUID;

public class MemberConf {
	
	private String ip;
	private String publicHost;
	private int port;
	private String id = UUID.randomUUID().toString();
	public String getIp() {
		return ip;
	}
	public void setIp(String ip) {
		this.ip = ip;
	}
	public int getPort() {
		return port;
	}
	public void setPort(int port) {
		this.port = port;
	}
	public String getId() {
		return id;
	}
	public void setId(String id) {
		this.id = id;
	}
	public String getPublicHost() {
		return publicHost;
	}
	public void setPublicHost(String publicHost) {
		this.publicHost = publicHost;
	}

	
}
