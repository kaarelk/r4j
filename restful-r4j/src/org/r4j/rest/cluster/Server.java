package org.r4j.rest.cluster;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ScheduledThreadPoolExecutor;

import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.r4j.ClusterMember;
import org.r4j.Raft;
import org.r4j.RaftEventQueue;
import org.r4j.protocol.AppendRequest;
import org.r4j.protocol.AppendResponse;
import org.r4j.protocol.RequestForVote;
import org.r4j.protocol.VoteGranted;
import org.r4j.rest.cluster.conf.ClusterConf;
import org.r4j.rest.cluster.conf.MemberConf;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectMapper.DefaultTyping;

public class Server implements Runnable {

	private Map<String, MemberImpl> members = new HashMap<String, MemberImpl>();
	
	private boolean running = true;
	
	private Raft raft;
	
	private ScheduledThreadPoolExecutor executor = new ScheduledThreadPoolExecutor(5);
	
	private ClusterConf conf;
	
	private Logger logger = LoggerFactory.getLogger(getClass());
	
	private RaftEventQueue queue;
	
	public static ObjectMapper om = new ObjectMapper().
			configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false).
			enableDefaultTyping(DefaultTyping.JAVA_LANG_OBJECT);
	
	static {
		System.setProperty("org.slf4j.simpleLogger.showDateTime", "true");
		System.setProperty("org.slf4j.simpleLogger.defaultLogLevel", "info");
		System.setProperty("org.slf4j.simpleLogger.dateTimeFormat", "yyyy-MM-dd HH:mm:ss.SSS");
//		System.setProperty("org.slf4j.simpleLogger.logFile", "raft.log");		
	}
	
	public static void main(String[] args) throws IOException {
		
		String s = "cluster.conf";
		if (args.length > 0) {
			s = args[0];
		}
		new Server(s).run();
		
	}
	
	public Server(String confFile) {
		super();
		try {
			conf = om.readValue(new File(confFile), ClusterConf.class);
		} catch (Exception e) {
			System.err.println(e);
			e.printStackTrace();
			System.exit(1);
		}
	}

	public Server(ClusterConf c) {
		super();
		this.conf = c;
	}

	
	public void run() {
		raft = new Raft();
		raft.setId(conf.getId());
		
		queue = new RaftEventQueue(raft, executor);
		raft.setCommitHandler(new Commits());
		Context ctx = ZMQ.context(1);
        Socket publisher = ctx.socket(ZMQ.PUB);
        if (conf.getHost() == null) {
        	conf.setHost("*");
        }
        publisher.bind("tcp://" + conf.getHost() + ":" + conf.getPort());
        
        if (conf.getId() == null) {
        	conf.setId(conf.getHost() + ":" + conf.getPort());
        }
        
		raft.getLog().setLogWriter(new Writer(new File(conf.getId() + ".log"), raft.getLog()));
		
        for (MemberConf mc : conf.getCluster()) {
            if (mc.getId() == null) {
            	mc.setId(mc.getIp() + ":" + mc.getPort());
            }
        	Socket s = ctx.socket(ZMQ.SUB);
        	s.connect("tcp://" + mc.getIp() + ":" + mc.getPort());
        	s.subscribe(("{\"targetId\":\"" + conf.getId() + "\"").getBytes());
        	MemberImpl m = new MemberImpl(s, mc.getId(), publisher);
        	if (mc.getPublicHost() == null) {
        		mc.setPublicHost(mc.getIp());
        	}
        	m.setUri("http://" + mc.getPublicHost() + ":" + (mc.getPort() + 10));
        	members.put(mc.getId(), m);
        }
		
        LinkedList<MemberImpl> list = new LinkedList<>(members.values());
        
        raft.setMembers(list);
        queue.init();
        CRUDServlet.setRaft(queue);
        org.eclipse.jetty.server.Server server = new org.eclipse.jetty.server.Server(conf.getPort() + 10);
        ServletHolder _jersey = new ServletHolder(new ServletContainer(new ResourceConfig(CRUDServlet.class, RestartServlet.class)));
        _jersey.setName("restful service servlet");


        ServletContextHandler servletCtx = new ServletContextHandler();
        servletCtx.addServlet(_jersey, "/*");

        servletCtx.setContextPath("/");
        server.setHandler(servletCtx);  
        try {
			server.start();
		} catch (Exception e1) {
			logger.error(""+ e1, e1);
			System.exit(1);
		}
        
        while (running) {
        	int ctr = 0;
            for (MemberImpl mi : list) {
            	String s = mi.getSocket().recvStr(ZMQ.DONTWAIT);
            	if (s != null) {
            		ctr++;
            		try {
    					Command c = om.readValue(s, Command.class);
    					logger.info("IN(" + mi + "): " + c);
    					if (c.getRaftCommand() instanceof AppendRequest) {
    						queue.append(mi, (AppendRequest) c.getRaftCommand());
    						mi.setLastAppend(System.currentTimeMillis());
    					} else if (c.getRaftCommand() instanceof AppendResponse) {
    						queue.appendResponse(mi, (AppendResponse) c.getRaftCommand());
    					} else if (c.getRaftCommand() instanceof RequestForVote) {
    						queue.vote(mi, (RequestForVote) c.getRaftCommand());
    					} else if (c.getRaftCommand() instanceof VoteGranted) {
    						queue.voteReceived(mi, (VoteGranted) c.getRaftCommand());
    					} else {
    						logger.warn("Unknown cmd: " + c);
    					}
    				} catch (Exception e) {
    					logger.error("" + e, e);
    				}
            		
            	}
            }
            if (ctr == 0) {
            	try {
					Thread.sleep(100);
				} catch (InterruptedException e) {
					logger.warn("" + e);
				}
            }
        }
	}

	public Raft getRaft() {
		return raft;
	}

	public RaftEventQueue getQueue() {
		return queue;
	}
	
}
