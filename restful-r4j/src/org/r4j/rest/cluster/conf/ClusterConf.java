package org.r4j.rest.cluster.conf;

import java.util.List;

public class ClusterConf {

	private int port;
	
	private String host;
	
	private String id;
	
	private List<MemberConf> cluster;

	public List<MemberConf> getCluster() {
		return cluster;
	}

	public void setCluster(List<MemberConf> cluster) {
		this.cluster = cluster;
	}

	public int getPort() {
		return port;
	}

	public void setPort(int port) {
		this.port = port;
	}

	public String getHost() {
		return host;
	}

	public void setHost(String host) {
		this.host = host;
	}

	public String getId() {
		return id;
	}

	public void setId(String id) {
		this.id = id;
	}
	
	
	
	
}
