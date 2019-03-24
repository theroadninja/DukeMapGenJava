package trn;

/**
 * For XYZ coordinates.
 * 
 * @author Dave
 *
 */
public class PointXYZ {
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

	public PointXY asPointXY(){
		return new PointXY(this.x, this.y);
	}
	
	public PointXYZ getTransformTo(PointXYZ dest){
		return new PointXYZ(dest.x - this.x, dest.y - this.y, dest.z - this.z);
	}
}
