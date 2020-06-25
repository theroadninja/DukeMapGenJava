package trn;

import org.apache.commons.lang3.tuple.Pair;

/** creating this to house static intersection functions that started in PointXY */
public class IntersectXY {

    /**
     * This intersects  point0 --> point0+vector0 with point1 --> point1+vector1
     *
     * This just calculates the T and U parameters for an intersection -- its up to the caller to decide what that
     * means, based on whether these vectors represent segments, rays or lines.
     * @param point0
     * @param vector0
     * @param point1
     * @param vector1
     * @return (t, u) where t is the factor multiplied against vector0 to hit the intersection, and u is the factor
     *              multiplied against vector1 to hit the intersection.   Returns null if the vectors are parellel.
     */
    public static Pair<Double, Double> intersect(PointXY point0, PointXY vector0, PointXY point1, PointXY vector1){
        PointXY a = point0;
        PointXY b = vector0;
        PointXY c = point1;
        PointXY d = vector1;
        int bd = b.crossProduct2d(d);
        if(0 == bd) return null;
        PointXY ca = c.subtractedBy(a);

        // t is the factor multiplied against a+b
        double t = ca.crossProduct2d(d) / (double)bd;

        // u is the factor multiplied against c+d
        double u = ca.crossProduct2d(b) / (double)bd; // -bxd = dxb
        return Pair.of(t, u);
    }

    public static Pair<Double, Double> lineSegmentIntersect(LineXY line, LineSegmentXY segment){
        Pair<Double, Double> tu = intersect(line.getPoint(), line.getVector(), segment.getP1(), segment.getVector());
        if(tu == null){
            return null;
        }else{
            // double t = tu.getLeft(); // dont care what T is because the left object is a line
            double u = tu.getRight();
            if(0.0 <= u && u <= 1.0){
                return tu;
            }else{
                return null;
            }
        }

    }
}
