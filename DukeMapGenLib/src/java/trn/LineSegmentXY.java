package trn;

import javax.sound.sampled.Line;
import java.util.ArrayList;
import java.util.List;

public class LineSegmentXY {

    private final PointXY p1;
    private final PointXY p2;

    public LineSegmentXY(PointXY p1, PointXY p2){
        if(p1 == null || p2 == null || p1.equals(p2)) throw new IllegalArgumentException();
        this.p1 = p1;
        this.p2 = p2;
    }

    @Override
    public boolean equals(Object other){
        if(other == this){
            return true;
        }
        if(!(other instanceof LineSegmentXY)){
            return false;
        }

        LineSegmentXY line2 = (LineSegmentXY)other;
        return this.p1.equals(line2.p1) && this.p2.equals(line2.p2);
    }

    public final List<PointXY> toList(){
        ArrayList<PointXY> list = new ArrayList<>(2);
        list.add(getP1());
        list.add(getP2());
        return list;
    }

    public PointXY getP1(){
        return this.p1;
    }
    public PointXY getP2(){
        return this.p2;
    }

    public LineSegmentXY reversed(){
        return new LineSegmentXY(p2, p1);
    }


    /**
     * @return the vector from p1 to p2
     */
    public PointXY getVector(){
        return p2.subtractedBy(p1);
    }

    public double getLength(){
        return p1.distanceTo(p2);
    }

    public long getManhattanLength(){
        return p1.manhattanDistanceTo(p2);
    }

    /**
     * Tests line segment intersection.  Note: returns true of overlapping and same lines
     * @param line2 the other line
     * @return true if the lines intersect.
     */
    public boolean intersects(LineSegmentXY line2){
        return PointXY.segmentsIntersect(this.p1, this.getVector(), line2.p1, line2.getVector());
    }

    /**
     * Tests intersect between a ray (or half line, a line starting from a point and going infinitely in one direction)
     * and a segment.
     *
     * @param rayPoint starting point of the ray
     * @param rayVector a vector (unit or not) indicating the direction of the ray
     * @return
     */
    public boolean intersectsRay(PointXY rayPoint, PointXY rayVector, boolean endingExclusive){
        return PointXY.raySegmentIntersect(rayPoint, rayVector, this.p1, this.getVector(), endingExclusive);
    }

    public boolean isParallel(PointXY vector){
        return PointXY.vectorsParallel(getVector(), vector);
    }

    @Override
    public String toString(){
        return this.p1.toString() + "--" + this.p2.toString();
    }


}
