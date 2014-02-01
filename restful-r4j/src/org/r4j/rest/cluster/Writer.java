package org.r4j.rest.cluster;

import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.List;

import org.r4j.Log;
import org.r4j.LogImpl;
import org.r4j.LogWriter;
import org.r4j.protocol.AppendRequest;

import com.fasterxml.jackson.core.JsonGenerationException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonMappingException;
import com.fasterxml.jackson.databind.MappingIterator;
import com.fasterxml.jackson.databind.ObjectMapper;

public class Writer implements LogWriter {

	private File file;
	
	private Log log;
	
	long curIndex = -1L;

	private ObjectMapper om = Server.om;
	
	private DataOutputStream out;
	
	public Writer(File file, Log log) {
		super();
		this.file = file;
		this.log = log;
		try {
			load();
			file.delete();
			out = new DataOutputStream(new FileOutputStream(file));
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	public void load() throws JsonProcessingException, IOException {
		MappingIterator<AppendRequest> iter = om.reader(AppendRequest.class).readValues(file);
		while (iter.hasNext()) {
			log.append(iter.next());
		}
	}


	@Override
	public void flush() {
		try {
			if (curIndex > log.getLastIndex()) {
				out.close();
				curIndex = -1L;
				file.delete();
				out = new DataOutputStream(new FileOutputStream(file));
			}
			
			
			for (;(curIndex + 1) <= log.getLastIndex(); curIndex++) {
				try {
					byte[] b = om.writeValueAsBytes(log.get(curIndex+1));
					out.write(b);
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			out.flush();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

}
