package org.r4j.rest.cluster;

import org.r4j.protocol.AppendRequest;
import org.r4j.protocol.AppendResponse;
import org.r4j.protocol.RequestForVote;
import org.r4j.protocol.VoteGranted;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonSubTypes.Type;

public class Command {

	private String targetId;
	
	@JsonSubTypes({@Type(name="req", value=AppendRequest.class),
				   @Type(name="resp", value=AppendResponse.class),
				   @Type(name="voteReq", value=RequestForVote.class),
				   @Type(name="voteResp", value=VoteGranted.class)
				   })
	private Object raftCommand;

	
	public Command(String targetId, Object raftCommand) {
		super();
		this.targetId = targetId;
		this.raftCommand = raftCommand;
	}

	public Command() {
		super();
	}

	public String getTargetId() {
		return targetId;
	}

	public void setTargetId(String targetId) {
		this.targetId = targetId;
	}

	public Object getRaftCommand() {
		return raftCommand;
	}

	public void setRaftCommand(Object raftCommand) {
		this.raftCommand = raftCommand;
	}

	@Override
	public String toString() {
		return "Command [targetId=" + targetId + ", raftCommand=" + raftCommand
				+ "]";
	}
	
	
}
