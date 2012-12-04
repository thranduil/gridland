package thesis_agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.grid.agent.Agent;
import org.grid.agent.Membership;
import org.grid.protocol.Neighborhood;
import org.grid.protocol.NewMessage.Direction;
import org.grid.protocol.Position;

import thesis_agents.Map.FindType;

//java -cp bin org.grid.agent.Agent localhost thesis_agents.DExplorer
@Membership(team = "dExplorer", passphrase = "1")
public class DExplorer extends Agent{

	private static enum Mode {
		EXPLORE, HOMERUN, REBOOT, FOODHUNT, ONLYEXPLORE, NEARHQ
	}
	
	/* Private variables */
	Map localMap;
	
	private ConcurrentLinkedQueue<StateMessage> states;
	private ConcurrentLinkedQueue<Direction> plan;
	private ConcurrentLinkedQueue<AgentsMessage> inbox;
	private HashMap<Integer, Integer> communication;
	
	private int step;
	private int messageDistance = 3;
	
	boolean debug = false;
	Mode mode;
	
	/* Overridden methods */
	
	@Override
	public void terminate() {
		if(debug)
		{
			System.out.println("I am dead!");
		}
	}

	@Override
	public void initialize() {
		localMap = new Map(getId(), debug);
		states = new ConcurrentLinkedQueue<StateMessage>();
		plan = new ConcurrentLinkedQueue<Direction>();
		inbox = new ConcurrentLinkedQueue<AgentsMessage>();
		communication = new HashMap<Integer, Integer>();
		
		mode = Mode.EXPLORE;
		step = 0;
	}

	@Override
	public void receive(int from, byte[] message) {
		inbox.add(new AgentsMessage(from, message));
	}

	@Override
	public void state(int stamp, Neighborhood neighborhood,
			Direction direction, boolean hasFlag) {
		
		states.add(new StateMessage(neighborhood, hasFlag, direction));
	}

	@Override
	public void run() {
		
		//first trigger scan request, so that we get initial state
		scan(0);
		
		Position nextTarget = null;
		while(isAlive())
		{	
			//process all received messages
			while(!inbox.isEmpty())
			{
				localMap.updateMap(inbox.poll());
			}
			
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
					
					localMap.updateMap(msg);
					
					if(debug)
					{
						System.out.println("Local map after merging with received neighborhood");
						localMap.printLocalMap();
					}
				}
				
				//get all visible friendly agents
				ArrayList<Integer> agents = localMap.getFriendlyAgents(messageDistance);
				for(Integer a : agents)
				{
					sendMessage(a);
				}
				
				int iteration = 0;
				do
				{
					//finding next target field based on agent mode					
					nextTarget = findTarget(iteration);
					
					iteration ++;
	
					//compute next move based on nextTarget
					
					//TODO: perform planning on other thread and here wait
					//for specific limit and use old plan if computing takes too long
					
					//plan = new ConcurrentLinkedQueue<Direction>(localMap.dijkstraPlan(nextTarget, mode == Mode.HOMERUN || mode == Mode.NEARHQ));
					plan = new ConcurrentLinkedQueue<Direction>(localMap.aStarPlan(nextTarget, mode == Mode.HOMERUN || mode == Mode.NEARHQ));
					
					if(plan == null || plan.size() == 0)
					{	
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
						
						if(debug)
						{
							System.out.println("Next target will be chosen randomly among nearest");
						}
					}
				
				}while(plan.size() == 0 && iteration < 10);

				Direction nextMove = plan.poll();
				
				//move agent if it can be safely moved or
				//if this move is the last in plan (flag or hq)
				if(iteration < 10 && (plan.isEmpty() || localMap.canSafelyMove(nextMove, (mode == Mode.HOMERUN || mode == Mode.NEARHQ))))
				{
					if(debug)
					{
						System.out.println("Move: " + nextMove);
					}
					this.move(nextMove);
					step++;
				}
				else
				{
					if(debug)
					{
						System.out.println("Can't move " + nextMove + ". Sending direction NONE.");
					}
					this.move(Direction.NONE);
				}					
				
			}
		}
		
	}
	
	/* Private methods */

	private void changeMode(Mode m)
	{
		if(debug)
		{
			System.out.println("Changing mode to " + m.toString());
		}
		mode = m;
	}
	
	/**
	 * Send message (local map) to an agent
	 * It checks for time of last message that was sent
	 * Only one message per one real move is allowed
	 * 
	 * @param agentId the agent id
	 */
	private void sendMessage(int agentId)
	{
		Integer msgStep = communication.get(agentId);
		
		if(msgStep == null || msgStep <= step - 1 )
		{
			send(agentId, localMap.getEncodedMap());
			communication.put(agentId, step);
		}
	}
	
	private Position findTarget(int iteration)
	{
		Position nextTarget = null;
		switch(mode)
		{
			case FOODHUNT:
			case EXPLORE:
			{
				nextTarget = localMap.findNearest(FindType.FOOD, iteration);
				if(nextTarget != null)
				{
					changeMode(Mode.FOODHUNT);
				}
				else
				{
					nextTarget = localMap.findNearest(FindType.UNEXPLORED, iteration);
					
					//No flags and no unexplored places found
					if(nextTarget == null)
					{
						changeMode(Mode.REBOOT);
					}
				}
				
				break;
			}
			case REBOOT:
				localMap.clearMap();
				changeMode(Mode.EXPLORE);
				break;
			case HOMERUN:
			{
				nextTarget = localMap.findNearest(FindType.HQ, iteration);
				break;
			}
			case NEARHQ:
			{
				nextTarget = localMap.findNearest(FindType.HQ, 0);
				nextTarget = localMap.findEmptyFieldNear(nextTarget, 2);
				changeMode(Mode.HOMERUN);
				break;
			}
			case ONLYEXPLORE:
			{
				nextTarget = localMap.findNearest(FindType.UNEXPLORED, iteration);
				if(iteration == 0)
				{
					//change mode back to explore only if this is first iteration
					//otherwise we could have a lock (FOOD->ONLYEXPLORE->EXPLORE->FOOD)
					changeMode(Mode.EXPLORE);
				}
				break;
			}
		}
		
		if(debug)
		{
			System.out.println("Target:" + nextTarget);
		}
		
		return nextTarget;
	}

}
