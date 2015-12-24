package main;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.HeadlessException;
import java.awt.Point;

import javax.swing.JFrame;
import javax.swing.JPanel;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;

import edu.emory.mathcs.backport.java.util.Arrays;

public class Map extends JFrame {
    private static final int SIZE_X = 100;
    private static final int SIZE_Y = 100;

    private MapPixel[][] matrix = new MapPixel[SIZE_Y][SIZE_X];
    
    private JPanel panel = new JPanel() {

        public void paintComponent(Graphics g) {
        	super.paintComponent(g);
        	
        	// Draw each pixel
            for (int y=0; y<SIZE_Y; ++y) {
                for (int x=0; x<SIZE_X; ++x) {
            		g.setColor(matrix[y][x].getColor());
                	g.fillRect(x, y, 1, 1);
                }
            }
        }
    };

    public Map(String title, Point location, Dimension dimension) throws HeadlessException {
        
        // Initially the map is in unknown state for each pixel
		for (MapPixel[] row : matrix) {
			Arrays.fill(row, MapPixel.UNKNOWN);
		}
		
		setTitle(title);
        setPreferredSize(dimension);
        setResizable(false);
        setBackground(Color.WHITE);
        setContentPane(panel);
        pack();
        createBufferStrategy(2);
        setLocation(location);
        setVisible(true);
    }
    
	/**
	 * A utility method that update the internal representation of the map using a JSONArray received by the server.
	 * It takes care of resizing the map if necesarry (the map will grow during the exploration since we don't know
	 * the actual size of the world)
	 * @param x_pos the x position of the center of the sensor matrix. That should be the position of the robot
	 * @param y_pos the y position of the center of the sentor matrix. That should be the position of the robot
	 * @param sensor the JSONArray sent by the server. It must be an flat list of the matrix containing the state of
	 * all the pixels around the robot
	 * @author James Nolan
	 */
	public void update(int x_pos, int y_pos, JSONArray sensor) {

		// We don't know the size of the world. The internal representation of the world is on a fixed size map, so
		// we first check if we need to grow the internal map in order to add the new data from the sensor.
		// We first compute the world's theorical minimum size according to the actual position of the bot
		int worldMinWidth = 1 + x_pos;
		int worldMinHeight = 1 + y_pos;
		
		// We now have the minimum theorical map size. If our internal map doesn't fit this size, we have to allocate
		// more "pixels" to it
		if (worldMinWidth > matrix[0].length || worldMinHeight > matrix.length) {
			// Create a new map with the correct dimension, and fill it with default "unknown" pixels
			MapPixel[][] newMatrix = new MapPixel[worldMinHeight][worldMinWidth];
			for (MapPixel[] row : newMatrix) {
				Arrays.fill(row, MapPixel.UNKNOWN);
			}
			
			// Copy the old value of the map in the new map.
			for (int i = 0; i < matrix.length; ++i) {
				for (int j = 0; j < matrix[i].length; ++j) {
					newMatrix[i][j] = matrix[i][j];
				}
			}
			
			// Erase old map and use new
			matrix = newMatrix;
		}
		
		// The map now has enough place to fill it with the sensor's data
		MapPixel[] states = MapPixel.values();
		
		// The radar vision is assumed to be square, so 9 cells is a 3x3 map, 4 is a 2x2...
		int radarSize = (int)Math.sqrt(sensor.length());
		
		for (int i = 0; i < sensor.length(); ++i) {
			// 1D to 2D coordinates
			int x_coord = (i % radarSize) - radarSize/2;
			int y_coord = (i / radarSize) - radarSize/2;
			try {
				// Ignore borders that are off the map, we just don't draw them
				if (y_pos + y_coord >= 0 && x_pos + x_coord >= 0 && y_pos + y_coord < 100 && x_pos + x_coord < 100) { // WARNING: I INCLUDED THE <100 PART SO IT DIDNT CRASH
					matrix[y_pos + y_coord][x_pos + x_coord] = states[sensor.getInt(i)];
				}
			} catch (JSONException e) {
				e.printStackTrace();
			}
		}
		
		// Repaint map
		repaint();
	}
	
	/**
	 * A function to know whether a given move is valid. A move is valid only if it doesn't go into a wall or an obstacle or an unknown position
	 * @param robotX the actual x position of the robot
	 * @param robotY the actual y position of the robot
	 * @param direction the desired direction
	 * @return true if move is valid, false otherwise
	 */
	public boolean validMove(int robotX, int robotY, Direction direction) {
		int intention_x = robotX + direction.getXCoord();
		int intention_y = robotY + direction.getYCoord();
		if (intention_x >= 0 && intention_y >= 0) {
			System.out.println("State at " + intention_x + ", " + intention_y + " is " + matrix[intention_y][intention_x]);
		}
		return (intention_x >= 0 && intention_y >= 0 && (matrix[intention_y][intention_x] == MapPixel.FREE || matrix[intention_y][intention_x] == MapPixel.GOAL));
	}
}