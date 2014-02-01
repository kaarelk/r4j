package org.r4j.executor;

import java.util.Queue;
import java.util.concurrent.LinkedBlockingQueue;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class QueueTask<E> implements Runnable {

	private Logger logger;
	
	private Queue<E> queue = new LinkedBlockingQueue<>();
	
	private AtomicBoolean running = new AtomicBoolean(false);
	
	private Lock lock = new ReentrantLock();
	
	private Handler<E> handler;
	
	public QueueTask(Handler<E> handler) {
		super();
		this.handler = handler;
		logger = LoggerFactory.getLogger(handler.getClass());
	}


	@Override
	public void run() {
		String oldName = Thread.currentThread().getName();
		Thread.currentThread().setName("" + handler.getName());
		while (true) {
			E o = queue.poll();
			if (o == null) {
				lock.lock();
				try {
					if (queue.isEmpty()) {
						running.set(false);
						Thread.currentThread().setName(oldName);
						return;
					} else {
						continue;
					}
				} finally {
					lock.unlock();
				}
			}
			try {
				handler.handleEvent(o);
			} catch (Exception e) {
				logger.error("Failed to handle: " + o + ": " + e, e);
			}			
		}
	}


	public AtomicBoolean getRunning() {
		return running;
	}

	public Lock getLock() {
		return lock;
	}


	public Queue<E> getQueue() {
		return queue;
	}
	
}
