package trn.maze;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.TreeSet;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import trn.duke.experiments.LegacyGrid;

/**
 * TODO finish converting to duchy.experiments.render.maze.MutableMazeGraph
 */
public class DfsMazeGen {
	private static final Random random = new Random();

	/** adjacency list */
	public static class Graph<T> {
		Map<T, Set<T>> adjacencyList = new HashMap<T, Set<T>>();
		
		/**
		 * just tacking this on to get a little more mileage out of this
		 * graph class before I'm forced to write something better.
		 * 
		 * @author Dave
		 *
		 */
		Map<T, LegacyGrid.BlockInfo> nodeInfo = new HashMap<T, LegacyGrid.BlockInfo>();

		public Graph(){}

		public LegacyGrid.BlockInfo getBlockInfo(T t){
			LegacyGrid.BlockInfo ni = nodeInfo.get(t);
			if(ni == null){
				ni = new LegacyGrid.BlockInfo();
			}
			nodeInfo.put(t, ni);
			return ni;
		}

		public Set<T> getNodes() {
			return adjacencyList.keySet();
		}
		
		public Map<T, Set<T>> getAdjacencyList(){
			return this.adjacencyList;
		}
		
		Set<T> getListFor(T t){
			Set<T> list = adjacencyList.get(t);
			if(list == null){
				list = new TreeSet<T>();
				adjacencyList.put(t, list);
			}
			return list;
		}
		
		public void addEdge(T t1, T t2){
			getListFor(t1).add(t2);
			getListFor(t2).add(t1);
		}
		
		public boolean hasEdge(T t1, T t2){
			return getListFor(t1).contains(t2);
		}
		
		@Override
		public String toString(){
			final String ln = "\n";
			
			StringBuilder sb = new StringBuilder();
			sb.append("{").append(ln);
			for(T t : adjacencyList.keySet()){
				sb.append(t).append(": ").append("[");
				for(T t2 : adjacencyList.get(t)){
					sb.append(t2).append(", ");
				}
				sb.append("],").append(ln);
				
			}
			sb.append("}").append(ln);
			
			return sb.toString();
		}
		
	}
	
	/**
	 * For now, assumes 0,0 for start.
	 * 
	 * @param width
	 * @param height
	 * @return
	 */
	public static Graph<Pair<Integer, Integer>> createGridMaze(int width, int height){
		if(width < 1 || height < 1) throw new IllegalArgumentException();
		Graph<Pair<Integer, Integer>> maze = new Graph<Pair<Integer, Integer>>();
		createGridMaze(maze, width, height, new HashSet<Pair<Integer, Integer>>(), new ImmutablePair<Integer, Integer>(0, 0));
		return maze;
	}

	
	static boolean inBounds(int xmin, int ymin, int width, int height, Pair<Integer, Integer> node){
		int x = node.getLeft();
		int y = node.getRight();
		return xmin <= x && x < xmin + width
				&& ymin <= y && y < ymin + height;
	}
	
	private static void createGridMaze(Graph<Pair<Integer, Integer>> maze,
			int width, int height,
			Set<Pair<Integer, Integer>> visitedList, Pair<Integer, Integer> currentNode){
		
		if(visitedList.contains(currentNode)){
			return;
		}else{
			visitedList.add(currentNode);
		}
		
		//calc possible moves
		List<Pair<Integer, Integer>> list = new ArrayList<Pair<Integer, Integer>>();
		for(Heading h : Heading.values()){
			Pair<Integer, Integer> n = h.move(currentNode);
			if(inBounds(0, 0, width, height, n) && ! visitedList.contains(n)){
				list.add(n);
			}
		}
		
		//select one
		if(list.size() < 1){
			return; //dead end
		}
		
		final int BRANCH = 3; //not guaranted to actually branch 3 times
		for(int i = 0; i < BRANCH; ++i){
			Pair<Integer, Integer> nextNode = list.get( random.nextInt(list.size()) );
			
			//have to check visited list again
			if(! visitedList.contains(nextNode)){
				maze.addEdge(currentNode, nextNode);
				createGridMaze(maze, width, height, visitedList, nextNode);
			}
		}
	}
}
