package trn;

/**
 * Represents a line (not a line segment).
 */
public class LineXY {

    final PointXY point;
    final PointXY vector;

    public LineXY(PointXY point, PointXY vector){
        this.point = point;
        this.vector = vector;
    }

    public static LineXY fromPoints(PointXY p0, PointXY p1){
        if(p0 == p1){
            throw new IllegalArgumentException("invalid line: points cannot be the same");
        }
        return new LineXY(p0, p1.subtractedBy(p0));
    }

    public PointXY getPoint(){
        return this.point;
    }

    // vector is not normalized, because this works with integers
    public PointXY getVector(){
        return this.vector;
    }

    @Override
    public boolean equals(Object other){
        if(other == this){
            return true;
        }
        if(!(other instanceof LineXY)){
            return false;
        }
        LineXY line2 = (LineXY)other;
        return this.point.equals(line2.point) && this.vector.equals(line2.vector);
    }

    @Override
    public int hashCode(){
        return point.hashCode() << 4 + vector.hashCode();
    }

    @Override
    public String toString(){
        return "{ LineXY p=" + this.point + " vector=" + this.vector + " }";
    }

    /**
     * @return this a copy of this line, rotated 90 degrees clockwise (real clockwise, not build clockwise)
     */
    public LineXY rotatedCW(){
        // Formula for rotating a vector <x1, y1> ANTICLOCKWISE by angle A to a new vector <x2, y2>
        //
        // x2 = cos(A) * x1 - sin(A) * y1
        // y2 = sin(A) * x1 + cos(A) * y1
        //
        // Sin(90) = 1, Cos(90) = 0
        //
        // So rotating by 90 degrees anticlockwise becomes:
        // int x2 = -vector.y;
        // int y2 = vector.x;
        // by -90 degrees:
        int x2 = vector.y;
        int y2 = -vector.x;
        return new LineXY(point, new PointXY(x2, y2));
    }
}
