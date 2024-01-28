package trn.duke.experiments.gridblock;

import java.util.HashMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import trn.maze.Heading;

public class Grid {

	private final java.util.Map<Pair<Integer, Integer>, Block> gridData = new HashMap<Pair<Integer, Integer>, Block>();
	
	public Grid(){ }
	
	// public Grid(DfsMazeGen.Graph<Pair<Integer, Integer>> maze){
	// 	copyFromGraphAndExpand(maze);
	// }
	
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

//	/**
//	 * turns the maze, specified by an adjacency list, into a grid implementation, where each edge
//	 * between nodes gets its own square in the grid.
//	 *
//	 * e.g.    (0,0)--edge-->(0,1)
//	 *
//	 * becomes:
//	 *
//	 *     (0,0), (0,1), (0,2)
//	 *
//	 * Where 0,0 and 0,2 represent the nodes in the maze.  So we need to multiple node indices by 2 to
//	 * be able to add the edges between them.
//	 *
//	 * SEE ALSO:  LegacyGrid
//	 *
//	 * @param maze
//	 * @return
//	 */
//	public void copyFromGraphAndExpand(DfsMazeGen.Graph<Pair<Integer, Integer>> maze){
//
//		for(Pair<Integer, Integer> node : maze.getAdjacencyList().keySet()){
//
//
//			BlockInfo ni = maze.getBlockInfo(node);
//			if(ni == null){
//				throw new RuntimeException("not supported");
//			}else{
//
//				SimpleBlock sb = new SimpleBlock(toGridNode(node));
//				sb.setWallTex(ni.getTileset().wallTexture);
//				sb.setFloorTex(ni.getTileset().floorTexture);
//				sb.setCeilTex(ni.getTileset().ceilingTexture);
//				sb.setFloorZ(ni.floorZ);
//				add(sb);
//			}
//
//			//TODO:  we are adding these edges multiple times ...
//			for(Pair<Integer, Integer> n2 : maze.getAdjacencyList().get(node)){
//
//				Pair<Integer, Integer> edgeBlockCoordinate = toGridEdge(node, n2);
//
//				if(isVerticalPassage(edgeBlockCoordinate)){
//
//					Block north = get(Heading.NORTH.move(edgeBlockCoordinate));
//					Block south = get(Heading.SOUTH.move(edgeBlockCoordinate));
//					int southZ = ((OrdinalConnector)south.getConnector(Heading.NORTH)).getFloorZ();
//					int northZ = ((OrdinalConnector)north.getConnector(Heading.SOUTH)).getFloorZ();
//
//					if(validForVerticalStairs(edgeBlockCoordinate)){
//						add(new VertStairsBlock(edgeBlockCoordinate, southZ, northZ));
//					}else{
//						//at least make sure the floor matches
//						VertDoorBlock sb = new VertDoorBlock(edgeBlockCoordinate);
//						sb.setFloorZ(southZ);
//						add(sb);
//					}
//				}else if(isHorizontalPassage(edgeBlockCoordinate)){
//
//					Block east = get(Heading.EAST.move(edgeBlockCoordinate));
//					Block west = get(Heading.WEST.move(edgeBlockCoordinate));
//					int westZ = ((OrdinalConnector)west.getConnector(Heading.EAST)).getFloorZ();
//					int eastZ = ((OrdinalConnector)east.getConnector(Heading.WEST)).getFloorZ();
//
//					if(westZ != eastZ){
//
//						add(new HorizStairsBlock(edgeBlockCoordinate, westZ, eastZ));
//
//					}else{
//
//						//for now, match the floor.  later, do horizontal stairs
//						SimpleBlock sb = new SimpleBlock(edgeBlockCoordinate);
//						sb.setFloorZ(westZ);
//						add(sb);
//					}
//				}else{
//					//TODO:  might still need to adjust the z coordinate, if we can
//					add(new SimpleBlock(edgeBlockCoordinate));
//				}
//			}
//		}
//	}

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
