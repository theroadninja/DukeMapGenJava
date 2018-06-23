package trn.duke.experiments;

import java.io.IOException;
import java.util.HashMap;
import java.util.Random;

import org.apache.commons.lang3.tuple.Pair;

import trn.Main;
import trn.Map;
import trn.PlayerStart;
import trn.Sector;
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
	
	private static final Random random = new Random();


	static LegacyGrid.SimpleTileset Block0 = new LegacyGrid.SimpleTileset(E5CreateMaze.MAZE_WALL_TEX, 0, 0);
	
	static LegacyGrid.SimpleTileset Block1 = new LegacyGrid.SimpleTileset(781, 782, 781);
	
	static LegacyGrid.SimpleTileset Block2 = new LegacyGrid.SimpleTileset(800, 801, 800);
	
	static LegacyGrid.SimpleTileset BLOCKS[] = new LegacyGrid.SimpleTileset[]{ Block0, Block1, Block2 };
	
	
	public static void main(String[] args) throws IOException{
		
		//System.out.println(DfsMazeGen.createGridMaze(5, 5));
		
		
		int width = 9;
		int height = 9;
		
		
		final int oneLevelDown = 24576;
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
		
		
		
		
		
		

		
		trn.Map map = E5CreateMaze.createMap(grid, width, height);
		
		Main.writeResult(map);
		
	}


	
}
