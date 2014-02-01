package org.r4j;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.r4j.protocol.AppendRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class LogImpl implements Log {

	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private static final long LOG_START = 0;

	/** marker for log start - used for compacting log */
	protected long logStartIndex = 0;
	
	protected List<AppendRequest> log = new ArrayList<AppendRequest>();
	
	protected Term term;
	
	protected long currentCommitIndex = -1L;
	
	protected LogWriter writer;
	
	protected CommitHandler handler;
	
	public LogImpl(Term term) {
		super();
		this.term = term;
	}
	
	@Override
	public void setLogWriter(LogWriter writer) {
		this.writer = writer;
	}
	
	@Override
	public boolean append(AppendRequest entry) {
		return appendImpl(entry);
	}

	private boolean appendImpl(AppendRequest entry) {
		if (entry.getPayload() == null) {
			commit(entry.getCommitIndex());
			return true;
		}
		if (!validateEntry(entry)) {
			return false;
		}

		AppendRequest previous = null;
		int index = getIndex(entry.getIndex());
		if (index > log.size() - 1) {
			log.add(entry);
		} else {
			previous = log.set(index, entry);			
		}
		if (previous == null) {
			
		} else if (previous.getLogTerm() == entry.getLogTerm()) {
			//retry
		} else {
			//new leader
			//backward loop to minimize moving of elements
			for (int i = log.size() - 1; i > entry.getIndex(); i--) {
				AppendRequest r = log.remove(i);
				handler.reject(r);
			}
		}

		commit(entry.getCommitIndex());
		
		if (writer != null) {
			writer.flush();
		}
		
		return true;
	}

	public void commit(long index) {
		while (currentCommitIndex < index && get(currentCommitIndex + 1) != null) {
			currentCommitIndex++;
			logger.info("commit: " + get(currentCommitIndex));
			if (handler != null) {
				handler.commit(get(currentCommitIndex));
			}
			if (writer != null) {
				writer.flush();
			}
		}
	}

	private boolean validateEntry(AppendRequest entry) {
		if (entry.getLeaderTerm() < term.getCurrent()) {
			logger.info("entry.getTerm() < term.getCurrent()");
			return false;
		}
		
		AppendRequest current = get(entry.getIndex());
		if (current != null && current.getLogTerm() > entry.getLogTerm()) {
			logger.info("current.getTerm() > entry.getTerm()");
			//shouldn't be needed
			return false;
		}
		
		if (entry.getIndex() == LOG_START) {
			return true;
		}
		
		AppendRequest prev = get(entry.getPreviousIndex());	
		if ( prev != null && (prev.getIndex() != entry.getPreviousIndex() || prev.getLogTerm() != entry.getPreviousTerm())) {
			logger.info("prev.getTerm() != entry.getPreviousTerm(): " + prev.getLogTerm() + " " + entry.getPreviousTerm());
			return false;
		}
		
		if (entry.getPreviousIndex() > getLastIndex()) {
			logger.info("entry.getPreviousIndex() > getLastIndex(): ");
			return false;
		}
		
		return true;
	}

	@Override
	public AppendRequest get(long index) {
		int i = getIndex(index);
		if (log.size() > i) {
			return log.get(i);
		}
		return null;
	}

	@Override
	public long getLastCommitIndex() {
		if (this.currentCommitIndex < 0) {
			return -1L;
		}
		AppendRequest ar = get(this.currentCommitIndex);
		if (ar == null) {
			return -1L;
		} else {
			return ar.getIndex();
		}
	}

	protected int getIndex(long index) {
		return (int) (index - logStartIndex);
	}

	@Override
	public long getLastIndex() {
		if (log.isEmpty()) {
			return LOG_START - 1;
		} else {
			return log.get(log.size() - 1).getIndex();			
		}
		 
	}

	@Override
	public long getLastTerm() {
		if (log.isEmpty()) {
			return LOG_START;
		} else {
			return log.get(log.size() - 1).getLogTerm();			
		}
	}

	@Override
	public void setCommitHandler(CommitHandler cm) {
		this.handler = cm;
	}

	public CommitHandler getHandler() {
		return handler;
	}

}
