package trn.duke.experiments.gridblock;

import java.util.HashMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import trn.maze.Heading;

public class Grid {

	private final java.util.Map<Pair<Integer, Integer>, Block> gridData = new HashMap<Pair<Integer, Integer>, Block>();
	
	public Grid(){ }
	
	public void add(Block block){
		gridData.put(block.getGridCoordinate(), block);
	}
	
	public Block getBlock(Pair<Integer, Integer> node){
		return get(node);
	}
	
	public Block get(Pair<Integer, Integer> node){
		return gridData.get(node);
	}

	public java.util.Set<Pair<Integer, Integer>> getNodes(){
		return gridData.keySet();
	}
	
	public boolean contains(Pair<Integer, Integer> node){
		return gridData.containsKey(node);
	}

	public boolean isHorizontalPassage(Pair<Integer, Integer> gc){
		
		Block north = get(Heading.NORTH.move(gc));
		Block east = get(Heading.EAST.move(gc));
		Block west = get(Heading.WEST.move(gc));
		Block south = get(Heading.SOUTH.move(gc));
		
		return north == null
				&& south == null
				&& east != null
				&& west != null;
	}
	
	
	public boolean isVerticalPassage(Pair<Integer, Integer> gc){
		
		Block north = get(Heading.NORTH.move(gc));
		Block east = get(Heading.EAST.move(gc));
		Block west = get(Heading.WEST.move(gc));
		Block south = get(Heading.SOUTH.move(gc));
		
		return north != null
				&& south != null
				&& east == null
				&& west == null;
	}
	
	public boolean validForVerticalStairs(Pair<Integer, Integer> gc){
		
		Block north = get(Heading.NORTH.move(gc));
		Block south = get(Heading.SOUTH.move(gc));
		
		boolean roomsMatch = isVerticalPassage(gc);
		
		if(roomsMatch){
			int z1 = (north.getConnector(Heading.SOUTH)).getFloorZ();
			int z2 = (south.getConnector(Heading.NORTH)).getFloorZ();
			return z1 != z2;
		}else{
			return false;
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
