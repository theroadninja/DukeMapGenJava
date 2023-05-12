package trn.duke.experiments.gridblock;

import org.apache.commons.lang3.tuple.Pair;

import trn.maze.Heading;

/**
 * Re-usable set of sectors that are in the general shape of a square
 * and fit into a grid.
 * 
 * They can be connected to other sectors only at the four sides:  N,E,S,W
 * 
 * More connector possibilities:
 *  	-top and bottom, via grating
 *  	-top and bottom, via soudnless teleporter
 *  	-center, via normal teleporter
 * 
 * @author Dave
 *
 */
public interface Block {
	
	public Pair<Integer, Integer> getGridCoordinate();
	
	public Connector getConnector(Heading heading);
	public Connector getEastConnector();
	public Connector getSouthConnector();

	//public int getWallIndex(Heading heading);
	
	/**
	 * 
	 * @param map
	 * @return the index of the (a?) sector that was created.
	 */
	public int draw(trn.Map map);
	
	//TODO:  drawConnector( ... ) .....maybe not; might call the connector object directly.
	

}
