package org.r4j.executor;

public interface Handler<E> {

	public void handleEvent(E o);

	public String getName();
	
}
