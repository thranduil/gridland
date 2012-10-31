package thesis_agents;

import org.grid.protocol.Position;

public class GraphNode {

	Position position;
	int distance;
	
	public GraphNode(Position p, int dist)
	{
		position = p;
		distance = dist;
	}
	
	public Position getPosition() {
		return position;
	}
	
	public int getDistance() {
		return distance;
	}

}
