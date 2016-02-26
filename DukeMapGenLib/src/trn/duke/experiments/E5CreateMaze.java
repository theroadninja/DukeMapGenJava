package trn.duke.experiments;

import java.io.IOException;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import trn.Main;
import trn.Map;
import trn.PlayerStart;
import trn.Wall;
import trn.duke.Util;
import trn.maze.DfsMazeGen;

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
		
		Set<Pair<Integer, Integer>> grid = toGrid(DfsMazeGen.createGridMaze(width, height));

		/* simple ass grid for testing
		Set<Pair<Integer, Integer>> grid = new HashSet<Pair<Integer, Integer>>();
		grid.add(new ImmutablePair<Integer, Integer>(0, 0));
		grid.add(new ImmutablePair<Integer, Integer>(0, 1));
		 */
		
		trn.Map map = createMap(grid, width, height);
		
		Main.writeResult(map);
		
	}
	
	
	
	
	public static trn.Map createMap(Set<Pair<Integer, Integer>> grid, int width, int height){
		
		Map map = Map.createNew();
		
		map.setPlayerStart(new PlayerStart(512, 512, 0, PlayerStart.SOUTH));
		
		//map of grid to sector, so we can find the sector by its grid index
		java.util.Map<Pair<Integer, Integer>, Integer> gridToSector = new HashMap<Pair<Integer, Integer>, Integer>();
		
		//create the sectors
		for(Pair<Integer, Integer> p : grid){
			//System.out.println(p);
			
			int sectorIndex = createSector(map, p);
			
			gridToSector.put(p, sectorIndex);
		}
		
		//link the sectors
		for(Pair<Integer, Integer> p : grid){
			
			Pair<Integer, Integer> east = DfsMazeGen.Heading.EAST.move(p);
			Pair<Integer, Integer> south = DfsMazeGen.Heading.SOUTH.move(p);
			
			if(grid.contains(east)){
				linkSectors(map, gridToSector.get(p), gridToSector.get(east));
			}
			
			if(grid.contains(south)){
				linkSectors(map, gridToSector.get(p), gridToSector.get(south));
			}
			
		}
		
		return map;
	}
	
	
	/**
	 * maybe this could be expanded to a general purpose glue utility
	 * 
	 * @param map
	 * @param sector0
	 * @param sector1
	 */
	public static void linkSectors(trn.Map map, int sector0, int sector1){

		Pair<List<Integer>, List<Integer>> overlappingWalls = Util.filterOverlappingPoints(map, sector0, sector1);
		
		if(overlappingWalls.getLeft().size() != 2) throw new RuntimeException();
		if(overlappingWalls.getRight().size() != 2) throw new RuntimeException();
		
		Util.orderWalls(map, overlappingWalls.getLeft());
		Util.orderWalls(map, overlappingWalls.getRight());
		
		/*
		System.out.println("-----wall set 1");
		for(int i : overlappingWalls.getLeft()){
			System.out.println("i=" + i);
			System.out.println(map.getWall(i));
		}
		System.out.println("------wall set 2");
		for(int i : overlappingWalls.getRight()){
			System.out.println("i=" + i);
			System.out.println(map.getWall(i));
		}*/
		
		
		int w0 = overlappingWalls.getLeft().get(0);
		int w1 = overlappingWalls.getRight().get(0);
		
		//System.out.println(map.getWall(w0));
		//System.out.println(map.getWall(w1));
		
		//System.out.println(String.format("map.linkRedWalls(%d, %d, %d, %d)", sector0, w0, sector1, w1));
		map.linkRedWalls(sector0, w0, sector1, w1);
	
	}
	
	
	
	
	public static int createSector(Map map, Pair<Integer, Integer> gc){
		
		int west = gc.getLeft() * WALL_LENGTH;
		int east = (gc.getLeft() + 1) * WALL_LENGTH;
		int north = gc.getRight() * WALL_LENGTH;
		int south = (gc.getRight() + 1) * WALL_LENGTH;
		
		Wall nw = new Wall(west, north, MAZE_WALL_TEX, 16, 8); //first wall; also matches the grid coordinate
		Wall ne = new Wall(east, north, MAZE_WALL_TEX, 16, 8);
		Wall se = new Wall(east, south, MAZE_WALL_TEX, 16, 8);
		Wall sw = new Wall(west, south, MAZE_WALL_TEX, 16, 8);
		
		return map.createSectorFromLoop(nw, ne, se, sw);
	}
	
	
	
	/**
	 * turns the maze, specified by an adjacency list, into a grid implementation, where each edge
	 * between nodes gets its own square in the grid.
	 * 
	 * e.g.    (0,0)--edge-->(0,1)
	 * 
	 * becomes:
	 * 
	 *     (0,0), (0,1), (0,2)
	 * 
	 * Where 0,0 and 0,2 represent the nodes in the maze.  So we need to multiple node indices by 2 to
	 * be able to add the edges between them.
	 * 
	 * @param maze
	 * @return
	 */
	public static Set<Pair<Integer, Integer>> toGrid(DfsMazeGen.Graph<Pair<Integer, Integer>> maze){
		
		Set<Pair<Integer, Integer>> grid = new HashSet<Pair<Integer, Integer>>();
		
		for(Pair<Integer, Integer> node : maze.getAdjacencyList().keySet()){
			
			grid.add(toGridNode(node));
			
			for(Pair<Integer, Integer> n2 : maze.getAdjacencyList().get(node)){
				grid.add(toGridEdge(node, n2));
			}
			
		}
		
		return grid;
		
	}
	
	
	static Pair<Integer, Integer> toGridNode(Pair<Integer, Integer> node){
		return new ImmutablePair<Integer, Integer>(
				node.getLeft() * 2, node.getRight() * 2);
	}
	
	static Pair<Integer, Integer> toGridEdge(Pair<Integer, Integer> node, Pair<Integer, Integer> node2){
		//super lazy way of doing it
		return new ImmutablePair<Integer, Integer>(
				(node.getLeft() + node2.getLeft()),
				(node.getRight() + node2.getRight()));
	}

	
}
