package trn.duke.experiments.gridblock;

import org.apache.commons.lang3.tuple.Pair;

import trn.PointXY;
import trn.maze.Heading;

public abstract class AbstractBlock implements Block {

	public static final int WALL_LENGTH = 2048; //2 x largest grid size

	protected final Pair<Integer, Integer> gridCoordinate;
	
	public AbstractBlock(Pair<Integer, Integer> gridCoordinate){
		this.gridCoordinate = gridCoordinate;
	}
	
	@Override
	public final Pair<Integer, Integer> getGridCoordinate() {
		return this.gridCoordinate;
	}
	
	public final int getOuterWallLength(){
		return WALL_LENGTH;
	}
	
	public final int getWestEdge(){
		return gridCoordinate.getLeft() * WALL_LENGTH;
	}
	
	public final int getEastEdge(){
		return (gridCoordinate.getLeft() + 1) * WALL_LENGTH;
	}
	
	public final int getNorthEdge(){
		return gridCoordinate.getRight() * WALL_LENGTH;
	}

	public final int getSouthEdge(){
		return (gridCoordinate.getRight() + 1) * WALL_LENGTH;
	}
	
	public PointXY getCenter(){
		return new PointXY((getWestEdge() + getEastEdge()) / 2,
				(getNorthEdge() + getSouthEdge()) / 2);
	}

	public final LegacyConnector getEastConnector(){
		return getConnector(Heading.EAST);
	}
	public final LegacyConnector getSouthConnector(){
		return getConnector(Heading.SOUTH);
	}

}
