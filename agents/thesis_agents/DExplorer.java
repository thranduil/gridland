package thesis_agents;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.grid.agent.Agent;
import org.grid.agent.Membership;
import org.grid.protocol.Neighborhood;
import org.grid.protocol.NewMessage.Direction;

@Membership(team = "dExplorer", passphrase = "1")
public class DExplorer extends Agent{

	/* Private variables */
	Map localMap;
	ConcurrentLinkedQueue<StateMessage> states;
	
	
	/* Overridden methods */
	
	@Override
	public void terminate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initialize() {
		localMap = new Map();
		states = new ConcurrentLinkedQueue<StateMessage>();
		
	}

	@Override
	public void receive(int from, byte[] message) {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void state(int stamp, Neighborhood neighborhood,
			Direction direction, boolean hasFlag) {
		
		if(direction != Direction.NONE)
		{
			System.out.println("Agent is still moving! This should not happen.");
		}
		
		states.add(new StateMessage(neighborhood, hasFlag));
	}

	@Override
	public void run() {
		
		//first trigger scan request, so that we get initial state
		scan(0);
		
		while(isAlive())
		{
			//process state if there is any
			//TODO: optimization - if there is no states, compute next move more accurate
			if(!states.isEmpty())
			{
				//update local map with all received states
				while(states.peek() != null)
				{
					localMap.UpdateMap(states.poll());
				}
				
				//compute next move
				
				//send message
				
				//move
			}
		}
		
	}
	
	/* Private methods */

}
