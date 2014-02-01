package org.r4j.rest.cluster.conf;

import java.util.ArrayList;
import java.util.List;

import org.r4j.Raft;
import org.r4j.Role;
import org.r4j.protocol.ClientRequest;
import org.r4j.protocol.MessageChannel;
import org.r4j.rest.cluster.Get;
import org.r4j.rest.cluster.Put;
import org.r4j.rest.cluster.Server;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testng.annotations.Test;

public class ClusterTest {

	private Logger logger;
	
	@Test
	public void test() throws InterruptedException {
		logger = LoggerFactory.getLogger(getClass());
		List<Server> list = new ArrayList<>();
		for (int i = 0; i < 3; i++) {
			ClusterConf c = new ClusterConf();
			c.setCluster(new ArrayList<MemberConf>());
			c.setId("" + i);
			c.setPort(2000 + i);
			for (int j = 0; j < 3; j++) {
				if (i == j) continue;
				MemberConf mc = new MemberConf();
				mc.setId("" + j);
				mc.setIp("localhost");
				mc.setPort(2000 + j);
				c.getCluster().add(mc);
			}
			list.add(new Server(c));
		}
		int ctr = 0;
		for (Server s : list) {
			new Thread(s, "server-" + ctr).start();
			ctr++;
		}
		
		for (int i = 0; i < 1; i++) {
			Server leader = null;
			for (Server s : list) {
				if (s.getQueue() != null && s.getQueue().getRaft().getRole() == Role.LEADER) {
					leader = s;
					break;
				}
			}
			if (leader == null) {
				i--;
			} else {
				leader.getQueue().handleClientRequest(new MessageChannel() {
					@Override
					public void send(Raft source, Object o) {
						System.out.println("PUT: " + o);
					}
				}, new ClientRequest(new Put("" + i, "" + (2 >> i))));
				leader.getQueue().handleClientRequest(new MessageChannel() {
					@Override
					public void send(Raft source, Object o) {
						System.out.println("GET: " + o);
					}
				}, new ClientRequest(new Get("" + i)));
			}
			Thread.sleep(1000);
		}
		Thread.sleep(100000);
	}
	
}
