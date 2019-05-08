package trn;

/**
 * For XYZ coordinates.
 * 
 * @author Dave
 *
 */
public class PointXYZ {
	public static PointXYZ ZERO = new PointXYZ(0, 0, 0);

	public final int x;
	public final int y;
	public final int z;
	
	public PointXYZ(int x, int y, int z){
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	public PointXYZ(PointXY xy, int z){
		this.x = xy.x;
		this.y = xy.y;
		this.z = z;
	}

	public PointXYZ add(PointXYZ p){
		return new PointXYZ(x + p.x, y + p.y, z + p.z);
	}

	public PointXY asPointXY(){
		return new PointXY(this.x, this.y);
	}

	public PointXY asXY(){
		return asPointXY();
	}

	/**
	 * @returns a copy of this object, but with z set to the given value.
	 */
	public PointXYZ withZ(int z){
		return new PointXYZ(this.x, this.y, z);
	}
	
	public PointXYZ getTransformTo(PointXYZ dest){
		return new PointXYZ(dest.x - this.x, dest.y - this.y, dest.z - this.z);
	}

	@Override
	public String toString(){
		return "{ PointXY x=" + this.x + " y=" + y + " z=" + z + " }";
	}

}
