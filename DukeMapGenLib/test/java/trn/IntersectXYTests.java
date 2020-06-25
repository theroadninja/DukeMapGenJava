package trn;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;

public class IntersectXYTests {

    private static PointXY p(int x, int y){
        return new PointXY(x, y);
    }
    private static LineXY line(PointXY point, PointXY vector){
        return new LineXY(point, vector);
    }
    private static LineSegmentXY seg(PointXY p0, PointXY p1){
        return new LineSegmentXY(p0, p1);
    }

    @Test
    public void testIntersect(){
        Assert.assertNull(IntersectXY.intersect(p(0, 0), p(1, 0), p(500, 600), p(1, 0)));
        Assert.assertEquals(Pair.of(0.0, 0.0), IntersectXY.intersect(p(0, 0), p(1, 0), p(0, 0), p(0, 1)));
        Assert.assertEquals(Pair.of(10.0, 10.0), IntersectXY.intersect(p(10, 0), p(0, 1), p(0, 10), p(1, 0)));
        Assert.assertEquals(Pair.of(1.0, 1.0), IntersectXY.intersect(p(10, 0), p(0, 10), p(0, 10), p(10, 0)));
        Assert.assertEquals(Pair.of(0.5, 0.5), IntersectXY.intersect(p(10, 0), p(0, 20), p(0, 10), p(20, 0)));
    }

    @Test
    public void testLineSegmentIntersect(){
        Assert.assertNull(IntersectXY.lineSegmentIntersect(line(p(0, 0), p(1, 0)), seg(p(40, -23), p(50, -23))));
        Assert.assertNull(IntersectXY.lineSegmentIntersect(line(p(0, 0), p(1, 0)), seg(p(40, -23), p(-40, -23))));
        Assert.assertNull(IntersectXY.lineSegmentIntersect(line(p(0, 0), p(1, 0)), seg(p(40, 1000), p(-40, 1000))));
        Assert.assertNull(IntersectXY.lineSegmentIntersect(line(p(0, 0), p(-12, 0)), seg(p(40, 1000), p(-40, 1000))));

        // x aligned at y=32
        LineXY xaligned = line(p(16, 32), p(1, 0));
        Assert.assertEquals(1.0, IntersectXY.lineSegmentIntersect(xaligned, seg(p(0, 0), p(0, 32))).getRight(), 0.00001);
        Assert.assertEquals(1.0, IntersectXY.lineSegmentIntersect(xaligned, seg(p(0, 0), p(4096, 32))).getRight(), 0.00001);
        Assert.assertEquals(0.0, IntersectXY.lineSegmentIntersect(xaligned, seg(p(4096, 32), p(0, 0))).getRight(), 0.00001);
        Assert.assertEquals(0.5, IntersectXY.lineSegmentIntersect(xaligned, seg(p(0, 0), p(4096, 64))).getRight(), 0.00001);
        Assert.assertEquals(0.5, IntersectXY.lineSegmentIntersect(xaligned, seg(p(4096, 64), p(0, 0))).getRight(), 0.00001);

        // m = 1  (intersects y=-16 and x=16)
        LineXY m1 = line(p(-16, -32), p(-1, -1));
        Assert.assertEquals(0.0, IntersectXY.lineSegmentIntersect(m1, seg(p(-16, -32), p(0, 0))).getRight(), 0.00001);
        Assert.assertEquals(0.625, IntersectXY.lineSegmentIntersect(m1, seg(p(0, 64), p(64, 0))).getRight(), 0.00001); // intersects at 40, 24
        Assert.assertNull(IntersectXY.lineSegmentIntersect(m1, seg(p(0, 64), p(4, 60))));
    }
}
