package org.r4j.protocol;

public interface ProtocolFactory {

	public AppendRequest createAppend(long term, long index, long prevTerm, long prevIndex, Object payload);
	
	
	
}
