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

	private static enum Mode {
		EXPLORE, HOMERUN, HITMAN, FOODHUNT
	}
	
	/* Private variables */
	Map localMap;
	
	private ConcurrentLinkedQueue<StateMessage> states;
	private ConcurrentLinkedQueue<Direction> plan;
	
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
		plan = new ConcurrentLinkedQueue<Direction>();
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
		
		Position nextTarget = null;
		while(isAlive())
		{
			//process state if there is any
			//TODO: optimization - if there is no states, compute next move more accurate
			if(!states.isEmpty())
			{
				//update local map with all received states
				while(states.peek() != null)
				{
					localMap.updateMap(states.poll());
					localMap.printLocalMap();
				}
				
				//finding next target field based on agent mode
				switch(mode)
				{
					case EXPLORE:
					{
						nextTarget = localMap.findNearest(FindType.FOOD);
						if(nextTarget != null)
						{
							changeMode(Mode.FOODHUNT);
						}
						else
						{
							nextTarget = localMap.findNearest(FindType.UNEXPLORED);
							
							//No flags and no unexplored places found
							if(nextTarget == null)
							{
								changeMode(Mode.HITMAN);
							}
						}
						
						break;
					}
					case FOODHUNT:
					{
						
						break;
					}
					case HITMAN:
						break;
					case HOMERUN:
						break;
					default:
						break;
				}
				
				//compute next move based on nextTarget
				
				/*
				if(plan.isEmpty())
				{
					//plan path to nextTarget
				}
				
				Direction nextMove = plan.poll();
				
				if(localMap.CanSafelyMove(nextMove))
				{
					this.move(nextMove);
				}
				*/
				
				plan = localMap.dijkstraPlan(nextTarget);
				
				if(localMap.canSafelyMove(Direction.LEFT))
				{
					this.move(Direction.LEFT);
				}
				else if(localMap.canSafelyMove(Direction.UP))
				{
					this.move(Direction.UP);
				}
				else if(localMap.canSafelyMove(Direction.RIGHT))
				{
					this.move(Direction.RIGHT);
				}
				else if(localMap.canSafelyMove(Direction.DOWN))
				{
					this.move(Direction.DOWN);
				}


				
				
				//send message
				
				//move
			}
		}
		
	}
	
	/* Private methods */
	
	private void changeMode(Mode m)
	{
		System.out.println("Changing mode to " + m.toString());
		mode = m;
	}

}
