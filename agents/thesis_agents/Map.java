package thesis_agents;

import java.util.HashMap;

import org.grid.protocol.Neighborhood;
import org.grid.protocol.Position;

public class Map {

	HashMap<Position, Integer> map = new HashMap<Position, Integer>();
	Position agentLocation = null;
	int agentId;
	
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
			int xOffset = 0;
			int yOffset = 0;			
			
			UpdateMapWithNeighborhood(msg.neighborhood, xOffset, yOffset);	
		}
		
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
		
		default: return "Y";
		}
	}
}
