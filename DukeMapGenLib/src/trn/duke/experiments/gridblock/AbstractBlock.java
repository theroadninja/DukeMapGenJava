package trn.duke.experiments.gridblock;

import org.apache.commons.lang3.tuple.Pair;

public abstract class AbstractBlock implements Block {

	protected final Pair<Integer, Integer> gridCoordinate;
	
	public AbstractBlock(Pair<Integer, Integer> gridCoordinate){
		this.gridCoordinate = gridCoordinate;
	}
	
	@Override
	public final Pair<Integer, Integer> getGridCoordinate() {
		return this.gridCoordinate;
	}



}
