package org.r4j;

import org.r4j.protocol.MessageChannel;


public interface ClusterMember {

	public MessageChannel getChannel();
	
	public long getNextIndex();
	
	public void setNextIndex(long index);
	
	public long getMatchIndex();

	public void setMatchIndex(long matchIndex);

	public long getLastCommandReceived();
	
	public void setLastcommandReceived(long time);
}
