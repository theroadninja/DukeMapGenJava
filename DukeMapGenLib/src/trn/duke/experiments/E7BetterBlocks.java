package trn.duke.experiments;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.lang3.tuple.Pair;

import trn.Main;
import trn.Map;
import trn.PlayerStart;
import trn.Sector;
import trn.duke.Util;
import trn.duke.experiments.gridblock.Block;
import trn.duke.experiments.gridblock.Grid;
import trn.duke.experiments.gridblock.GridUtils;
import trn.maze.DfsMazeGen;
import trn.maze.Heading;

public class E7BetterBlocks {
	
	
	
	/**
	 * 
	 * TODO:  instead of tilesets, we need an array of block prefabs, which create blocks.
	 */

	static LegacyGrid.SimpleTileset Block0 = new LegacyGrid.SimpleTileset(E5CreateMaze.MAZE_WALL_TEX, 0, 0);
	
	static LegacyGrid.SimpleTileset Block1 = new LegacyGrid.SimpleTileset(781, 782, 781);
	
	static LegacyGrid.SimpleTileset Block2 = new LegacyGrid.SimpleTileset(800, 801, 800);
	
	static LegacyGrid.SimpleTileset BLOCKS[] = new LegacyGrid.SimpleTileset[]{ Block0, Block1, Block2 };
	
	
	public static void main(String[] args) throws IOException{
		
		//System.out.println(DfsMazeGen.createGridMaze(5, 5));
		
		
		int width = 9;
		int height = 9;
		
		
		final int oneLevelDown = 24576;
		int[] floorz = new int[]{Sector.DEFAULT_FLOOR_Z, 
				//Sector.DEFAULT_FLOOR_Z, 
				oneLevelDown};
		
		//create a graph that represents a maze
		DfsMazeGen.Graph<Pair<Integer,Integer>> graph = DfsMazeGen.createGridMaze(width, height);
		
		
		//assign random integers to represent tilesets/blocks
		for(Pair<Integer, Integer> node : graph.getAdjacencyList().keySet()){
			LegacyGrid.BlockInfo bi = graph.getBlockInfo(node);
			bi.tileset = BLOCKS[Util.getRandom().nextInt(BLOCKS.length)];
			bi.floorZ = Integer.valueOf(floorz[Util.getRandom().nextInt(floorz.length)]);
		}
		
		
		//block construction is here
		Grid grid = new Grid(graph);
		
		//LegacyGrid grid = new LegacyGrid(graph);
		
		
		
		
		
		

		
		trn.Map map = createMap(grid);
		
		//Main.writeResult(map);
		Main.deployTest(map);
		
	}
	
	public static trn.Map createMap(Grid grid 
			){
		
		Map map = Map.createNew();
		
		map.setPlayerStart(new PlayerStart(512, 512, 0, PlayerStart.SOUTH));
		
		//map of grid to sector, so we can find the sector by its grid index
		java.util.Map<Pair<Integer, Integer>, Integer> gridToSector = new HashMap<Pair<Integer, Integer>, Integer>();
		
		//create the sectors
		for(Pair<Integer, Integer> p : grid.getNodes()){
			//System.out.println(p);
			
			int sectorIndex = createSector(map, p, grid.getBlock(p));
			
			gridToSector.put(p, sectorIndex);
		}
		
		//link the sectors
		for(Pair<Integer, Integer> p : grid.getNodes()){
			
			Pair<Integer, Integer> east = Heading.EAST.move(p);
			Pair<Integer, Integer> south = Heading.SOUTH.move(p);
			
			if(grid.contains(east)){
				
				grid.getBlock(p).getConnector(Heading.EAST).draw(map,  grid.getBlock(east));
				
			}
			
			if(grid.contains(south)){
				
				grid.getBlock(p).getConnector(Heading.SOUTH).draw(map, grid.getBlock(south));
				
			}
			
		}
		
		return map;
	}
	
	
	public static int createSector(Map map, Pair<Integer, Integer> gc, Block block){
		
		/*
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
			
			
			
		}*/
		
		int sectorIndex = block.draw(map);
		
		return sectorIndex;
	}
	
	
	
}
