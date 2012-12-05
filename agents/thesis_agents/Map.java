package thesis_agents;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.PriorityQueue;

import org.grid.protocol.Neighborhood;
import org.grid.protocol.NewMessage.Direction;
import org.grid.protocol.Position;

public class Map {

	private HashMap<Position, Integer> map = new HashMap<Position, Integer>();
	private HashMap<Integer, Integer> agentsTTL = new HashMap<Integer, Integer>();
	
	private Position agentLocation = null;
	private Direction lastMove = Direction.NONE;
	private int agentId;
	
	//path with positions of last path search
	private ArrayList<Position> currentPath = new ArrayList<Position>();
	
	private boolean debug;
	
	private final int TTL = 5;
	
	
	public static enum FindType { 
		UNEXPLORED, FOOD, AGENT, HQ
	}
	
	public Map(int id, boolean debug)
	{
		agentId = id;
		this.debug = debug;
	}
	
	/**
	 * Clears map but leaves one path to HQ intact
	 */
	public void clearMap()
	{
		HashMap<Position, Integer> newMap = (HashMap<Position, Integer>) map.clone();
		aStarPlan(findNearest(FindType.HQ, 0), true, false);
		
		for(Position p : map.keySet())
		{
			if(!currentPath.contains(p))
			{
				newMap.remove(p);
			}
		}
		map = newMap;
	}
	
	public void updateMap(StateMessage msg)
	{	
		AgentsTTLCheck();
		
		lastMove = msg.direction;
		if(debug)
		{
			System.out.println("Server move: " + msg.direction);
			printNeighborhood(msg.neighborhood);
		}
		
		if(map.size() == 0)
		{
			//when map is empty add neighborhood to map like it was received
			UpdateMapWithNeighborhood(msg.neighborhood, 0, 0);
		}
		else
		{
			//if we already have elements in map do appropriate neighborhood shift			
			int offset[] = getOffset(msg.neighborhood);
			
			UpdateMapWithNeighborhood(msg.neighborhood, agentLocation.getX() + offset[0], agentLocation.getY() + offset[1]);	
		}		
	}
	
	/**
	 * Decrease TTL for each agent,
	 * remove invalid (TTL = 0) agents and
	 * update map
	 */
	private void AgentsTTLCheck() {
		ArrayList<Integer> agentsForRemoval = new ArrayList<Integer>();
		//decrease counter and mark agents
		for(Integer agentId : agentsTTL.keySet())
		{
			if(agentsTTL.get(agentId) == 1)
			{
				agentsForRemoval.add(agentId);
				continue;
			}
			agentsTTL.put(agentId, agentsTTL.get(agentId) - 1);	
		}
		
		//remove agents from ttl list
		for(Integer id : agentsForRemoval)
		{
			agentsTTL.remove(id);
			
			if(debug)
			{
				System.out.println("Removed agent " + id);
			}
		}
		
		//remove agents from map
		if(!agentsForRemoval.isEmpty())
		{
			for(Position p : map.keySet())
			{
				Integer field = map.get(p);
				if(agentsForRemoval.contains(field))
				{
					map.put(p, 0);
				}
			}
		}
	}

	public void updateMap(AgentsMessage msg)
	{
		//do not process agents msg if we don't have
		//local map filled
		if(map.isEmpty())
		{
			return;
		}
		
		HashMap<Position, Integer> receivedMap = msg.getMap();
		
		if(debug)
		{
			System.out.println("Received map");
			printMap(receivedMap);
		}
		
		//find hq in received map and get map offset
		Position offset = getOffsetForReceivedMap(receivedMap);
		
		for(Position p : receivedMap.keySet())
		{
			//skip my field
			if((p.getX() + offset.getX() == agentLocation.getX() && p.getY() + offset.getY() == agentLocation.getY()) 
					|| receivedMap.get(p) == agentId)
			{
				continue;
			}
			
			//set agent ttl to max
			if(receivedMap.get(p) > 0)
			{
				agentsTTL.put(receivedMap.get(p), TTL);
			}
			
			//update all fields
			map.put(new Position(p.getX() + offset.getX(), p.getY() + offset.getY()), receivedMap.get(p));
		}
		
		if(debug)
		{
			System.out.println("Updated map");
			printLocalMap();
		}
	}
	
	public byte[] getEncodedMap()
	{
		StringBuilder sb = new StringBuilder();
		for(Position p : map.keySet())
		{
			Integer field = map.get(p);
			
			//send only agents that are still in our neighborhood(of size 5 fields)
			if((field == -6 || field > 0) && GetDistanceFromAgent(p) > 5)
			{
				continue;
			}
			
			sb.append(p.getX()+","+p.getY()+","+map.get(p)+";");
		}
		return sb.toString().getBytes();
	}
	
