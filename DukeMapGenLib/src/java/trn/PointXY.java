package trn;

/**
 * For xy coordinates.
 *
 * Represents a point in 2D space, but also used to represent 2D vectors (which some would argue are the same thing).
 * 
 * @author Dave
 *
 */
public class PointXY {
	public static PointXY ZERO = new PointXY(0, 0);

	public final int x;
	public final int y;

	public PointXY(int x, int y){
		this.x = x;
		this.y = y;
	}

	public static PointXY fromDouble(double x, double y){
		return new PointXY((int)Math.round(x), (int)Math.round(y));
	}

	public PointXY(Wall w){
		this.x = w.getX();
		this.y = w.getY();
	}

	public PointXYZ withZ(int z){
		return new PointXYZ(this.x, this.y, z);
	}

	// indexed access, so that matrix functions make more sense
    // NOTE: if we were doing this in scala could just have `def apply(Int)` for index syntax
	public int get(int i){
		if(i == 0){
			return x;
		}else if(i == 1){
			return y;
		}else{
			throw new IndexOutOfBoundsException();
		}
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


	public long manhattanDistanceTo(PointXY dest) {
		long dx = Math.abs(dest.x - this.x);
		long dy = Math.abs(dest.y - this.y);
		return dx  + dy;
	}

	public double distanceTo(PointXY dest){
		double dx = (dest.x - this.x);
		double dy = (dest.y - this.y);
		return Math.sqrt(dx*dx + dy*dy);
	}

	public PointXY translateTo(PointXY dest){
		return new PointXY(dest.x - this.x, dest.y - this.y);
	}

	public PointXY add(PointXY p){
		return new PointXY(x + p.x, y + p.y);
	}

	/**
	 * subtract THIS by the given point.
	 */
	public PointXY subtractedBy(PointXY other){
		return new PointXY(this.x - other.x, this.y - other.y);
	}

	public PointXY subtractedBy(PointXYZ other){
		return subtractedBy(other.asXY());
	}

	public PointXY multipliedBy(int f){
		return new PointXY(x * f, y * f);
	}

	/**
	 * Treating this object as a vector,
	 * @return its magnitude
	 */
	public double vectorMagnitude(){
	    return Math.sqrt(x * x + y * y);
	}

	/**
	 * Implementation of the "2d" special case of cross product.
	 * The result is supposed to be an integer that is positive or negative, representing whether the new vector
	 * is coming out of or going into the screen.
     *
	 * For vectors A and B:
	 *   A x B = A.x * B.y - A.y * B.x
	 *   A x B = - B x A
	 *
	 * See also isSpritePointedAtWall
	 * @param other
	 * @return
	 */
	public int crossProduct2d(PointXY other){
	    return this.x * other.y - this.y * other.x;
	}

	public int dotProduct(PointXY other){
		return this.x * other.x + this.y * other.y;
	}

	/**
	 * Tests intersection between segments (a, a+b) and (c, c+d).
	 * WARNING:  returns false for perfect overlap
	 *
	 * Based on:
	 *   a + tb = c + ud
	 *
     * @param a segment 1, point 1
	 * @param b the vector between segment 1, point 1 and segment 1, point 2 (i.e. NOT point 2)
	 * @param c segment 2, point 1
	 * @param d the vector between segment 2, point 1 and segment 2, point 2 (i.e. NOT point 2)
	 * @return true IFF the segments intersect -- BUT it returns false if the segments overlap!
	 */
	public static boolean segmentsIntersect(PointXY a, PointXY b, PointXY c, PointXY d) {
		return intersectInclusive(a, b, c, d, false, false);
	}

	/**
	 * TODO - how should we handle two identical line segments?
	 *
	 * Tests intersect between a ray (or half line, a line starting from a point and going infinitely in one direction)
	 * and a segment.
     *
	 * @param rayPoint starting point of the ray
	 * @param rayVector a vector (unit or not) indicating the direction of the ray
	 * @param c point 1 of the segment
	 * @param d the vector between point 1 and point 2 of the segment (i.e. NOT point 2)
	 * @return
	 */
	public static boolean raySegmentIntersect(PointXY rayPoint, PointXY rayVector, PointXY c, PointXY d, boolean endingExclusive){
		return intersect(rayPoint, rayVector, c, d, true, false, endingExclusive);
	}
	public static boolean raySegmentIntersect(PointXY rayPoint, PointXY rayVector, PointXY c, PointXY d){
	    return raySegmentIntersect(rayPoint, rayVector, c, d, false);
	}

	public static boolean rayCircleIntersect(PointXY rayPoint, PointXY rayVector, PointXY circleCenter, int circleRadius){
		// a^2 + b^2 = c^2, where
		// a = projection of circleVector onto rayVector
		// b = shortest line from rayVector to circleCenter
		// c = circleVector
		PointXY circleVector = circleCenter.subtractedBy(rayPoint);
		int d = rayVector.dotProduct(circleVector);
		if(d < 0){
			return false;
		}else if(d == 0){
			// they are already at 90 degree angles
			return circleVector.vectorMagnitude() <= circleRadius;
		}else{
			// the dot project is the length of the projection X magnitude of rayVector
			double a = ((double)d)/rayVector.vectorMagnitude();
			double c = circleVector.vectorMagnitude();
			double b = Math.sqrt(c * c - a * a);
			return b <= (double)circleRadius;
		}
	}

	public static boolean vectorsParallel(PointXY v1, PointXY v2){
		return v1.crossProduct2d(v2) == 0;
	}

	/**
	 * being on either endpoint counts as being on the line
	 */
	static boolean intersectInclusive(PointXY a, PointXY b, PointXY c, PointXY d, boolean isRay1, boolean isRay2) {
		int bd = b.crossProduct2d(d);
		if(0 == bd) return false;
		// Explanation:
		// a + tb = c + ud
		// solve for t:
		// (a + tb) x d = (c + ud) x d      // because d x d = 0 to eliminate u
		// t = (c x d - a x d) / (b x d)
		// solve for u:
		// (a + tb) x b = (c + ud) x b      // because b x b = 0 to eliminate t
		// u = (a x b - c x b) / (d x b)
        // so, the unoptimized version is:
		//double t = c.subtractedBy(a).crossProduct2d(d) / (double)bd;
		//double u = (a.crossProduct2d(b) - c.crossProduct2d(b)) / (double)d.crossProduct2d(b);
		PointXY ca = c.subtractedBy(a);
		double t = ca.crossProduct2d(d) / (double)bd;
		double u = ca.crossProduct2d(b) / (double)bd; // -bxd = dxb
		// return (0.0 <= t && t <= 1.0) && (0.0 <= u && u <= 1.0); // simple, line segments only
        return (0.0 <= t && (isRay1 || t <= 1.0))
				&& (0.0 <= u && (isRay2 || u <= 1.0));
	}

	/**
	 * TODO - DRY
	 */
	static boolean intersect(PointXY a, PointXY b, PointXY c, PointXY d, boolean isRay1, boolean isRay2, boolean endingExclusive) {
		int bd = b.crossProduct2d(d);
		if(0 == bd) return false;
		PointXY ca = c.subtractedBy(a);
		double t = ca.crossProduct2d(d) / (double)bd;
		double u = ca.crossProduct2d(b) / (double)bd; // -bxd = dxb
        if(endingExclusive){
            // if the intersection happens at the beginning of the segment, in counts, but not at the other end
			// used for polygon math so that we don't double count when crossing vertexes
			return (0.0 <= t && (isRay1 || t < 1.0)) // <-- this is whats different
					&& (0.0 <= u && (isRay2 || u < 1.0));
		}else{
			return (0.0 <= t && (isRay1 || t <= 1.0))
					&& (0.0 <= u && (isRay2 || u <= 1.0));
		}

	}

	/**
	 * TODO - DRY
	 */
	public static boolean intersectSementsForPoly(PointXY a, PointXY b, PointXY c, PointXY d, boolean isRay1, boolean isRay2){
		int bd = b.crossProduct2d(d);
		if(0 == bd) return false;
		PointXY ca = c.subtractedBy(a);
		double t = ca.crossProduct2d(d) / (double)bd;
		double u = ca.crossProduct2d(b) / (double)bd; // -bxd = dxb
		// if the intersection happens at the beginning of the segment, in counts, but not at the other end
		// used for polygon math so that we don't double count when crossing vertexes
		return (0.0 < t && (isRay1 || t < 1.0)) // <-- this is whats different
				&& (0.0 < u && (isRay2 || u < 1.0));

	}

	public static PointXY midpoint(PointXY p0, PointXY p1){
		return new PointXY((p0.x + p1.x)/2, (p0.y + p1.y)/2);
	}
}
