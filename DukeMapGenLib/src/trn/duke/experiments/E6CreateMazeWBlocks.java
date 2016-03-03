package trn.duke.experiments;

import java.io.IOException;
import java.util.Random;
import java.util.Set;

import org.apache.commons.lang3.tuple.Pair;

import trn.Main;
import trn.maze.DfsMazeGen;

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

	//block 0 - default shit from last experiment.
	
	/*
	 * block 1
	 * 
	 * walls - 781
	 * floor - 782
	 * ceiling - 781
	 * 
	 */
	
	/*
	 * block 2
	 * 
	 * walls - 800
	 * floor - 801
	 * ceiling - 800
	 */
	

	
	static Grid.SimpleTileset Block0 = new Grid.SimpleTileset(E5CreateMaze.MAZE_WALL_TEX, 0, 0);
	
	static Grid.SimpleTileset Block1 = new Grid.SimpleTileset(781, 782, 781);
	
	static Grid.SimpleTileset Block2 = new Grid.SimpleTileset(800, 801, 800);
	
	static Grid.SimpleTileset BLOCKS[] = new Grid.SimpleTileset[]{ Block0, Block1, Block2 };
	
	
	public static void main(String[] args) throws IOException{
		
		//System.out.println(DfsMazeGen.createGridMaze(5, 5));
		
		
		int width = 10;
		int height = 10;
		
		
		//create a graph that represents a maze
		DfsMazeGen.Graph<Pair<Integer,Integer>> graph = DfsMazeGen.createGridMaze(width, height);
		
		
		//assign random integers to represent tilesets/blocks
		for(Pair<Integer, Integer> node : graph.getAdjacencyList().keySet()){
			graph.getNodeInfo(node).tileset = BLOCKS[random.nextInt(BLOCKS.length)];
		}
		
		
		Grid grid = new Grid(graph);

		
		trn.Map map = E5CreateMaze.createMap(grid, width, height);
		
		Main.writeResult(map);
		
	}

	
}
