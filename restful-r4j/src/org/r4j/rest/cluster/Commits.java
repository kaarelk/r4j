package org.r4j.rest.cluster;

import java.util.HashMap;
import java.util.Map;

import org.r4j.CommitHandler;
import org.r4j.protocol.AppendRequest;
import org.r4j.protocol.ClientRequest;
import org.r4j.protocol.ClientResponse;

public class Commits implements CommitHandler {
	
	//replace with leveldb
	private Map<String, String> map = new HashMap<>();
	
	@Override
	public void commit(AppendRequest entry) {
		Object o = entry.getPayload();
		if (o instanceof Put) {
			Put p = (Put)o;
			map.put(p.getKey(), p.getValue());
			if (entry.getClientChannel() != null) {
				entry.getClientChannel().send(null, new ClientResponse(0, ""));				
			}
		} else if (o instanceof Get) {
			Get g = (Get) o;
			String value = map.get(g.getKey());
			if (entry.getClientChannel() != null) {
				entry.getClientChannel().send(null, new ClientResponse(0, "" + value));				
			}
		} else if (o instanceof Delete) {
			Delete e = (Delete) o;
			String value = map.remove(e.getKey());
			if (entry.getClientChannel() != null) {
				entry.getClientChannel().send(null, new ClientResponse(0, "" + value));				
			}
		}
	}

	@Override
	public void reject(AppendRequest entry) {
		if (entry.getClientChannel() != null) {
			entry.getClientChannel().send(null, new ClientResponse(301, null));				
		}
	}

}
