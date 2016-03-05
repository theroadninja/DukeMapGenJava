package trn.duke.experiments.gridblock;

import org.apache.commons.lang3.tuple.Pair;

import trn.maze.Heading;

/**
 * a block that has a standard exit to the north and south.
 * 
 * @author Dave
 *
 */
public abstract class VertBlock extends AbstractBlock implements Block {
	
	protected final NorthSouthConnector northConnector;
	protected final NorthSouthConnector southConnector;

	public VertBlock(Pair<Integer, Integer> gridCoordinate) {
		super(gridCoordinate);
		
		northConnector = NorthSouthConnector.northEdge(this);
		southConnector = NorthSouthConnector.southEdge(this);
		
	}
	
	protected void setFloorZAllConnectors(Integer z){
		northConnector.setFloorZ(z);
		southConnector.setFloorZ(z);
	}

	@Override
	public final Connector getConnector(Heading heading) {
		
		if(Heading.NORTH == heading){
			return northConnector;
		}else if(Heading.SOUTH == heading){
			return southConnector;
		}else{
			return null;
		}
	}


}
