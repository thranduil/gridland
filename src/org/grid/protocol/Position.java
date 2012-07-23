/**
 * 
 */
package org.grid.protocol;

import java.io.Serializable;

// TODO: Auto-generated Javadoc
public class Position implements Serializable {
	
	private static final long serialVersionUID = 1L;

	private int x;
	
	private int y;

	public int getX() {
		return x;
	}
	
	public void setX(int x) {
		this.x = x;
	}
	
	public int getY() {
		return y;
	}
	
	public void setY(int y) {
		this.y = y;
	}
	

	/**
	 * Instantiates a new position.
	 *
	 * @param x the x
	 * @param y the y
	 */
	public Position(int x, int y) {
		super();
		this.x = x;
		this.y = y;
	}
	
	/**
	 * Instantiates a new position.
	 *
	 * @param p the p
	 */
	public Position(Position p) {
		super();
		this.x = p.x;
		this.y = p.y;
	}
	
	/**
	 * Instantiates a new position.
	 *
	 * @param p the p
	 * @param factor the factor
	 */
	public Position(Position p, int factor) {
		super();
		this.x = p.x * factor;
		this.y = p.y * factor;
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#toString()
	 */
	public String toString() {
		return String.format("Position: %d, %d", getX(), getY());
	}
	
	/* (non-Javadoc)
	 * @see java.lang.Object#equals(java.lang.Object)
	 */
	@Override
	public boolean equals(Object obj) {
		if (obj != null && obj instanceof Position) 
			return (((Position) obj).x == x && ((Position) obj).y == y);
		else return false;
	}

    /* (non-Javadoc)
     * @see java.lang.Object#hashCode()
     */
    @Override
    public int hashCode()
    {
        int hash = 17;
        hash = 31*hash + x;
        hash = 31*hash + y;
        return hash;
    }

    /**
     * Distance.
     *
     * @param p1 the p1
     * @param p2 the p2
     * @return the int
     */
    public static int distance(Position p1, Position p2) {
		return Math.max(Math.abs(p1.getX() - p2.getX()), Math.abs(p1.getY()
				- p2.getY()));
    }
    
    /**
     * Offset.
     *
     * @param p the p
     */
    public void offset(Position p) {
    	
    	x += p.x;
    	y += p.y;
    	
    }
}
