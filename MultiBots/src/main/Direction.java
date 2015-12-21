package main;

/**
 * Enumeration of all the available directions. Each direction is associated with a command to help
 * the program translate a direction to a commande. For example, the NORTH direction has the moveN command
 * @author James Nolan
 */
public enum Direction {
	NORTH("moveN", 0, -1),
	NORTH_EAST("moveNE", 1, -1),
	EAST("moveE", 1, 0),
	SOUTH_EAST("moveSE", 1, 1),
	SOUTH("moveS", 0, 1),
	SOUTH_WEST("moveSW", -1, 1),
	WEST("moveW", -1, 0),
	NORTH_WEST("moveNW", -1, -1);
	
	// Constructor takes the command string as paramter. It also takes the x and y coordinates of the 2D direction of
	// the direction, using the convention:
	// x = 1 => right
	// x = -1 => left
	// y = 1 => down
	// y = -1 = up
	private Direction(String command, int xCoord, int yCoord) {
		this.command = command;
		this.xCoord = xCoord;
		this.yCoord = yCoord;
	}
	
	// Each direction has an associated command (NORTH has the moveN command), xcoord and y coord
	private String command;
	private int xCoord;
	private int yCoord;
	
	// Cast to string returns the command value
	public String toString() {
		return this.command;
	}
	
	// Get x coord of the directional vector
	public int getXCoord() {
		return xCoord;
	}
	
	// Get y coord of the directional vector
	public int getYCoord() {
		return yCoord;
	}
}