	public void printLocalMap()
	{
		System.out.println("Agent id: " + agentId);
		this.printMap(map);
	}
		
	public Position findNearest(FindType type, int priority)
	{
		HashMap<Integer, Position> distanceToField = new HashMap<Integer, Position>();
		
		for(Position p : map.keySet())
		{
			int field = map.get(p);
			boolean satisfyCondition = false;
			
			switch(type)
			{
				case UNEXPLORED:
				{
					//field must be empty and one of the neighbors must not exist
					if(field == 0 && ( !map.containsKey(new Position(p.getX() + 1, p.getY()))
									|| !map.containsKey(new Position(p.getX() - 1, p.getY()))
									|| !map.containsKey(new Position(p.getX(), p.getY() + 1))
									|| !map.containsKey(new Position(p.getX(), p.getY() - 1))))
					{
						satisfyCondition = true;
					}
					break;
				}
				case FOOD:
				{
					if(field == -3 || field == -5)
					{
						satisfyCondition = true;
					}
					break;
				}
				case AGENT:
					//TODO: implement this
					break;
				case HQ:
					if(field == -2)
					{
						satisfyCondition = true;
					}
					break;
			}
			
			//check if current position is nearest among the most near
			if(satisfyCondition)
			{
				distanceToField.put(GetDistanceFromAgent(p), p);
			}
		}
		
		if(distanceToField.isEmpty())
		{
			return null;
		}
		
		Position result = null;
		
		//check distances from 1 to 100 and return smallest
		//priority indicates how many elements we skip
		for(int i = 1; i < 100; i++)
		{
			if(distanceToField.containsKey(i))
			{
				result = distanceToField.get(i);
				
				if(priority == 0)
				{
					break;
				}
				else
				{
					priority--;
				}
			}
		}
		
		return result;
	}
	
	
	/**
	 * Find empty field near given position near.
	 *
	 * @param position the position
	 * @param radius the radius of search
	 * @return the position empty position
	 */
	public Position findEmptyFieldNear(Position position, int radius)
	{
		if(position == null)
		{
			System.err.println("findEmptyFieldNear() - Position is null.");
			return new Position(0,0);
		}
		//find position in radius around given position
		for(int i = -radius; i <= radius; i++)
		{
			for(int j = -radius; j <= radius; j++)
			{
				if (map.containsKey(new Position(position.getX() + i, position.getY() + j)) 
					&& map.get(new Position(position.getX() + i, position.getY() +j)) == 0) {
					
					return new Position(position.getX() + i, position.getY() +j);
				}
			}
		}

		return position;
	}
	
	
	public boolean canSafelyMove(Direction nextMove, boolean agentHasFood, boolean canMoveToEnemyAgent) {
		Position n = null;
		
		//get next Position and get positions to check for enemies (n1,n2,n3)
		Position n1 = null;
		Position n2 = null;
		Position n3 = null;
		
		switch(nextMove)
		{
			case LEFT:
			{
				n = new Position(agentLocation.getX() - 1, agentLocation.getY());
				
				n1 = new Position(n.getX(), n.getY() + 1);
				n2 = new Position(n.getX(), n.getY() - 1);
				n3 = new Position(n.getX() - 1, n.getY());
				
				break;
			}
			case DOWN:
			{
				n = new Position(agentLocation.getX(), agentLocation.getY() + 1);
				
				n1 = new Position(n.getX() + 1, n.getY());
				n2 = new Position(n.getX() - 1, n.getY());
				n3 = new Position(n.getX(), n.getY() + 1);
				
				break;
			}
			case RIGHT:
			{
				n = new Position(agentLocation.getX() + 1, agentLocation.getY());
				
				n1 = new Position(n.getX(), n.getY() + 1);
				n2 = new Position(n.getX(), n.getY() - 1);
				n3 = new Position(n.getX() + 1, n.getY());
				
				break;
			}
			case UP:
			{
				n = new Position(agentLocation.getX(), agentLocation.getY() - 1);
				
				n1 = new Position(n.getX() + 1, n.getY());
				n2 = new Position(n.getX() - 1, n.getY());
				n3 = new Position(n.getX(), n.getY() - 1);
				
				break;
			}
			case NONE:
			{
				return true;
			}
		}
		
		//agent can't move to field that are not empty,food or hq
		if(map.containsKey(n) 
				&& map.get(n) != 0		//empty
				&& map.get(n) != -2 	//hq
				&& map.get(n) != -3 	//food
				&& map.get(n) != -5)	//food
		{
			return false;
		}
		
			
		//check if in near fields are enemy agents
		if(!canMoveToEnemyAgent && map.containsKey(n1) && (map.get(n1) == -6
				|| map.containsKey(n2) && map.get(n2) == -6
				|| map.containsKey(n3) && map.get(n3) == -6))
		{
			if(Math.random() > 0.1)
			{
				//don't move if enemy agent is in field near in 90%
				return false;
			}
			else if(debug)
			{
				System.out.println("I shouldn't move, but i will.");
			}
		}
		
		boolean canMove = true;
		
		for(int x = -2; x <= 2; x++)
		{
			for(int y = -2; y<= 2; y++)
			{
				int ax = Math.abs(x);
				int ay = Math.abs(y);
				
				//if agent doesn't carry food look only one field around target place
				//and don't look diagonal positions
				if(!agentHasFood && ( ax == 2 || ay == 2 || ax + ay == 2))
				{
					continue;
				}
				
				//if agent carry food, skip positions that are more than 2 fields far
				if(agentHasFood && ax + ay > 2)
				{
					continue;
				}

				
				Position p = new Position(n.getX() + x, n.getY() + y);
				if(map.containsKey(p))
				{
					//agents with lower id have priority
					int id = map.get(p);
					if( id > 0 &&  id < agentId)
					{
						canMove = false;
					}
				}
			}
		}
		
		return canMove;
	}
			
