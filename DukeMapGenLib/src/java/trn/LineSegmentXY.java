package trn;

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


    /**
     * @return the vector from p1 to p2
     */
    public PointXY getVector(){
        return p2.subtractedBy(p1);
    }

    /**
     * Tests line segment intersection.  Note: returns true of overlapping and same lines
     * @param line2 the other line
     * @return true if the lines intersect.
     */
    public boolean intersects(LineSegmentXY line2){
        return PointXY.segmentsIntersect(this.p1, this.getVector(), line2.p1, line2.getVector());
    }



}