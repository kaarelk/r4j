package org.r4j.rest.cluster.conf;

import java.util.ArrayList;

import org.r4j.Raft;
import org.r4j.protocol.AppendRequest;
import org.r4j.protocol.MessageChannel;
import org.r4j.protocol.RequestForVote;
import org.r4j.rest.cluster.Command;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ObjectWriter;
import com.fasterxml.jackson.databind.SerializationFeature;

public class ConfJsonExample {
	
	@Test
	public void test() throws JsonProcessingException {
		ObjectMapper om = new ObjectMapper();
		ObjectWriter ow = om.writer().with(SerializationFeature.INDENT_OUTPUT);
		ClusterConf c = new ClusterConf();
		c.setHost("localhost");
		c.setId("server1");
		c.setPort(5556);
		MemberConf mc = new MemberConf();
		mc.setId("test1");
		mc.setIp("localhost");
		mc.setPort(3344);
		c.setCluster(new ArrayList<MemberConf>());
		c.getCluster().add(mc);
		System.out.println(ow.writeValueAsString(c));		
		Command cmd = new Command("testxx", new RequestForVote(1, 2, 3));
		System.out.println(om.writeValueAsString(cmd));		
		AppendRequest r = new AppendRequest(3, 4, 5, 1, 3, 2, "payload", new MessageChannel() {
			
			@Override
			public void send(Raft source, Object o) {
			}
		});
		System.out.println(om.writeValueAsString(r));		
	}

}