	public LinkedList<Direction> dijkstraPlan(Position nextTarget, boolean returnToHQ, boolean includeEnemyAgent) {
		
		HashMap<Position, Position> previous = new HashMap<Position, Position>();
		HashMap<Position, Integer> distances = new HashMap<Position, Integer>();
		LinkedList<Direction> resultPath = new LinkedList<Direction>();
		
		if(nextTarget == null)
		{
			return resultPath;
		}
		
		//add all map fields to list with distance set to max
		for(Position p : map.keySet())
		{
			if(p.equals(agentLocation))
			{
				distances.put(p, 0);
				previous.put(p, null);
				continue;
			}
			
			distances.put(p, Integer.MAX_VALUE);
		}
		
		while(!distances.isEmpty())
		{
			Position node = getSmallestDistance(distances);
			
			//if node has max possible distance, all next has it
			//there is no solution 
			if(node == null || distances.get(node) == Integer.MAX_VALUE)
			{
				break;
			}
			
			int nodeDistance = distances.get(node);
			//remove distance for current node, as it is no longer needed
			distances.remove(node);
			
			if(node.equals(nextTarget))
			{
				//we found finish node
				while(previous.get(node) != null)
				{
					resultPath.addFirst(getDirectionFrom(previous.get(node), node));
					currentPath.add(node);
					node = previous.get(node);
				}
				return resultPath;
			}
			
			//check all neighbor fields
			Position leftField = new Position(node.getX() - 1, node.getY());
			Position rightField = new Position(node.getX() + 1, node.getY());
			Position upperField = new Position(node.getX(), node.getY() - 1);
			Position downField = new Position(node.getX(), node.getY() + 1);
			
			int newDistance = nodeDistance + 1;
			
			if(canMove(leftField, returnToHQ, includeEnemyAgent) && distances.containsKey(leftField))
			{
				if(newDistance < distances.get(leftField))
				{
					previous.put(leftField, node);			
					distances.put(leftField, newDistance);
				}
			}
			
			if(canMove(rightField, returnToHQ, includeEnemyAgent) && distances.containsKey(rightField))
			{
				if(newDistance < distances.get(rightField))
				{
					previous.put(rightField, node);			
					distances.put(rightField, newDistance);
				}
			}
			
			if(canMove(upperField, returnToHQ, includeEnemyAgent) && distances.containsKey(upperField))
			{
				if(newDistance < distances.get(upperField))
				{
					previous.put(upperField, node);			
					distances.put(upperField, newDistance);
				}
			}
			
			if(canMove(downField, returnToHQ, includeEnemyAgent) && distances.containsKey(downField))
			{
				if(newDistance < distances.get(downField))
				{
					previous.put(downField, node);			
					distances.put(downField, newDistance);
				}
			}
		}
		if(debug)
		{
			System.err.println("Path to (" + nextTarget.getX() + "," + nextTarget.getY() + ") was not found");
		}
		return resultPath;
	}
	
