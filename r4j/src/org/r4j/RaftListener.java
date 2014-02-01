package org.r4j;

import org.r4j.protocol.AppendRequest;
import org.r4j.protocol.AppendResponse;
import org.r4j.protocol.ClientRequest;
import org.r4j.protocol.MessageChannel;
import org.r4j.protocol.RequestForVote;
import org.r4j.protocol.VoteGranted;

public interface RaftListener {

	public void voteReceived(ClusterMember member, VoteGranted vote);

	public void append(ClusterMember source, AppendRequest entry);

	public void vote(ClusterMember clusterMember, RequestForVote vote);
	
	public void appendResponse(ClusterMember source, AppendResponse resp);
	
	public void handleClientRequest(MessageChannel client, ClientRequest req);
	
}
