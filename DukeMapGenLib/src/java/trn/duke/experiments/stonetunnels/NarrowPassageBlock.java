package trn.duke.experiments.stonetunnels;

import org.apache.commons.lang3.tuple.Pair;

import trn.Sector;
import trn.Wall;
import trn.duke.experiments.WallPrefab;
import trn.duke.experiments.gridblock.AbstractBlock;
import trn.duke.experiments.gridblock.Connector;
import trn.duke.experiments.gridblock.EastWestConnector;
import trn.duke.experiments.gridblock.NorthSouthConnector;
import trn.duke.experiments.gridblock.OrdinalConnector;
import trn.duke.experiments.gridblock.SimpleBlock;
import trn.maze.Heading;

public class NarrowPassageBlock extends AbstractBlock {
	
	private final int rotation;
	
	protected final OrdinalConnector[] connectors = new OrdinalConnector[]{null, null, null, null};

	public NarrowPassageBlock(Pair<Integer, Integer> gridCoordinate, int rotation) {
		super(gridCoordinate);
		this.rotation = rotation;
		
		if(BlockRotation.VERTICAL == (rotation & BlockRotation.VERTICAL)){
			
			connectors[Heading.NORTH.arrayIndex] = NorthSouthConnector.northEdge(this);
			connectors[Heading.SOUTH.arrayIndex] = NorthSouthConnector.southEdge(this);
		}else{
			connectors[Heading.EAST.arrayIndex] = EastWestConnector.eastEdge(this);
			connectors[Heading.WEST.arrayIndex] = EastWestConnector.westEdge(this);
		}
	}

	@Override
	public Connector getConnector(Heading heading) {
		return connectors[heading.arrayIndex];
	}

	/**
	 * see also E5CreateMaze.createSector()
	 * @param map
	 */
	public int draw(trn.Map map){
		
		int WALL_LENGTH = SimpleBlock.WALL_LENGTH;
		
		int west = gridCoordinate.getLeft() * WALL_LENGTH;
		int east = (gridCoordinate.getLeft() + 1) * WALL_LENGTH;
		int north = gridCoordinate.getRight() * WALL_LENGTH;
		int south = (gridCoordinate.getRight() + 1) * WALL_LENGTH;
		
		
		int wallTex = StoneConstants.UPPER_WALL_TEX;
		
		int vMiddle = north+(south-north)/2;
		int bowedDist = 512; //amount middle points are drawn in
		
		Wall nw = new Wall(west, north, wallTex, 16, 8); //first wall; also matches the grid coordinate
		Wall ne = new Wall(east, north, wallTex, 16, 8);
		Wall ne2 = new Wall(east - bowedDist, vMiddle-512, wallTex, 8, 8);
		Wall ne3 = new Wall(east - bowedDist, vMiddle+512, wallTex, 16, 8);
		Wall se = new Wall(east, south, wallTex, 16, 8);
		Wall sw = new Wall(west, south, wallTex, 16, 8);
		Wall sw2 = new Wall(west + bowedDist, vMiddle+512, wallTex, 8, 8);
		Wall sw3 = new Wall(west + bowedDist, vMiddle-512, wallTex, 16, 8);
		
		
		WallPrefab wallPrefab = StoneConstants.UPPER_WALL;
		wallPrefab.writeTo(nw);
		wallPrefab.writeTo(ne);
		wallPrefab.writeTo(ne2, ne3);
		wallPrefab.writeTo(se);
		wallPrefab.writeTo(sw);
		wallPrefab.writeTo(sw2, sw3);
		
		
		//int sectorIndex =  map.createSectorFromLoop(nw, ne, se, sw);
		int sectorIndex =  map.createSectorFromLoop(nw, ne, ne2, ne3, se, sw, sw2, sw3);
		
		
		Sector s = map.getSector(sectorIndex);
		StoneConstants.UPPER_SECTOR.writeTo(s);
		

		int createdSectorIndex = sectorIndex;
		
		for(OrdinalConnector c : connectors){
			if(c != null){
				c.setSectorIndex(createdSectorIndex);
			}
		}
		
		return sectorIndex;
		
		
	}
	
	

}
