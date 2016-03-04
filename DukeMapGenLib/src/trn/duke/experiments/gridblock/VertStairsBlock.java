package trn.duke.experiments.gridblock;

import org.apache.commons.lang3.tuple.Pair;

import trn.Map;
import trn.Sector;
import trn.Wall;
import trn.maze.Heading;

public class VertStairsBlock extends AbstractBlock implements Block {

	private final NorthSouthConnector northConnector;
	private final NorthSouthConnector southConnector;

	
	private int southZ = Sector.DEFAULT_FLOOR_Z;
	private int northZ = Sector.DEFAULT_FLOOR_Z;

	public VertStairsBlock(Pair<Integer, Integer> gridCoordinate, int southZ, int northZ) {
		super(gridCoordinate);
		
		northConnector = NorthSouthConnector.northEdge(this);
		southConnector = NorthSouthConnector.southEdge(this);
		
		this.southZ = southZ;
		this.northZ = northZ;
	}

	@Override
	public Connector getConnector(Heading heading) {
		
		if(Heading.NORTH == heading){
			return northConnector;
		}else if(Heading.SOUTH == heading){
			return southConnector;
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
		
		
		int ydelta = north - south;
		//int[] y = new int[]{ south, south + ydelta / 3, south + ydelta * 2 / 3,  north };
		//int stepCount = y.length -1;
		
		
		int stepCount = 4;
		int[] y = new int[stepCount + 1];
		y[y.length-1] = north;
		
		for(int i = 0; i < stepCount; ++i){
			y[i] = south + ydelta/stepCount * i;
		}
		
		
		int[] z = stepInterpolate(southZ, northZ, stepCount);
		
		int[] sectorIndexes = new int[stepCount];
		
		
		for(int i = 0; i < stepCount; ++i){
			
			
			//TODO:  reduce the repeat ...
			
			sectorIndexes[i] = map.createSectorFromLoop(
					new Wall(west, y[i], wallTex, 16 / stepCount, 8),
					new Wall(west, y[i+1], wallTex, 16 / stepCount, 8),
					new Wall(east, y[i+1], wallTex, 16 / stepCount, 8),
					new Wall(east, y[i], wallTex, 16 / stepCount, 8));
			
			
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
		
		
		this.southConnector.setSectorIndex(sectorIndexes[0]);
		this.northConnector.setSectorIndex(sectorIndexes[sectorIndexes.length-1]);
		
		return sectorIndexes[0];
		
		
		
	}
	
	public static int[] stepInterpolate(int start, int end, int stepCount){
		
		//z axis is negative going up!!!!
		
		int delta = (end - start) / (stepCount - 1);
		
		int[] results = new int[stepCount];
		
		for(int i = 0; i < stepCount; ++i){
			results[i] = start + delta * i;
		}
		
		return results;
	}



}
