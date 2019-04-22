package trn.duke.experiments;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.lang3.tuple.Pair;

import trn.Main;
import trn.Map;
import trn.PlayerStart;
import trn.Wall;
import trn.duke.experiments.gridblock.GridUtils;
import trn.maze.DfsMazeGen;
import trn.maze.Heading;

/**
 * Like E4 but creates a full maze.
 * 
 * 
 * @author Dave
 *
 */
public class E5CreateMaze {
	
	public static final int MAZE_WALL_TEX = 772;
	public static final int WALL_LENGTH = 2048; //1024 seems just big enough to fit duke comfortably

	public static void main(String[] args) throws IOException{
		
		//System.out.println(DfsMazeGen.createGridMaze(5, 5));
		
		
		int width = 10;
		int height = 10;
		
		
		LegacyGrid grid = new LegacyGrid(DfsMazeGen.createGridMaze(width, height));

		/* simple ass grid for testing
		Set<Pair<Integer, Integer>> grid = new HashSet<Pair<Integer, Integer>>();
		grid.add(new ImmutablePair<Integer, Integer>(0, 0));
		grid.add(new ImmutablePair<Integer, Integer>(0, 1));
		 */
		
		trn.Map map = createMap(grid, width, height);
		
		//Main.writeResult(map);
		Main.deployTest(map);
		
	}
	
	
	
	
	public static trn.Map createMap(LegacyGrid grid, int width, int height){
		
		Map map = Map.createNew();
		
		map.setPlayerStart(new PlayerStart(512, 512, 0, PlayerStart.SOUTH));
		
		//map of grid to sector, so we can find the sector by its grid index
		java.util.Map<Pair<Integer, Integer>, Integer> gridToSector = new HashMap<Pair<Integer, Integer>, Integer>();
		
		//create the sectors
		for(Pair<Integer, Integer> p : grid.getNodes()){
			//System.out.println(p);
			
			int sectorIndex = createSector(map, p, grid.getBlockInfo(p));
			
			gridToSector.put(p, sectorIndex);
		}
		
		//link the sectors
		for(Pair<Integer, Integer> p : grid.getNodes()){
			
			Pair<Integer, Integer> east = Heading.EAST.move(p);
			Pair<Integer, Integer> south = Heading.SOUTH.move(p);
			
			if(grid.contains(east)){
				GridUtils.linkSectors(map, gridToSector.get(p), gridToSector.get(east));
			}
			
			if(grid.contains(south)){
				GridUtils.linkSectors(map, gridToSector.get(p), gridToSector.get(south));
			}
			
		}
		
		return map;
	}
	
	

	
	
	
	
	public static int createSector(Map map, Pair<Integer, Integer> gc, LegacyGrid.BlockInfo blockInfo){
		
		int wallTex = MAZE_WALL_TEX;
		if(blockInfo != null && blockInfo.tileset != null){
			wallTex = blockInfo.tileset.wallTexture;
		}
		
		
		int west = gc.getLeft() * WALL_LENGTH;
		int east = (gc.getLeft() + 1) * WALL_LENGTH;
		int north = gc.getRight() * WALL_LENGTH;
		int south = (gc.getRight() + 1) * WALL_LENGTH;
		
		Wall nw = new Wall(west, north, wallTex, 16, 8); //first wall; also matches the grid coordinate
		Wall ne = new Wall(east, north, wallTex, 16, 8);
		Wall se = new Wall(east, south, wallTex, 16, 8);
		Wall sw = new Wall(west, south, wallTex, 16, 8);
		
		int sectorIndex =  map.createSectorFromLoop(nw, ne, se, sw);
		
		if(blockInfo != null){
			
			if(blockInfo.tileset != null){
				blockInfo.tileset.applyToCeilAndFloor(map.getSector(sectorIndex));
				//System.out.println("applying tileset: " + blockInfo.tileset);
			}
			
			if(blockInfo.floorZ != null){
				map.getSector(sectorIndex).setFloorZ(blockInfo.floorZ);
			}
			
			
			
		}
		
		return sectorIndex;
	}
	
	
	


	
}
