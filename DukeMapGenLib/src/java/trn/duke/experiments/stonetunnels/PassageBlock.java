package trn.duke.experiments.stonetunnels;

import org.apache.commons.lang3.tuple.Pair;

import trn.SectorPrefab;
import trn.WallPrefab;
import trn.duke.experiments.gridblock.SimpleBlock;

public class PassageBlock extends SimpleBlock {

	public PassageBlock(Pair<Integer, Integer> gridCoordinate) {
		super(gridCoordinate);
		
		setWallPrefab(new WallPrefab(StoneConstants.UPPER_WALL_TEX).setShade(StoneConstants.SHADE));
		setSectorPrefab(new SectorPrefab(StoneConstants.UPPER_FLOOR, StoneConstants.UPPER_CEILING)
				.setFloorShade(StoneConstants.SHADE).setCeilingShade(StoneConstants.SHADE));
	}

}