	public LinkedList<Direction> aStarPlan(Position nextTarget, boolean returnToHQ, boolean includeEnemyAgent)
	{
		LinkedList<Direction> result = new LinkedList<Direction>();
		
		if(nextTarget == null)
		{
			return result;
		}
		
		PriorityQueue<ANode> openSet = new PriorityQueue<ANode>();
		HashSet<Position> closedSet = new HashSet<Position>();
		HashMap<Position, Integer> gScore = new HashMap<Position, Integer>();
		HashMap<Position, Position> cameFrom = new HashMap<Position, Position>();
		
		openSet.add(new ANode(agentLocation, GetManhattanDistance(agentLocation, nextTarget)));
		gScore.put(agentLocation, 0);
		
		while(!openSet.isEmpty())
		{
			ANode current = openSet.poll();
			
			if(current.getPosition().equals(nextTarget))
			{
				ArrayList<Position> path = new ArrayList<Position>();
				
				Position p = current.getPosition();
				while(cameFrom.containsKey(p))
				{
					result.addFirst(getDirectionFrom(cameFrom.get(p), p));
					path.add(p);
					p = cameFrom.get(p);
				}
				return result;
			}
			
			closedSet.add(current.getPosition());
			
			for(Position neighbor : getNeighborNodes(current, returnToHQ, includeEnemyAgent))
			{
				if(closedSet.contains(neighbor))
				{
					continue;
				}
				
				int newGScore = gScore.get(current.getPosition()) + 1;
				
				if(!openSet.contains(neighbor) || (gScore.containsKey(neighbor) && newGScore <= gScore.get(neighbor)))
				{
					cameFrom.put(neighbor, current.getPosition());
					gScore.put(neighbor, newGScore);
					
					//remove it if exist and then add it
					openSet.remove(new ANode(neighbor, 100));
					openSet.add(new ANode(neighbor, newGScore + GetManhattanDistance(neighbor, nextTarget)));
				}
			}
		}
		
		if(debug)
		{
			System.err.println("Path to (" + nextTarget.getX() + "," + nextTarget.getY() + ") was not found");
		}
		return result;	
	}
	
	
	public ArrayList<Integer> getFriendlyAgents(int radius)
	{
		ArrayList<Integer> agents = new ArrayList<Integer>();
		
		for(int x = -radius; x <= radius; x++)
		{
			for(int y = -radius; y <= radius; y++)
			{
				if(x == 0 && y == 0)
				{
					continue;
				}
				
				Position current = new Position(agentLocation.getX() + x, agentLocation.getY() + y);
				if(map.containsKey(current) && map.get(current) > 0)
				{
					agents.add(map.get(current));
				}
			}
		}
		
		return agents;
	}	
	
	public Position getNearbyEnemyAgent(int radius)
	{
		Position result = null;
		
		for(int x = -radius; x <= radius; x++)
		{
			for(int y = -radius; y <= radius; y++)
			{
				Position current = new Position(agentLocation.getX() + x, agentLocation.getY() + y);
				
				if(map.containsKey(current) && map.get(current) == -6)
				{
					if(result == null || GetDistanceFromAgent(current) < GetDistanceFromAgent(result))
					{
						result = current;
					}
				}
			}
		}
		
		return result;
	}
	
	
	private int GetDistanceFromAgent(Position p)
	{		
		return GetManhattanDistance(agentLocation, p);
	}
	
