package trn.duke.experiments;

import java.io.IOException;
import java.util.HashMap;

import org.apache.commons.lang3.tuple.Pair;

import trn.Main;
import trn.Map;
import trn.PlayerStart;
import trn.Sector;
import trn.duke.experiments.gridblock.Block;
import trn.duke.experiments.gridblock.Grid;
import trn.maze.DfsMazeGen;
import trn.maze.Heading;

public class E7BetterBlocks {

	public static final int MAZE_WALL_TEX = 772;
	static LegacyGrid.SimpleTileset Block0 = new LegacyGrid.SimpleTileset(MAZE_WALL_TEX, 0, 0);
	
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
		
		//1. create a graph that represents a maze
		//node types not define here -- see Grid() constructor
		DfsMazeGen.Graph<Pair<Integer,Integer>> graph = DfsMazeGen.createGridMaze(width, height);
		
		
		//2. assign random integers to represent tilesets/blocks
		for(Pair<Integer, Integer> node : graph.getAdjacencyList().keySet()){
			LegacyGrid.BlockInfo bi = graph.getBlockInfo(node);
			bi.tileset = BLOCKS[ExperimentUtil.getRandom().nextInt(BLOCKS.length)];
			bi.floorZ = Integer.valueOf(floorz[ExperimentUtil.getRandom().nextInt(floorz.length)]);
		}

		//3. block construction is here
		//node types selected here; think they are the 'connector' blocks
		Grid grid = new Grid(graph);
		trn.Map map = createMap(grid);
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
		return block.draw(map);
	}
}
