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
	
	/* is this useful?
	public Pair<Integer, Integer> toPair(){
		return new ImmutablePair<Integer, Integer>(x,y);
	}*/
	
}
