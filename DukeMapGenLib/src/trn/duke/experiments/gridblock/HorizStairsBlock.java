package trn.duke.experiments.gridblock;

import org.apache.commons.lang3.tuple.Pair;

import trn.Map;
import trn.Sector;
import trn.Wall;
import trn.maze.Heading;

public class HorizStairsBlock extends AbstractBlock implements Block {

	private final EastWestConnector eastConnector;
	private final EastWestConnector westConnector;

	
	private int eastZ = Sector.DEFAULT_FLOOR_Z;
	private int westZ = Sector.DEFAULT_FLOOR_Z;
	
	public HorizStairsBlock(Pair<Integer, Integer> gridCoordinate, int westZ, int eastZ) {
		super(gridCoordinate);
		this.eastZ = eastZ;
		this.westZ = westZ;
		
		this.eastConnector = EastWestConnector.eastEdge(this);
		this.westConnector = EastWestConnector.westEdge(this);
		
		
	}

	@Override
	public Connector getConnector(Heading heading) {
		if(Heading.EAST == heading){
			return eastConnector;
		}else if(Heading.WEST == heading){
			return westConnector;
		}else{
			return null;
		}
	}

	@Override
	public int draw(Map map) {
		
		final int wallTex = 786, floorTex = 0, ceilTex = 0;
		
		
		int west = gridCoordinate.getLeft() * SimpleBlock.WALL_LENGTH;
		int east = (gridCoordinate.getLeft() + 1) * SimpleBlock.WALL_LENGTH;
		int north = gridCoordinate.getRight() * SimpleBlock.WALL_LENGTH;
		int south = (gridCoordinate.getRight() + 1) * SimpleBlock.WALL_LENGTH;
		
		
		int xdelta = east - west;
		//int[] y = new int[]{ south, south + ydelta / 3, south + ydelta * 2 / 3,  north };
		//int stepCount = y.length -1;
		
		
		int stepCount = 5;
		int[] x = new int[stepCount + 1];
		x[x.length-1] = east;
		
		for(int i = 0; i < stepCount; ++i){
			x[i] = west + xdelta/stepCount * i;
		}
		
		
		int[] z = VertStairsBlock.stepInterpolate(westZ, eastZ, stepCount);
		
		int[] sectorIndexes = new int[stepCount];
		
		
		for(int i = 0; i < stepCount; ++i){
			
			
			
			sectorIndexes[i] = map.createSectorFromLoop(
					new Wall(x[i], south, wallTex, 16 / stepCount, 8),
					new Wall(x[i], north, wallTex, 16 / stepCount, 8),
					new Wall(x[i+1], north, wallTex, 16 / stepCount, 8),
					new Wall(x[i+1], south, wallTex, 16 / stepCount, 8));
			
			
			{
				Sector s = map.getSector(sectorIndexes[i]);
				
				s.setFloorTexture(floorTex);
				s.setCeilingTexture(ceilTex);
				
				s.setFloorZ(z[i]);
			}
			
			if(i > 0){
				GridUtils.linkSectors(map, sectorIndexes[i], sectorIndexes[i-1]);
			}
		}
		
		
		this.westConnector.setSectorIndex(sectorIndexes[0]);
		this.eastConnector.setSectorIndex(sectorIndexes[sectorIndexes.length-1]);
		
		return sectorIndexes[0];
		
		
		
	}

}
