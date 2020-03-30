package trn.duke.experiments.stonetunnels;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import trn.Map;
import trn.MapUtil;
import trn.PlayerStart;
import trn.PointXY;
import trn.Sector;
import trn.Wall;
import trn.duke.experiments.WallPrefab;
import trn.duke.experiments.gridblock.AbstractBlock;
import trn.duke.experiments.gridblock.Block;
import trn.duke.experiments.gridblock.Connector;
import trn.duke.experiments.gridblock.NorthSouthConnector;
import trn.maze.Heading;

/**
 * Having red wall issues with start block, so I'm trying a simpler version.
 * 
 * 
 * @author Dave
 *
 */
public class StartBlockSimple extends AbstractBlock implements Block {

	private Connector connector;
	
	private final Heading connectorEdge = Heading.NORTH;
	
	private final int floorZ = StoneConstants.UPPER_FLOORZ;
	
	public StartBlockSimple(Pair<Integer, Integer> gridCoordinate
			//TODO: , Heading connectorEdge
			) {
		super(gridCoordinate);
		
		
		this.connector = NorthSouthConnector.northEdge(this);
	}

	@Override
	public Connector getConnector(Heading heading) {
		return heading == connectorEdge ? connector : null;
	}
	
	public PlayerStart getPlayerStart(){
		
		//return player start in the center, facing the room's exit
		
		int x = (getWestEdge() + getEastEdge()) / 2;
		int y = (getSouthEdge() + getNorthEdge()) / 2;
		
		return new PlayerStart(x, y, floorZ, connectorEdge.getDukeAngle());
	}

	@Override
	public int draw(Map map) {

		int south = getSouthEdge();
		int west = getWestEdge();
		int north = getNorthEdge();
		int east = getEastEdge();
		
		
		
		
		
		PointXY[] box = new PointXY[]{
				new PointXY(west, south),
				new PointXY(west, north),
				new PointXY(east, north),
				new PointXY(east, south)
			};
		
		int delta = 256;
		
		PointXY[] innerSmallerBox = new PointXY[]{
				new PointXY(west + delta, south - delta),
				new PointXY(west + delta, north + delta),
				new PointXY(east - delta, north + delta),
				new PointXY(east - delta, south - delta)
		};
		
		PointXY[] outerSmallerBox = ArrayUtils.clone(innerSmallerBox);
		ArrayUtils.reverse(outerSmallerBox);
		
		//so..this showed that the wall direction really matters: ArrayUtils.reverse(box);
		
		WallPrefab wallspec = new WallPrefab(StoneConstants.UPPER_WALL_TEX).setXRepeat(16).setYRepeat(8);
		
		int largerBoxFirstWall = map.addLoop(Wall.createLoop(box, wallspec));
		int smallerBoxFirstWall = map.addLoop(Wall.createLoop(outerSmallerBox, wallspec));
		
		int outerSectorIndex = map.addSector(new Sector(largerBoxFirstWall, 8));
		
		map.getSector(outerSectorIndex).setFloorTexture(StoneConstants.UPPER_FLOOR);
		
		//
		// inner sector
		//
		
		int innerSectorSmallerBoxFirstWall = map.addLoop(Wall.createLoop(innerSmallerBox, wallspec));
		int innerSectorIndex = map.addSector(new Sector(innerSectorSmallerBoxFirstWall, 4));
		
		MapUtil.linkAllWalls(map, outerSectorIndex, smallerBoxFirstWall, innerSectorIndex, innerSectorSmallerBoxFirstWall);
		
		
		return -1;
	}
}
