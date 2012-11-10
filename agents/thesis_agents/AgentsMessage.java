package thesis_agents;

public class AgentsMessage {
	
	int agentId;
	byte[] message;
	
	AgentsMessage(int from, byte[] message)
	{
		this.agentId = from;
		this.message = message;
	}

}
