package trn.maze;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * using same coordinate system as Build map format.
 * y+ is south, x+ is east
 * @author Dave
 *
 */
public enum Heading {
	NORTH(0, -1,    0),
	EAST(1, 0,      1),
	SOUTH(0, 1,     2),
	WEST(-1, 0,     3);
	
	private final int dx;
	private final int dy;
	
	/** so you can use Headings as array indexes instead of instantiating an entire hashmap */
	public final int arrayIndex;
	
	private Heading(int dx, int dy, int arrayIndex){
		this.dx = dx;
		this.dy = dy;
		this.arrayIndex = arrayIndex;
	}
	
	public Pair<Integer, Integer> move(Pair<Integer, Integer> node){
		return new ImmutablePair<Integer, Integer>(node.getLeft() + dx, node.getRight() + dy);
	}
	

}