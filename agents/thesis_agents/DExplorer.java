package thesis_agents;

import java.util.concurrent.ConcurrentLinkedQueue;

import org.grid.agent.Agent;
import org.grid.agent.Membership;
import org.grid.protocol.Neighborhood;
import org.grid.protocol.NewMessage.Direction;
import org.grid.protocol.Position;

import thesis_agents.Map.FindType;

@Membership(team = "dExplorer", passphrase = "1")
public class DExplorer extends Agent{

	/* Private variables */
	Map localMap;
	ConcurrentLinkedQueue<StateMessage> states;
	
	private static enum Mode {
		EXPLORE, HOMERUN, HITMAN, FOODHUNT
	}
	
	Mode mode;
	
	
	/* Overridden methods */
	
	@Override
	public void terminate() {
		// TODO Auto-generated method stub
		
	}

	@Override
	public void initialize() {
		localMap = new Map(getId());
		states = new ConcurrentLinkedQueue<StateMessage>();
		mode = Mode.EXPLORE;
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
					localMap.PrintLocalMap();
				}
				
				//compute next move
				Position food = localMap.FindNearest(FindType.FOOD);
				if(food != null)
				{
					changeMode(Mode.FOODHUNT);
				}
				else
				{
					Position loc = localMap.FindNearest(FindType.UNEXPLORED);
				}
				
				
				this.move(Direction.LEFT);
				
				//send message
				
				//move
			}
		}
		
	}
	
	/* Private methods */
	
	private void changeMode(Mode m)
	{
		mode = m;
	}

}
