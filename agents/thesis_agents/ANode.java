package thesis_agents;

import org.grid.protocol.Position;

public class ANode extends Position implements Comparable<ANode> {

	private static final long serialVersionUID = 1L;
	
	int cost;
	
	public ANode(int x, int y, int cost) {
		super(x, y);
		this.cost = cost;
	}
	
	public ANode(Position p, int cost)
	{
		super(p);
		this.cost = cost;
	}
	
	public Position getPosition()
	{
		return new Position(this.getX(), this.getY());
	}
	
	public Position getPositionWithOffset(int x, int y)
	{
		return new Position(this.getX() + x, this.getY() + y);
	}
	
	public int getCost()
	{
		return this.cost;
	}
	
	public void setCost(int cost)
	{
		this.cost = cost;
	}
	
	@Override
	public int compareTo(ANode o) {
		if(o.getCost() > this.cost)
		{
			return -1;
		}
		else if(o.getCost() < this.cost)
		{
			return 1;
		}
		else
		{
			return 0;
		}
	}
	
	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof ANode) 
			return (((ANode) obj).getX() == this.getX() && ((ANode) obj).getY() == this.getY());
		else return false;
	}

}
