package trn.duke.experiments.stonetunnels;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import trn.Map;
import trn.MapUtil;
import trn.PlayerStart;
import trn.PointXY;
import trn.Sector;
import trn.Wall;
import trn.WallPrefab;
import trn.duke.experiments.gridblock.AbstractBlock;
import trn.duke.experiments.gridblock.Block;
import trn.duke.experiments.gridblock.Connector;
import trn.duke.experiments.gridblock.NorthSouthConnector;
import trn.maze.Heading;

/**
 * Start block that is meant to contain player start.
 * 
 * Has only one connector, but that can be at any edge.
 * 
 * Actually for now it only faces north.
 * 
 * TODO: allow it to be configured for any direction
 * 
 * @author Dave
 *
 */
public class StartBlock extends AbstractBlock implements Block {

	
	private Connector connector;
	
	private final Heading connectorEdge = Heading.NORTH;
	
	private final int floorZ = StoneConstants.UPPER_FLOORZ;
	
	public StartBlock(Pair<Integer, Integer> gridCoordinate
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
		
		//this room is a square with a misshapen dodecagon (12-sided circle)
		//in the middle, down which light should shine.
		
		
		int south = getSouthEdge();
		int west = getWestEdge();
		int north = getNorthEdge();
		int east = getEastEdge();
		
		
		//and remember, positive y goes down/south
		
		int mediumUnit = getOuterWallLength() / 8; //grid size #3, 256 in original drawing
		int smallUnit = getOuterWallLength() / 32; //grid size...64 in original drawing
		
		//outer bounding box for dodecagon
		int dodec_south = south - mediumUnit;
		int dodec_north = north + mediumUnit;
		int dodec_east = east - mediumUnit;
		int dodec_west = west + mediumUnit;
		
		//distance from outer edge to the corner point
		int cornerPointDist = 2 * mediumUnit - smallUnit;
		
		
		PointXY[] box = new PointXY[]{
			new PointXY(west, south),
			new PointXY(west, north),
			new PointXY(east, north),
			new PointXY(east, south)
		};
		
		//the dodecagon has a flat edge (two points) as N,E,S,W, and two edges (one point)
		//between those.
		//so lets call the flat edges at N/E/S/W the ordinal edges.
		//we are starting with the left/west point of the south ordinal edge, and going around
		//counter clockwise because I believe walls have to go in a certain direction
		
		PointXY[] circle = new PointXY[]{
				
				//south edge
				new PointXY(west + 3 * mediumUnit, dodec_south),
				new PointXY(east - 3 * mediumUnit, dodec_south),
				
				//SE point
				new PointXY(east - cornerPointDist, south - cornerPointDist),
				
				//east edge
				new PointXY(dodec_east, south - 3 * mediumUnit),
				new PointXY(dodec_east, north + 3 * mediumUnit),
				
				//NE point
				new PointXY(east - cornerPointDist, north + cornerPointDist),
				
				//north edge
				new PointXY(east - 3 * mediumUnit, dodec_north),
				new PointXY(west + 3 * mediumUnit, dodec_north),
				
				//NW point
				new PointXY(west + cornerPointDist, north + cornerPointDist),
				
				//west edge
				new PointXY(dodec_west, north + 3 * mediumUnit),
				new PointXY(dodec_west, south - 3 * mediumUnit),
				
				//SW point
				new PointXY(west + cornerPointDist, south - cornerPointDist),
		};
		
		
		//
		//first, try to create the outer sector
		//  NOTE:  the multiple loops must be next to each other, due to build's format
		
		WallPrefab wall = new WallPrefab(StoneConstants.UPPER_WALL).setXRepeat(16).setYRepeat(8);
		
		//map.createSectorFromLoop(wallsToAdd)
		int outerLoopCircle = map.addLoop(Wall.createLoop(circle, wall));
		int outerBox = map.addLoop(Wall.createLoop(box, wall));
		int outerSectorIndex = map.addSector(new Sector(outerLoopCircle, /*wall count*/circle.length + box.length));
		
		
		//
		// inner sector
		//
		ArrayUtils.reverse(circle);//now its the inner sector circle
		int innerLoop = map.addLoop(Wall.createLoop(circle, wall));
		int innerSectorIndex = map.addSector(new Sector(innerLoop, circle.length));
		
		
		MapUtil.linkAllWalls(map, outerSectorIndex, outerLoopCircle, innerSectorIndex, innerLoop);
		
		
		
		//Wall[] wallLoop = Wall.createWallLoop(outerSectorCircle, StoneConstants.UPPER_WALL, 16, 8);
		
		/* this worked 
		ArrayUtils.reverse(outerSectorCircle);//want the inner one to test
		Wall[] testLoop = Wall.createWallLoop(outerSectorCircle, StoneConstants.UPPER_WALL, 16, 8);
		
		int sectorIndex =  map.createSectorFromLoop(testLoop);
		
		Sector s = map.getSector(sectorIndex);
		
		s.setFloorTexture(StoneConstants.UPPER_FLOOR);
		s.setCeilingTexture(StoneConstants.UPPER_CEILING);
		s.setFloorZ(floorZ);
		*/
		
		
		
		
		
		
		
		
		return -1;
	}

}
