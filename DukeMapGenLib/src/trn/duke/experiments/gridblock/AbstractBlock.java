package trn.duke.experiments.gridblock;

import org.apache.commons.lang3.tuple.Pair;

import trn.PointXY;

public abstract class AbstractBlock implements Block {

	protected final Pair<Integer, Integer> gridCoordinate;
	
	public AbstractBlock(Pair<Integer, Integer> gridCoordinate){
		this.gridCoordinate = gridCoordinate;
	}
	
	@Override
	public final Pair<Integer, Integer> getGridCoordinate() {
		return this.gridCoordinate;
	}
	
	public final int getOuterWallLength(){
		return SimpleBlock.WALL_LENGTH;
	}
	
	public final int getWestEdge(){
		return gridCoordinate.getLeft() * SimpleBlock.WALL_LENGTH;
	}
	
	public final int getEastEdge(){
		return (gridCoordinate.getLeft() + 1) * SimpleBlock.WALL_LENGTH;
	}
	
	public final int getNorthEdge(){
		return gridCoordinate.getRight() * SimpleBlock.WALL_LENGTH;
	}

	public final int getSouthEdge(){
		return (gridCoordinate.getRight() + 1) * SimpleBlock.WALL_LENGTH;
	}
	
	public PointXY getCenter(){
		return new PointXY((getWestEdge() + getEastEdge()) / 2,
				(getNorthEdge() + getSouthEdge()) / 2);
	}


}
