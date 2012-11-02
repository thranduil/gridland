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

	HashMap<Position, Integer> map = new HashMap<Position, Integer>();
	
	Position agentLocation = null;
	int agentId;
	
	//offsets : up, down, right, left
	ArrayList<int[]> offsets = new ArrayList<int[]>(Arrays.asList(new int[]{0,-1}, new int[]{0,1}, new int[]{1,0}, new int[]{-1,0}));
	
	public static enum FindType { 
		UNEXPLORED, FOOD, AGENT 
	}
	
	
	public Map(int id)
	{
		agentId = id;
	}
	
	public void updateMap(StateMessage msg)
	{		
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
	
	public void printLocalMap()
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
	
	public boolean canSafelyMove(Direction nextMove) {
		Position n = null;
		switch(nextMove)
		{
			case LEFT:
			{
				n = new Position(agentLocation.getX() - 1, agentLocation.getY());
				if(!map.containsKey(n))
				{
					return true;
				}
				
				Integer a = map.get(new Position(n.getX() - 1, n.getY()));
				Integer b = map.get(new Position(n.getX(), n.getY() - 1));
				Integer c = map.get(new Position(n.getX(), n.getY() + 1));
				
				//do not move there if on that spot can move enemy agent
				if( (a != null && a <= -6)
					|| (b != null && b <= -6)
					|| (c != null && c <= -6))
				{
					return false;
				}
				break;
			}
			case DOWN:
			{
				n = new Position(agentLocation.getX(), agentLocation.getY() + 1);
				if(!map.containsKey(n))
				{
					return true;
				}
				
				Integer a = map.get(new Position(n.getX(), n.getY() + 1));
				Integer b = map.get(new Position(n.getX() + 1, n.getY()));
				Integer c = map.get(new Position(n.getX() - 1, n.getY()));
				
				//do not move there if on that spot can move enemy agent
				if( (a != null && a <= -6)
						|| (b != null && b <= -6)
						|| (c != null && c <= -6))
				{
					return false;
				}
				
				break;
			}
			case RIGHT:
			{
				n = new Position(agentLocation.getX() + 1, agentLocation.getY());
				if(!map.containsKey(n))
				{
					return true;
				}
				
				Integer a = map.get(new Position(n.getX() + 1, n.getY()));
				Integer b = map.get(new Position(n.getX(), n.getY() - 1));
				Integer c = map.get(new Position(n.getX(), n.getY() + 1));
				
				//do not move there if on that spot can move enemy agent
				if( (a != null && a <= -6)
						|| (b != null && b <= -6)
						|| (c != null && c <= -6))
				{
					return false;
				}
				
				break;
			}
			case UP:
			{
				n = new Position(agentLocation.getX(), agentLocation.getY() - 1);
				if(!map.containsKey(n))
				{
					return true;
				}
				
				Integer a = map.get(new Position(n.getX(), n.getY() - 1));
				Integer b = map.get(new Position(n.getX() + 1, n.getY()));
				Integer c = map.get(new Position(n.getX() - 1, n.getY()));
				
				//do not move there if on that spot can move enemy agent
				if( (a != null && a <= -6)
						|| (b != null && b <= -6)
						|| (c != null && c <= -6))
				{
					return false;
				}
				
				break;
			}
			case NONE:
			{
				return true;
			}
		}
		
		int title = map.get(n);
		
		//do not move if there is wall, enemy hq or other agent
		if(title == -1 || title == -4 || title <= -6 || title > 0)
		{
			return false;
		}
		
		return true;
	}
	
	private double GetDistanceFromAgent(Position p)
	{		
		return Math.abs(agentLocation.getX() - p.getX()) + Math.abs(agentLocation.getY() - p.getY());
	}
	
	private int[] getOffset(Neighborhood n)
	{			
		for(int[] offset : this.offsets)
		{
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
		}
		
		System.err.println("Offset can not be found.");
		return new int[]{0,0};		
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
		
	public LinkedList<Direction> dijkstraPlan(Position nextTarget) {
		if(nextTarget == null)
		{
			return null;
		}
		
		HashMap<Position, Position> previous = new HashMap<Position, Position>();
		HashMap<Position, Integer> distances = new HashMap<Position, Integer>();
		
		
		//add all map fields to priorityQueue with distance set to max
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
			int nodeDistance = distances.get(node);
			//remove distance for current node, as it is no longer needed
			distances.remove(node);
			//if node has max possible distance, all next has it
			//there is no solution 
			if(nodeDistance == Integer.MAX_VALUE)
			{
				break;
			}
			
			if(node.equals(nextTarget))
			{
				//we found finish node
				LinkedList<Direction> resultPath = new LinkedList<Direction>();
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
			
			if(canMove(leftField) && distances.containsKey(leftField))
			{
				if(newDistance < distances.get(leftField))
				{
					previous.put(leftField, node);			
					distances.put(leftField, newDistance);
				}
			}
			
			if(canMove(rightField) && distances.containsKey(rightField))
			{
				if(newDistance < distances.get(rightField))
				{
					previous.put(rightField, node);			
					distances.put(rightField, newDistance);
				}
			}
			
			if(canMove(upperField) && distances.containsKey(upperField))
			{
				if(newDistance < distances.get(upperField))
				{
					previous.put(upperField, node);			
					distances.put(upperField, newDistance);
				}
			}
			
			if(canMove(downField) && distances.containsKey(downField))
			{
				if(newDistance < distances.get(downField))
				{
					previous.put(downField, node);			
					distances.put(downField, newDistance);
				}
			}
		}
		System.err.println("Path to (" + nextTarget.getX() + "," + nextTarget.getY() + ") was not found");
		return null;
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
	
	private boolean canMove(Position p)
	{
		if(map.containsKey(p) && map.get(p) != -1 && map.get(p) != -4)
		{
			return true;
		}
		return false;
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


