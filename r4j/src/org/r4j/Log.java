package org.r4j;

import java.util.List;

import org.r4j.protocol.AppendRequest;

public interface Log {
	
	public boolean append(AppendRequest log);
	
	public AppendRequest get(long index);
	
	public long getLastCommitIndex();
	
	public long getLastIndex();
	
	public long getLastTerm();
	
	public void commit(long index);
 
	public void setCommitHandler(CommitHandler cm);
	public void setLogWriter(LogWriter writer);	
}
