package thesis_agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.PriorityQueue;
import java.util.Set;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.grid.protocol.Neighborhood;
import org.grid.protocol.NewMessage.Direction;
import org.grid.protocol.Position;

import agents.LocalMap;

public class Map {

	private HashMap<Position, Integer> map = new HashMap<Position, Integer>();
	
	private Position agentLocation = null;
	private Direction lastMove = Direction.NONE;
	private int agentId;
		
	public static enum FindType { 
		UNEXPLORED, FOOD, AGENT, HQ
	}
	
	
	public Map(int id)
	{
		agentId = id;
	}
	
	public void updateMap(StateMessage msg, boolean debug)
	{	
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
	
	public void updateMap(AgentsMessage msg, boolean debug)
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
			if((map.containsKey(p) && map.get(p) == agentId)
				|| (receivedMap.get(p) == agentId))
			{
				continue;
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
			sb.append(p.getX()+","+p.getY()+","+map.get(p)+";");
		}
		return sb.toString().getBytes();
	}
	
	public void printLocalMap()
	{
		System.out.println("Agent id: " + agentId);
		this.printMap(map);
	}
		
	public Position findNearest(FindType type)
	{
		Position nearest = null;
		
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
			
			//check if current position is nearest
			if(satisfyCondition)
			{
				if(nearest == null || GetDistanceFromAgent(p) < GetDistanceFromAgent(nearest))
				{
					nearest = p;
				}
			}
		}
		
		return nearest;
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
	
	public boolean canSafelyMove(Direction nextMove, boolean agentHasFood) {
		Position n = null;
		switch(nextMove)
		{
			case LEFT:
			{
				n = new Position(agentLocation.getX() - 1, agentLocation.getY());
				break;
			}
			case DOWN:
			{
				n = new Position(agentLocation.getX(), agentLocation.getY() + 1);
				break;
			}
			case RIGHT:
			{
				n = new Position(agentLocation.getX() + 1, agentLocation.getY());
				break;
			}
			case UP:
			{
				n = new Position(agentLocation.getX(), agentLocation.getY() - 1);
				break;
			}
			case NONE:
			{
				return true;
			}
		}
		
		//agent can't move to field that are not empty,food or hq
		if(map.containsKey(n) 
				&& map.get(n) != 0 
				&& map.get(n) != -2 
				&& map.get(n) != -3 
				&& map.get(n) != -5)
		{
			return false;
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
					//agents (our or enemy) with lower id have priority
					int id = Math.abs(map.get(p));
					if( id > 6 &&  id < agentId)
					{
						canMove = false;
					}
				}
			}
		}
		
		return canMove;
	}
			
	public LinkedList<Direction> dijkstraPlan(Position nextTarget) {
		
		HashMap<Position, Position> previous = new HashMap<Position, Position>();
		HashMap<Position, Integer> distances = new HashMap<Position, Integer>();
		LinkedList<Direction> resultPath = new LinkedList<Direction>();
		
		if(nextTarget == null)
		{
			return resultPath;
		}
		
		//find out if target field is HQ 
		boolean returnToHQ = false;
		if(map.get(nextTarget) == -2)
		{
			returnToHQ = true;
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
			
			if(canMove(leftField, returnToHQ) && distances.containsKey(leftField))
			{
				if(newDistance < distances.get(leftField))
				{
					previous.put(leftField, node);			
					distances.put(leftField, newDistance);
				}
			}
			
			if(canMove(rightField, returnToHQ) && distances.containsKey(rightField))
			{
				if(newDistance < distances.get(rightField))
				{
					previous.put(rightField, node);			
					distances.put(rightField, newDistance);
				}
			}
			
			if(canMove(upperField, returnToHQ) && distances.containsKey(upperField))
			{
				if(newDistance < distances.get(upperField))
				{
					previous.put(upperField, node);			
					distances.put(upperField, newDistance);
				}
			}
			
			if(canMove(downField, returnToHQ) && distances.containsKey(downField))
			{
				if(newDistance < distances.get(downField))
				{
					previous.put(downField, node);			
					distances.put(downField, newDistance);
				}
			}
		}
		System.err.println("Path to (" + nextTarget.getX() + "," + nextTarget.getY() + ") was not found");
		return resultPath;
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
	
	private double GetDistanceFromAgent(Position p)
	{		
		return Math.abs(agentLocation.getX() - p.getX()) + Math.abs(agentLocation.getY() - p.getY());
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
		
		/*
		boolean aligned = true;
		
		int xStart = (offset[0] == -1 ) ? -n.getSize() + 1 : -n.getSize();
		int xEnd = (offset[0] == 1) ? n.getSize() - 1 : n.getSize();
		int yStart = (offset[1] == -1 ) ? -n.getSize() + 1 : -n.getSize();
		int yEnd = (offset[1] == 1) ? n.getSize() -1 : n.getSize();
		
		for(int y = yStart; y <= yEnd; y++)
		{
			for(int x = xStart; x <= xEnd; x++)
			{
				int current = n.getCell(x, y);
				
				//check only wall, empty space or hq
				if(current == 0 || current == -1 || current == -2 || current == -4)
				{
					int local = map.get(new Position(x + agentLocation.getX() + offset[0], y + agentLocation.getY() + offset[1] ));
					
					//skip agents
					if(local > 0) continue;
					
					if(current != local)
					{
						aligned = false;
						break;
					}
				}
			}
			if(aligned == false)
			{
				break;
			}
		}
		
		if(aligned)
		{
			return offset;
		}
		
		System.err.println("\nOffset for neighborhood can not be found.");
		System.out.println("Last move:" + lastMove);
		printNeighborhood(n);
		System.out.println("Local map");
		printLocalMap();
		return new int[]{0,0};	
		*/	
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
	
	private boolean canMove(Position p, boolean includeHQ)
	{
		//when we are returning to hq agent can move to hq, but not on food
		//in other cases agent can't move to hq, but can move to food
		if(map.containsKey(p) && (map.get(p) == 0 || (map.get(p) == -3 && !includeHQ) || (map.get(p) == -5 && !includeHQ) || (map.get(p) == -2 && includeHQ)))
		{
			return true;
		}
		return false;
	}
	
	private void printMap(HashMap<Position, Integer> map)
	{
		for(int y = -10; y <= 10; y++)
		{
			for(int x = -10; x <= 10; x++)
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


