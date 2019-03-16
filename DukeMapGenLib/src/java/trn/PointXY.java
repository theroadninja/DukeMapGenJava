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
	public boolean equals(Object other){
		if(other == this){
			return true;
		}
		if(!(other instanceof PointXY)){
			return false;
		}

		PointXY p2 = (PointXY)other;
		return x == p2.x && y == p2.y;
	}
	
	@Override
	public String toString(){
		return "{ PointXY x=" + this.x + " y=" + y + " }";
	}
	
	/* is this useful?
	public Pair<Integer, Integer> toPair(){
		return new ImmutablePair<Integer, Integer>(x,y);
	}*/

	public PointXY translateTo(PointXY dest){
		return new PointXY(dest.x - this.x, dest.y - this.y);
	}
}
