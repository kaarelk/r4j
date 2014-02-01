package org.r4j.rest.cluster;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Path("/control")
public class RestartServlet {

	private Logger logger = LoggerFactory.getLogger(getClass());

	@GET
	@Path("/pause")
	public String pause() {
		CRUDServlet.raft.stop();
		return "paused";
	}

	@GET
	@Path("/start")
	public String start() {
		if (!CRUDServlet.raft.isRunning()) {
			CRUDServlet.raft.init();
		}
		return "started";
	}
	
	@GET
	@Path("/status")
	public String status() {
		if (CRUDServlet.raft.isRunning()) {
			CRUDServlet.raft.init();
			return "running: " + CRUDServlet.raft.getRaft().getRole();
		} else {
			return "paused: " + CRUDServlet.raft.getRaft().getRole();
			
		}
	}
	
	@GET
	@Path("/stop")
	public String stop() {
		System.exit(0);
		return "";
	}
	
	
}
