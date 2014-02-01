package org.r4j;

import java.util.concurrent.atomic.AtomicLong;

public class Term {

	private AtomicLong term = new AtomicLong();
	
	public long getCurrent() {
		return term.get();
	}
	
	public void setCurrent(long term) {
		this.term.set(term);
	}
	
	public long newTerm() {
		setCurrent(this.term.incrementAndGet());
		return getCurrent();
	}
	
}
