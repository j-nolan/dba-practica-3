package main;

import java.awt.Color;

/**
 * An enumeration to indicate all the available map pixels state
 * @author James Nolan
 */
public enum MapPixel {
	FREE(Color.WHITE),
	OBSTACLE(Color.BLACK),
	BORDER(Color.DARK_GRAY),
	GOAL(Color.RED),
	UNKNOWN(Color.LIGHT_GRAY),
	DRONE(Color.GREEN);

	// Constructor gets the color representation of the pixel (to display in a map for example)
	private MapPixel(Color colorRepresentation) {
		this.colorRepresentation = colorRepresentation;
	}
	
	// Color representation
	private Color colorRepresentation;
	
	public Color getColor() {
		return colorRepresentation;
	}
}
