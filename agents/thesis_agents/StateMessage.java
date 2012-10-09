package thesis_agents;

import org.grid.protocol.Neighborhood; 

public class StateMessage {
	
	Neighborhood neighborhood;
	boolean hasFlag;
	
	public StateMessage(Neighborhood n, boolean flag)
	{
		neighborhood = n;
		hasFlag = flag;
	}
}
