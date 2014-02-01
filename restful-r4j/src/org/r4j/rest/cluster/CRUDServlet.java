package org.r4j.rest.cluster;

import java.net.URI;
import java.util.concurrent.Exchanger;
import java.util.concurrent.Semaphore;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import javax.inject.Inject;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.NotAllowedException;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Response;

import org.r4j.ClusterMember;
import org.r4j.Raft;
import org.r4j.RaftEventQueue;
import org.r4j.protocol.ClientRequest;
import org.r4j.protocol.ClientResponse;
import org.r4j.protocol.MessageChannel;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/rest")
public class CRUDServlet {

	private Logger logger = LoggerFactory.getLogger(getClass());

	protected static RaftEventQueue raft;

	@GET
	@Path("/{key}")
	public Response get(@PathParam("key") String key) {
		Get g = new Get(key);
		return process(g, "/rest/" + key);
	}

	private Response process(Object o, String path) {
		final Exchanger<ClientResponse> exchanger = new Exchanger<>();
		try {
			raft.handleClientRequest(new MessageChannel() {

				@Override
				public void send(Raft source, Object o) {
					try {
						exchanger.exchange((ClientResponse) o, 30,
								TimeUnit.SECONDS);
					} catch (Exception e) {
						logger.error("" + e, e);
					}

				}
			}, new ClientRequest(o));
			
			try {
				ClientResponse r = exchanger.exchange(null, 30,
						TimeUnit.SECONDS);
			
				if (r.getErrCode() == 0) {
					return Response.ok("" + r.getPayload()).build();
				} else {
					long lastCmd = 0L;
					MemberImpl leader = null;
					for (ClusterMember cm : raft.getRaft().getMembers()) {
						MemberImpl m = (MemberImpl) cm;
						if (m.getLastAppend() > lastCmd) {
							lastCmd = m.getLastCommandReceived();
							leader = m;
						}
					}
					MemberImpl m = (MemberImpl) leader;
					if (m == null) {
						return Response.status(401).build();
					}
					return Response.temporaryRedirect(new URI(m.getUri() + path)).build();
				}
			} catch (Exception e) {
				logger.error("" + e, e);
				throw new WebApplicationException("Timed out");
			}
		} catch (NotAllowedException e) {
			logger.error("" + e, e);
			throw new WebApplicationException("Not  a leader", 301);
		}
	}

	@PUT
	@Path("/{key}/{value}")
	public Response put(@PathParam("key") String key,
			@PathParam("value") String value) {
		return process(new Put(key, value), "/rest/" + key + "/" + value);
	}

	@DELETE
	@Path("/{key}")
	public Response delete(@PathParam("key") String key) {
		return process(new Delete(key), "/rest/" + key);
	}

	public static void setRaft(RaftEventQueue raft) {
		CRUDServlet.raft = raft;
	}

}
