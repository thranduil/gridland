package thesis_agents;

import java.util.ArrayList;
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
		EXPLORE, HOMERUN, HITMAN, FOODHUNT, ONLYEXPLORE, NEARHQ
	}
	
	/* Private variables */
	Map localMap;
	
	private ConcurrentLinkedQueue<StateMessage> states;
	private ConcurrentLinkedQueue<Direction> plan;
	private ConcurrentLinkedQueue<AgentsMessage> inbox;
	
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
		inbox = new ConcurrentLinkedQueue<AgentsMessage>();
		mode = Mode.EXPLORE;
	}

	@Override
	public void receive(int from, byte[] message) {
		inbox.add(new AgentsMessage(from, message));
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
			if(!states.isEmpty())
			{
				//update local map with all received states
				while(!states.isEmpty())
				{
					StateMessage msg = states.poll();
					if(msg.hasFlag && mode != Mode.NEARHQ)
					{
						changeMode(Mode.HOMERUN);
					}
					
					localMap.updateMap(msg.neighborhood);
					//localMap.printLocalMap();
				}
				
				//process all received messages
				while(!inbox.isEmpty())
				{
					localMap.updateMap(inbox.poll());
				}
				
				//get all visible friendly agents
				ArrayList<Integer> agents = localMap.getFriendlyAgents(2);
				for(Integer a : agents)
				{
					send(a, localMap.getEncodedMap());
				}
				
				
				//finding next target field based on agent mode
				switch(mode)
				{
					case FOODHUNT:
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
					case HITMAN:
						break;
					case HOMERUN:
					{
						nextTarget = localMap.findNearest(FindType.HQ);
						break;
					}
					case NEARHQ:
					{
						nextTarget = localMap.findNearest(FindType.HQ);
						nextTarget = localMap.findEmptyFieldNear(nextTarget, 2);
						changeMode(Mode.HOMERUN);
						break;
					}
					case ONLYEXPLORE:
					{
						nextTarget = localMap.findNearest(FindType.UNEXPLORED);
						changeMode(Mode.EXPLORE);
						break;
					}
				}
				
				//compute next move based on nextTarget
				
				//TODO: perform planning on other thread and here wait
				//for specific limit and use old plan if computing takes too long
				System.out.println("Target:" + nextTarget);
				plan = new ConcurrentLinkedQueue<Direction>(localMap.dijkstraPlan(nextTarget));
				if(plan == null || plan.size() == 0)
				{
					localMap.setLastMove(Direction.NONE);
					this.move(Direction.NONE);
					
					//if food was found, but is not accessible
					//now search only for unexplored places
					if(mode == Mode.FOODHUNT)
					{
						changeMode(Mode.ONLYEXPLORE);
					}
					
					//if path to hq can not be found 
					//search for target point in vicinity of hq
					if(mode == Mode.HOMERUN)
					{
						changeMode(Mode.NEARHQ);
					}
					continue;
				}
				else
				{
					Direction nextMove = plan.poll();

					if(localMap.canSafelyMove(nextMove))
					{
						localMap.setLastMove(nextMove);
						this.move(nextMove);
					}
					else
					{
						if(Math.random() < 0.2)
						{
							localMap.setLastMove(nextMove);
							this.move(nextMove);
						}
						else
						{
							localMap.setLastMove(Direction.NONE);
							this.move(Direction.NONE);
						}
					}					
				}
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
