package trn.maze;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * TODO this is deprecated
 *
 * using same coordinate system as Build map format.
 * y+ is south, x+ is east
 * @author Dave
 *
 */
public enum Heading {
	NORTH(0, -1,    0, true, 1536),
	EAST(1, 0,      1, false, 0),
	SOUTH(0, 1,     2, true, 512),
	WEST(-1, 0,     3, false, 1024);
	
	private final int dx;
	private final int dy;
	
	/** so you can use Headings as array indexes instead of instantiating an entire hashmap */
	public final int arrayIndex;
	
	/** N,S are vertical, E,W are not */
	private final boolean vertical;
	
	/** the angle, in whatever units duke uses, which faces this heading */
	private final int dukeAngle;
	
	private Heading(int dx, int dy, int arrayIndex, boolean vertical, int dukeAngle){
		this.dx = dx;
		this.dy = dy;
		this.arrayIndex = arrayIndex;
		this.vertical = vertical;
		this.dukeAngle = dukeAngle;
	}
	
	public Pair<Integer, Integer> move(Pair<Integer, Integer> node){
		return new ImmutablePair<Integer, Integer>(node.getLeft() + dx, node.getRight() + dy);
	}
	
	/**
	 * 
	 * 
	 * @return true for North or South, false for East or West
	 */
	public boolean isMapVertical(){
		return vertical;
	}
	
	/**
	 * @returns this heading in the units that duke uses for angles.
	 */
	public int getDukeAngle(){
		return dukeAngle;
	}
	

}