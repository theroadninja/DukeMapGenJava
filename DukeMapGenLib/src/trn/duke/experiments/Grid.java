package trn.duke.experiments;

import java.util.HashMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import trn.Sector;
import trn.maze.DfsMazeGen;

/**
 * Simplified data structure used to create a map based on square rooms in a grid.
 * 
 * 
 * @author Dave
 *
 */
public class Grid {
	
	
	/**
	 * one texture for all the walls, one for ceiling, one for floor.
	 * @author Dave
	 *
	 */
	public static class SimpleTileset {
		final int wallTexture;
		final int floorTexture;
		final int ceilingTexture;
		
		public SimpleTileset(int wall, int floor, int ceil){
			this.wallTexture = wall;
			this.floorTexture = floor;
			this.ceilingTexture = ceil;
		}
		
		public void applyToCeilAndFloor(Sector s){
			s.setCeilingTexture(ceilingTexture);
			s.setFloorTexture(floorTexture);
		}
		
		@Override
		public String toString(){
			return String.format("{ wall: %d, floor: %d, ceiling: %d", wallTexture, floorTexture, ceilingTexture);
		}
		
	}

	public static class BlockInfo {
		
		//integer that identifies a 
		SimpleTileset tileset;
		
		public BlockInfo(){
			
		}
		
		public BlockInfo(SimpleTileset s){
			this.tileset = s;
		}
		
		public void setTileset(SimpleTileset s){
			this.tileset = s;
		}
		
		public SimpleTileset getTileset(){
			return this.tileset;
		}
	}
	
	
	private final java.util.Map<Pair<Integer, Integer>, BlockInfo> gridData = new HashMap<Pair<Integer, Integer>, BlockInfo>();
	
	public Grid(){
		
	}
	
	public Grid(DfsMazeGen.Graph<Pair<Integer, Integer>> maze){
		this.copyFromGraph(maze);
	}
	
	/**
	 * adds a node with blank BlockInfo (for use with experiment(s) that dont bother with block info)
	 * @param node
	 */
	public void add(Pair<Integer, Integer> node){
		gridData.put(node, new BlockInfo());
	}
	
	public void put(Pair<Integer, Integer> node, BlockInfo info){
		gridData.put(node, info);
	}
	
	public BlockInfo getBlockInfo(Pair<Integer, Integer> node){
		return gridData.get(node);
	}
	
	public java.util.Set<Pair<Integer, Integer>> getNodes(){
		return gridData.keySet();
	}
	
	public boolean contains(Pair<Integer, Integer> node){
		return gridData.containsKey(node);
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
	public void copyFromGraph(DfsMazeGen.Graph<Pair<Integer, Integer>> maze){
	
		
		
		for(Pair<Integer, Integer> node : maze.getAdjacencyList().keySet()){
			
			
			DfsMazeGen.NodeInfo ni = maze.getNodeInfo(node);
			if(ni == null || ni.tileset == null){
				add(toGridNode(node));
			}else{
				put(toGridNode(node), new BlockInfo(maze.getNodeInfo(node).tileset));
			}
			
			
			for(Pair<Integer, Integer> n2 : maze.getAdjacencyList().get(node)){
				add(toGridEdge(node, n2));
			}
			
		}
		
		
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
