package thesis_agents;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import org.grid.protocol.Neighborhood;
import org.grid.protocol.NewMessage.Direction;
import org.grid.protocol.Position;

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
	
	public void UpdateMap(StateMessage msg)
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
	
	public void PrintLocalMap()
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
				
				System.out.print(GetTitle(a) + " ");
			}
			System.out.println();
		}
	}

	public Direction GetNextMove()
	{
		//check for proximity of other agent and move accordingly
		
		return Direction.NONE;
	}
	
	public Position FindNearest(FindType type)
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
					break;
				default:
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
	
	private double GetDistanceFromAgent(Position p)
	{		
		return Math.sqrt(Math.pow(agentLocation.getX() - p.getX(),2)) + Math.sqrt(Math.pow(agentLocation.getY() - p.getY(), 2));
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
		
	private String GetTitle(int title)
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
				return "A";
			}
			return "Y";
		}
	}

}
