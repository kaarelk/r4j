package org.r4j.protocol;

import org.r4j.Raft;

public interface MessageChannel {

	public void send(Raft source, Object o);
	
}
