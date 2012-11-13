package thesis_agents;

import java.util.HashMap;

import org.grid.protocol.Position;

public class AgentsMessage {
	
	int agentId;
	byte[] message;
	
	AgentsMessage(int from, byte[] message)
	{
		this.agentId = from;
		this.message = message;
	}
	
	public HashMap<Position, Integer> getMap()
	{		
		String[] values = new String(message).split(";");
		
		HashMap<Position, Integer> receivedMap = new HashMap<Position, Integer>();
		for(String value : values)
		{
			try
			{
				String[] temp = value.split(",");
				receivedMap.put(new Position(Integer.parseInt(temp[0]), Integer.parseInt(temp[1])), Integer.parseInt(temp[2]));
			}
			catch(Exception e)
			{
				System.err.println("Something went wrong when parsing " + value);
			}
		}
		
		return receivedMap;
	}
	

}
