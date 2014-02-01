package org.r4j.rest.cluster.conf;

import java.io.File;
import java.io.IOException;
import java.util.List;

import org.r4j.protocol.AppendRequest;
import org.testng.annotations.Test;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

public class LogReaderTest {

	@Test
	public void test() throws JsonProcessingException, IOException {
		ObjectMapper om = new ObjectMapper();
		
		MappingIterator<AppendRequest> iter = om.reader(AppendRequest.class).readValues(new File("test1.log"));
		while (iter.hasNext()) {
			System.out.println(iter.next());
		}
	}
	
}
