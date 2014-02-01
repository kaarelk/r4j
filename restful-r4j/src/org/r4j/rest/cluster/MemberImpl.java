package org.r4j.rest.cluster;

import org.r4j.ClusterMember;
import org.r4j.Raft;
import org.r4j.protocol.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ.Socket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.FilterProvider;
import com.fasterxml.jackson.databind.ser.impl.SimpleBeanPropertyFilter;
import com.fasterxml.jackson.databind.ser.impl.SimpleFilterProvider;

public class MemberImpl implements ClusterMember, MessageChannel {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private long nextIndex;
	private long matchIndex;
	private long lastCommandReceived;
	private Socket inSocket;
	private String serverId;
	private Socket outSocket;
	
	private ObjectMapper om = Server.om;
	
	private String uri;
	private long lastAppend = 0L;
	
	public MemberImpl(Socket inSocket, String serverId, Socket outSocket) {
		super();
		this.inSocket = inSocket;
		this.serverId = serverId;
		this.outSocket = outSocket;
	
	}

	public MemberImpl() {
		super();
	}

	@Override
	public MessageChannel getChannel() {
		return this;
	}

	@Override
	public long getNextIndex() {
		return nextIndex;
	}

	@Override
	public void setNextIndex(long index) {
		this.nextIndex = index;
	}

	@Override
	public long getMatchIndex() {
		return matchIndex;
	}

	@Override
	public void setMatchIndex(long matchIndex) {
		this.matchIndex = matchIndex;
	}

	@Override
	public long getLastCommandReceived() {
		return lastCommandReceived;
	}

	@Override
	public void setLastcommandReceived(long time) {
		this.lastCommandReceived = time;
	}

	@Override
	public void send(Raft source, Object o) {
		try {
			Command c = new Command(serverId, o);
			logger.info("OUT: " + c);
			outSocket.send(om.writeValueAsBytes(c));
		} catch (JsonProcessingException e) {
			logger.error("Failed to send: " + o + ": " + e, e);
		}
	}

	public Socket getSocket() {
		return inSocket;
	}

	public String getUri() {
		return uri;
	}

	public void setUri(String uri) {
		this.uri = uri;
	}

	
	public long getLastAppend() {
		return lastAppend;
	}

	public void setLastAppend(long lastAppend) {
		this.lastAppend = lastAppend;
	}

	@Override
	public String toString() {
		return "MemberImpl [inSocket=" + inSocket + ", serverId=" + serverId
				+ "]";
	}

	
}
