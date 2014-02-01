package org.r4j.example;

import org.r4j.CommitHandler;
import org.r4j.protocol.AppendRequest;
import org.r4j.protocol.ClientResponse;

public class RespondingCommitHandler implements CommitHandler {

	@Override
	public void commit(AppendRequest entry) {
		Object o = commitImpl(entry);
		if (entry.getClientChannel() != null) {
			entry.getClientChannel().send(null, o);
		}
	}

	protected Object commitImpl(AppendRequest entry) {
		return new ClientResponse(0, null);
	}

	@Override
	public void reject(AppendRequest entry) {
		if (entry.getClientChannel() != null) {
			entry.getClientChannel().send(null, new ClientResponse(1, null));
		}
	}

	
}
