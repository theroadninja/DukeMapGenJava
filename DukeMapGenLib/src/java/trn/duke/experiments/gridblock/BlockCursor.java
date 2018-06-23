package trn.duke.experiments.gridblock;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * A sort of turtle-like object that makes it easier to select positions for blocks.
 * @author Dave
 *
 */
public class BlockCursor {
	
	int x = 0;
	int y = 0;
	
	public BlockCursor(Pair<Integer, Integer> coord){
		this(coord.getLeft(), coord.getRight());
	}
	public BlockCursor(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public Pair<Integer, Integer> get(){
		return new ImmutablePair<Integer, Integer>(x,y);
	}
	
	/**
	 * Moves the turtle in direction of decreasing y and returns the NEW position
	 * @return the position after moving, with y -= 1
	 */
	public Pair<Integer, Integer> moveNorth(){
		this.y -= 1;
		return get();
	}
	
	public Pair<Integer, Integer> moveEast(){
		this.x += 1;
		return get();
	}
	
	//TODO:  add peekNorth(), etc

}
