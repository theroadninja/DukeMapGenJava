package trn.duke.experiments;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

import trn.*;
import trn.duke.experiments.gridblock.GridUtils;
import trn.maze.DfsMazeGen;
import trn.maze.Heading;

/**
 * Maze generation, but the nodes can be different "blocks" which
 * affect textures and sprite placements.
 * 
 * TODO:
 * 	make 2 or three floor levels; make the 'edges' into stairs, slopes
 * 	add sprites
 *  vary the room structure in some way
 * 
 * 
 * 
 * @author Dave
 *
 */
public class E6CreateMazeWBlocks {

	/** Height in z units of a single PGUP / PGDOWN action in the build editor -- see BuildConstants.scala */
	public static final int  Z_STEP_HEIGHT = 1024;

	public static final int MAZE_WALL_TEX = 772;
	public static final int WALL_LENGTH = 2048; //1024 seems just big enough to fit duke comfortably

	private static final Random random = new Random();

	static LegacyGrid.SimpleTileset Block0 = new LegacyGrid.SimpleTileset(MAZE_WALL_TEX, 0, 0);
	
	static LegacyGrid.SimpleTileset Block1 = new LegacyGrid.SimpleTileset(781, 782, 781);
	
	static LegacyGrid.SimpleTileset Block2 = new LegacyGrid.SimpleTileset(800, 801, 800);
	
	static LegacyGrid.SimpleTileset BLOCKS[] = new LegacyGrid.SimpleTileset[]{ Block0, Block1, Block2 };
	
	
	public static void main(String[] args) throws IOException{
		int width = 9;
		int height = 9;
		// final int oneLevelDown = 24576;
		final int oneLevelDown = Sector.DEFAULT_FLOOR_Z + Z_STEP_HEIGHT * 6;
		int[] floorz = new int[]{Sector.DEFAULT_FLOOR_Z, Sector.DEFAULT_FLOOR_Z, oneLevelDown};
		
		//create a graph that represents a maze
		DfsMazeGen.Graph<Pair<Integer,Integer>> graph = DfsMazeGen.createGridMaze(width, height);
		
		//assign random integers to represent tilesets/blocks
		for(Pair<Integer, Integer> node : graph.getAdjacencyList().keySet()){
			LegacyGrid.BlockInfo bi = graph.getBlockInfo(node);
			bi.tileset = BLOCKS[random.nextInt(BLOCKS.length)];
			bi.floorZ = Integer.valueOf(floorz[random.nextInt(floorz.length)]);
		}

		LegacyGrid grid = new LegacyGrid(graph);
		trn.Map map = createMap(grid, width, height);
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
			}
			if(blockInfo.floorZ != null){
				map.getSector(sectorIndex).setFloorZ(blockInfo.floorZ);
			}
		}
		return sectorIndex;
	}

}
