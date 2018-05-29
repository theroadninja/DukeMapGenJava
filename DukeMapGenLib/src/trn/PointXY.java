package trn;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

/**
 * for xy coordinates
 * 
 * @author Dave
 *
 */
public class PointXY {

	public final int x;
	public final int y;
	
	public PointXY(int x, int y){
		this.x = x;
		this.y = y;
	}
	
	public PointXY(Wall w){
		this.x = w.getX();
		this.y = w.getY();
	}
	
	@Override
	public String toString(){
		return "{ PointXY x=" + this.x + " y=" + y + " }";
	}
	
	/* is this useful?
	public Pair<Integer, Integer> toPair(){
		return new ImmutablePair<Integer, Integer>(x,y);
	}*/
	
}
