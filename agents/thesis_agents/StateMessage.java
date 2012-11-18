package thesis_agents;

import org.grid.protocol.Neighborhood; 
import org.grid.protocol.NewMessage.Direction;

public class StateMessage {
	
	Neighborhood neighborhood;
	Direction direction;
	boolean hasFlag;
	
	public StateMessage(Neighborhood n, boolean flag, Direction d)
	{
		neighborhood = n;
		hasFlag = flag;
		direction = d;
	}
}