	private int GetManhattanDistance(Position p1, Position p2)
	{
		return Math.abs(p1.getX() - p2.getX()) + Math.abs(p1.getY() - p2.getY());
	}
	
	
	private int[] getOffset(Neighborhood n)
	{	
		int[] offset = null;
		
		switch(lastMove)
		{
			case UP:
				offset = new int[]{0,-1};
				break;
			case DOWN:
				offset = new int[]{0,1};
				break;
			case NONE:
				offset = new int[]{0,0};
				break;
			case LEFT:
				offset = new int[]{-1,0};
				break;
			case RIGHT:
				offset = new int[]{1,0};
				break;
		}
		
		return offset;
	}
	
	
	private Position getOffsetForReceivedMap(HashMap<Position, Integer> receivedMap)
	{
		Position localHQ = null;
		Position remoteHQ = null;
		
		//get hq from local and received map
		for(int x = -1; x <= 1; x++)
		{
			for(int y = -1; y <= 1; y++)
			{
				Position p = new Position(x,y);
				if(map.containsKey(p) && map.get(p) == -2)
				{
					localHQ = p;
				}
				
				if(receivedMap.containsKey(p) && receivedMap.get(p) == -2)
				{
					remoteHQ = p;
				}
			}
		}
		
		if(localHQ == null || remoteHQ == null)
		{
			System.err.println("Could not get offset for received map.");
			System.out.println("Received map");
			printMap(receivedMap);
			System.out.println("Current local map");
			printLocalMap();
			return null;
		}
		
		return new Position(localHQ.getX() - remoteHQ.getX(), localHQ.getY() - remoteHQ.getY());
	}
	
	
	private void UpdateMapWithNeighborhood(Neighborhood n, int offsetX, int offsetY)
	{
		for(int y = -n.getSize(); y <= n.getSize(); y++)
		{
			for(int x = -n.getSize(); x <= n.getSize(); x++)
			{
				int field = n.getCell(x, y);
				Position pos = new Position(x + offsetX, y + offsetY);
				
				map.put(pos, field);
				
				if(field == agentId)
				{
					agentLocation = pos;
				}
				else if(field > 0)
				{
					agentsTTL.put(field, TTL);
				}
			}
		}
	}
	
	
	private Direction getDirectionFrom(Position from, Position to) {
		if(from.getX() + 1 == to.getX())
		{
			return Direction.RIGHT;
		}
		if(from.getX() - 1 == to.getX())
		{
			return Direction.LEFT;
		}
		if(from.getY() + 1 == to.getY())
		{
			return Direction.DOWN;
		}
		if(from.getY() - 1 == to.getY())
		{
			return Direction.UP;
		}
		
		System.err.println("Could not find direction from ("+from.getX()+","+from.getY()+") to ("+to.getX()+","+to.getY()+").");
		return Direction.NONE;
	}

	
	private Position getSmallestDistance(HashMap<Position, Integer> distances)
	{
		int distance = Integer.MAX_VALUE;
		Position result = null;
		
		for(Position p : distances.keySet())
		{
			if(distances.get(p) < distance)
			{
				distance = distances.get(p);
				result = p;
			}
		}
		
		return result;
	}
	
	
	private boolean canMove(Position p, boolean includeHQ, boolean includeEnemyAgent)
	{
		//when we are returning to hq agent can move to hq, but not on food
		//in other cases agent can't move to hq, but can move to food
		if(map.containsKey(p) 
				&& (map.get(p) == 0 
				|| (map.get(p) == -3 && !includeHQ) 
				|| (map.get(p) == -5 && !includeHQ) 
				|| (map.get(p) == -2 && includeHQ)
				|| (map.get(p) == -6 && includeEnemyAgent)))
		{
			return true;
		}
		return false;
	}
	
	
	/**
	 * Returns neighbor nodes(Positions)
	 * that are empty or (depending on includeHQ) are food/hq
	 */
	private ArrayList<Position> getNeighborNodes(ANode current, boolean includeHQ, boolean includeEnemyAgent) {
		ArrayList<Position> result = new ArrayList<Position>();
		
		Position left = current.getPositionWithOffset(-1, 0);
		Position right = current.getPositionWithOffset(1, 0);
		Position up = current.getPositionWithOffset(0, -1);
		Position down = current.getPositionWithOffset(0, 1);
		
		if(canMove(left, includeHQ, includeEnemyAgent))
		{
			result.add(left);
		}
		if(canMove(right, includeHQ, includeEnemyAgent))
		{
			result.add(right);
		}
		if(canMove(up, includeHQ, includeEnemyAgent))
		{
			result.add(up);
		}
		if(canMove(down, includeHQ, includeEnemyAgent))
		{
			result.add(down);
		}
		
		return result;
	}
	
	
	private void printMap(HashMap<Position, Integer> map)
	{
		for(int y = -40; y <= 1; y++)
		{
			for(int x = -40; x <= 1; x++)
			{ 
				Position pos = new Position(x, y);
				int a = 1;
				if(map.containsKey(pos))
				{
					a = map.get(pos);
				}
				
				System.out.print(getTitle(a) + " ");
			}
			System.out.println();
		}
	}
	
	
	private void printNeighborhood(Neighborhood n)
	{
		System.out.println("Neighborhood (" + n.getWidth() + "x" + n.getHeight() + ")");
		for(int y = -n.getSize(); y <= n.getSize(); y++)
		{
			for(int x = -n.getSize(); x <= n.getSize(); x++)
			{
				System.out.print(getTitle(n.getCell(x, y)) + " ");
			}
			System.out.println();
		}
	}
	
	
	private String getTitle(int title)
	{
		switch(title)
		{
		case 0: return "_";
		case -1: return "X";
		case -2: return "H";
		case -3: return "F";
		case -4: return "H";
		case -5: return "F";
		
		default: 
			if(title == this.agentId)
			{
				//that's me!
				return "A";
			}
			if(title > 1)
			{
				//friendly agent
				return "a";
			}
			if(title < 0)
			{
				//enemy agent
				return "E";
			}
			return "Y";
		}
	}
}