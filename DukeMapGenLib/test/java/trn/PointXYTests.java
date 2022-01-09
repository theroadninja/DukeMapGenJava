package trn;

import org.apache.commons.lang3.tuple.Pair;
import org.junit.Assert;
import org.junit.Test;
import trn.prefab.Heading;

public class PointXYTests {

    private final PointXY origin = new PointXY(0, 0);

    @Test
    public void testTranslate(){
        PointXY p = new PointXY(10, 20);
        Assert.assertEquals(new PointXY(-10, -20), p.translateTo(origin));
        Assert.assertNotEquals(new PointXY(-11, -20), p.translateTo(origin));
        Assert.assertNotEquals(new PointXY(-10, -19), p.translateTo(origin));

        Assert.assertEquals(new PointXY(2, 2), new PointXY(1,2).translateTo(new PointXY(3,4)));
    }

    @Test
    public void testEquals(){
        Assert.assertEquals(new PointXY(0, 2048), new PointXY(0, 2048));
    }

    private boolean equals(double a, double b){
        final double EPSILON = 0.00001;
        return Math.abs(a - b) < EPSILON;
    }
    @Test
    public void testDistance(){
        Assert.assertTrue(equals(10, p(0, 0).distanceTo(p(10, 0))));
        Assert.assertTrue(equals(10, p(0, 0).distanceTo(p(-10, 0))));
        Assert.assertTrue(equals(10, p(0, 0).distanceTo(p(0, 10))));
        Assert.assertTrue(equals(10, p(0, 0).distanceTo(p(0, -10))));
        Assert.assertFalse(equals(10, p(0, 0).distanceTo(p(9, 0))));
        Assert.assertFalse(equals(10, p(0, 0).distanceTo(p(-9, 0))));
        Assert.assertFalse(equals(10, p(0, 0).distanceTo(p(0, 9))));
        Assert.assertFalse(equals(10, p(0, 0).distanceTo(p(0, -9))));

        Assert.assertTrue(equals(5.0, p(0, 0).distanceTo(p(3, 4))));
        Assert.assertTrue(equals(5.0, p(0, 0).distanceTo(p(-3, 4))));
        Assert.assertTrue(equals(5.0, p(0, 0).distanceTo(p(3, -4))));
        Assert.assertTrue(equals(5.0, p(0, 0).distanceTo(p(4, 3))));
        Assert.assertTrue(equals(5.0, p(0, 0).distanceTo(p(-4, 3))));
        Assert.assertTrue(equals(5.0, p(0, 0).distanceTo(p(4, -3))));

        Assert.assertTrue(equals(5.0, p(-1, 0).distanceTo(p(3, -3))));
    }

    @Test
    public void testManhattanDistance(){
        Assert.assertEquals(10, new PointXY(0, 0).manhattanDistanceTo(new PointXY(10, 0)));
        Assert.assertEquals(10, new PointXY(0, 0).manhattanDistanceTo(new PointXY(-10, 0)));
        Assert.assertEquals(10, new PointXY(0, 0).manhattanDistanceTo(new PointXY(0, 10)));
        Assert.assertEquals(10, new PointXY(0, 0).manhattanDistanceTo(new PointXY(0, -10)));
        Assert.assertEquals(10, new PointXY(10, 0).manhattanDistanceTo(new PointXY(0, 0)));
        Assert.assertEquals(10, new PointXY(-10, 0).manhattanDistanceTo(new PointXY(0, 0)));
        Assert.assertEquals(10, new PointXY(0, 10).manhattanDistanceTo(new PointXY(0, 0)));
        Assert.assertEquals(10, new PointXY(0, -10).manhattanDistanceTo(new PointXY(0, 0)));

        Assert.assertEquals(10, new PointXY(0, 1).manhattanDistanceTo(new PointXY(9, 0)));
        Assert.assertEquals(10, new PointXY(2, 0).manhattanDistanceTo(new PointXY(0, 8)));
        Assert.assertEquals(10, new PointXY(0, -3).manhattanDistanceTo(new PointXY(7, 0)));
        Assert.assertEquals(10, new PointXY(0, 3).manhattanDistanceTo(new PointXY(-7, 0)));
        Assert.assertEquals(10, new PointXY(0, 0).manhattanDistanceTo(new PointXY(5, 5)));
        Assert.assertEquals(10, new PointXY(0, 0).manhattanDistanceTo(new PointXY(5, -5)));
        Assert.assertEquals(10, new PointXY(0, 0).manhattanDistanceTo(new PointXY(-5, 5)));
        Assert.assertEquals(10, new PointXY(0, 0).manhattanDistanceTo(new PointXY(-5, -5)));

        Assert.assertEquals(10, new PointXY(-1, -2).manhattanDistanceTo(new PointXY(-8, -5)));
        Assert.assertEquals(10, new PointXY(-1, -5).manhattanDistanceTo(new PointXY(-8, -2)));
        Assert.assertEquals(10, new PointXY(-8, -2).manhattanDistanceTo(new PointXY(-1, -5)));
        Assert.assertEquals(10, new PointXY(-8, -5).manhattanDistanceTo(new PointXY(-1, -2)));
    }

    @Test
    public void testCrossProduct2d(){
        // zero vector
        Assert.assertEquals(0, new PointXY(0, 0).crossProduct2d(new PointXY(0, 0)));
        Assert.assertEquals(0, new PointXY(1, 0).crossProduct2d(new PointXY(0, 0)));

        // same vector
        Assert.assertEquals(0, new PointXY(1, 0).crossProduct2d(new PointXY(1, 0)));
        Assert.assertEquals(0, new PointXY(-1, 0).crossProduct2d(new PointXY(-1, 0)));
        Assert.assertEquals(0, new PointXY(0, 5).crossProduct2d(new PointXY(0, 5)));
        Assert.assertEquals(0, new PointXY(4, -42).crossProduct2d(new PointXY(4, -42)));
        Assert.assertEquals(0, new PointXY(0, 5).crossProduct2d(new PointXY(0, 4)));
        Assert.assertEquals(0, new PointXY(1, 0).crossProduct2d(new PointXY(2, 0)));

        // opposite vector
        Assert.assertEquals(0, new PointXY(1, 0).crossProduct2d(new PointXY(-1, 0)));
        Assert.assertEquals(0, new PointXY(4, -42).crossProduct2d(new PointXY(-4, 42)));
        Assert.assertEquals(0, new PointXY(-1, 0).crossProduct2d(new PointXY(1, 0)));

        // not same vector
        Assert.assertNotEquals(0, new PointXY(1, 0).crossProduct2d(new PointXY(1, 1)));

        // A x B = - B x A
        Assert.assertEquals(-5, new PointXY(3, 2).crossProduct2d(new PointXY(4, 1)));
        Assert.assertEquals(5, new PointXY(4, 1).crossProduct2d(new PointXY(3, 2)));

        // distributive property:  (A - B) x C = A x C - B x C
        PointXY a = new PointXY(1,2);
        PointXY b = new PointXY(3,4);
        PointXY c = new PointXY(-5, 4);
        Assert.assertEquals(a.subtractedBy(b).crossProduct2d(c), a.crossProduct2d(c) - b.crossProduct2d(c));
    }

    // Test intersect with segments (p1 to p2) vs (p3 to p4)
    private boolean intersect(PointXY p1, PointXY p2, PointXY p3, PointXY p4){
        // it should not matter which way we define the points
        boolean b1 = PointXY.segmentsIntersect(p1, p2.subtractedBy(p1), p3, p4.subtractedBy(p3));
        boolean b2 = PointXY.segmentsIntersect(p2, p1.subtractedBy(p2), p3, p4.subtractedBy(p3));
        boolean b3 = PointXY.segmentsIntersect(p1, p2.subtractedBy(p1), p4, p3.subtractedBy(p4));
        boolean b4 = PointXY.segmentsIntersect(p2, p1.subtractedBy(p2), p4, p3.subtractedBy(p4));

        boolean b5 = PointXY.segmentsIntersect(p3, p4.subtractedBy(p3), p1, p2.subtractedBy(p1));
        boolean b6 = PointXY.segmentsIntersect(p4, p3.subtractedBy(p4), p1, p2.subtractedBy(p1));
        boolean b7 = PointXY.segmentsIntersect(p3, p4.subtractedBy(p3), p2, p1.subtractedBy(p2));
        boolean b8 = PointXY.segmentsIntersect(p4, p3.subtractedBy(p4), p2, p1.subtractedBy(p2));

        Assert.assertEquals(b1, b2);
        Assert.assertEquals(b2, b3);
        Assert.assertEquals(b3, b4);
        Assert.assertEquals(b4, b5);
        Assert.assertEquals(b5, b6);
        Assert.assertEquals(b6, b7);
        Assert.assertEquals(b7, b8);
        return b1;
    }

    @Test
    public void testSegmentsIntersect(){
        PointXY a = new PointXY(2, 2);
        PointXY a2 = new PointXY(2, 3);
        PointXY c = new PointXY(1, 4);
        PointXY c2 = new PointXY(3,4);
        Assert.assertFalse(PointXY.segmentsIntersect(a, a2.subtractedBy(a), c, c2.subtractedBy(c)));
        Assert.assertFalse(intersect(a, a2, c, c2));

        // vertical and horizontal
        Assert.assertTrue(intersect(a, new PointXY(2,4), c, c2));
        Assert.assertTrue(intersect(new PointXY(2, 6), new PointXY(2,4), c, c2));
        Assert.assertTrue(intersect(a, new PointXY(2,8), c, c2));
        Assert.assertTrue(intersect(new PointXY(2, 8), new PointXY(2,2), c, c2));
        Assert.assertTrue(intersect(a, new PointXY(2,60000), c, c2));
        Assert.assertTrue(intersect(new PointXY(2, 60000), new PointXY(2,2), c, c2));

        // cross
        Assert.assertTrue(intersect(new PointXY(-1, -1), new PointXY(1, 1), new PointXY(-1, 1), new PointXY(1, -1)));
        Assert.assertTrue(intersect(new PointXY(5, 5), new PointXY(6, 6), new PointXY(5, 6), new PointXY(6, 5)));
        Assert.assertTrue(intersect(new PointXY(5, 5), new PointXY(100000, 100020), new PointXY(5, 6), new PointXY(6, 5)));
        Assert.assertTrue(intersect(new PointXY(-10000, -1), new PointXY(10000, 1), new PointXY(-10000, 1), new PointXY(10000, -1)));

        // > or <
        Assert.assertTrue(intersect(new PointXY(-1, -2), new PointXY(1, 0), new PointXY(-1, 1), new PointXY(1, 0)));
        Assert.assertTrue(intersect(new PointXY(5, 4), new PointXY(6, 5), new PointXY(5, 6), new PointXY(6, 5)));
        Assert.assertTrue(intersect(new PointXY(-1, 1), new PointXY(1, 2), new PointXY(-1, 1), new PointXY(1, 0)));
        Assert.assertFalse(intersect(new PointXY(-1, 2), new PointXY(1, 3), new PointXY(-1, 1), new PointXY(1, 0)));
    }

    // just syntax sugar
    private PointXY p(int x, int y){
        return new PointXY(x,y);
    }
    private boolean rsi(PointXY rayPoint, PointXY rayVector, PointXY c, PointXY d){
        return PointXY.raySegmentIntersect(rayPoint, rayVector, c, d);
    }

    @Test
    public void testRaySegmentIntersect(){
        PointXY p1 = new PointXY(5, 8);
        PointXY p2 = new PointXY(8, 8).subtractedBy(p1);

        Assert.assertFalse(PointXY.raySegmentIntersect(p(5, 5), p(1, 0), p1, p2));
        Assert.assertFalse(PointXY.raySegmentIntersect(p(5, 5), p(1, -1), p1, p2));
        Assert.assertFalse(PointXY.raySegmentIntersect(p(5, 5), p(0, -1), p1, p2));
        Assert.assertFalse(PointXY.raySegmentIntersect(p(5, 5), p(-1, -1), p1, p2));
        Assert.assertFalse(PointXY.raySegmentIntersect(p(5, 5), p(-1, 0), p1, p2));
        Assert.assertFalse(PointXY.raySegmentIntersect(p(5, 5), p(-1, 1), p1, p2));
        Assert.assertFalse(PointXY.raySegmentIntersect(p(5, 5), p(-1, 10000), p1, p2));
        Assert.assertTrue(PointXY.raySegmentIntersect(p(5, 5), p(0, 1), p1, p2));
        Assert.assertTrue(PointXY.raySegmentIntersect(p(5, 5), p(1, 2), p1, p2));
        Assert.assertTrue(PointXY.raySegmentIntersect(p(5, 5), p(1, 1), p1, p2));
        Assert.assertTrue(PointXY.raySegmentIntersect(p(5, 5), p(100, 100), p1, p2));
        Assert.assertFalse(PointXY.raySegmentIntersect(p(5, 5), p(4, 3), p1, p2));

        // vertical line
        PointXY p3 = new PointXY(-10, 10);
        PointXY p4 = new PointXY(-10, -5).subtractedBy(p3);
        PointXY p4b = new PointXY(-9, -5).subtractedBy(p3); // give it a slight angle

        // aimed straigt back (x-) at it
        Assert.assertFalse(rsi(p(0, 11), p(-1, 0), p3, p4));
        Assert.assertTrue(rsi(p(0, 10), p(-1, 0), p3, p4));
        Assert.assertTrue(rsi(p(0, 0), p(-1, 0), p3, p4));
        Assert.assertTrue(rsi(p(0, -5), p(-1, 0), p3, p4));
        Assert.assertFalse(rsi(p(0, -6), p(-1, 0), p3, p4));

        // spray from origin
        Assert.assertFalse(rsi(p(0, 0), p(-1, 2), p3, p4));
        Assert.assertTrue(rsi(p(0, 0), p(-1, 1), p3, p4));
        Assert.assertTrue(rsi(p(0, 0), p(-20, 1), p3, p4));
        Assert.assertTrue(rsi(p(0, 0), p(-2, -1), p3, p4));
        Assert.assertFalse(rsi(p(0, 0), p(-4, -3), p3, p4));
        Assert.assertFalse(rsi(p(0, 0), p(-1, 2), p3, p4b));
        Assert.assertTrue(rsi(p(0, 0), p(-1, 1), p3, p4b));
        Assert.assertTrue(rsi(p(0, 0), p(-20, 1), p3, p4b));
        Assert.assertTrue(rsi(p(0, 0), p(-2, -1), p3, p4b));
        Assert.assertFalse(rsi(p(0, 0), p(-4, -3), p3, p4b));
    }

    @Test
    public void testRayCircleIntersect(){
        Assert.assertTrue(PointXY.rayCircleIntersect(p(0, 0), p(1, 0), p(5, 0), 1));
        Assert.assertTrue(PointXY.rayCircleIntersect(p(0, 0), p(1, 0), p(5, 1), 1));
        Assert.assertFalse(PointXY.rayCircleIntersect(p(0, 0), p(1, 0), p(5, 2), 1));
        Assert.assertTrue(PointXY.rayCircleIntersect(p(0, 0), p(1, 0), p(5, 2), 2));

        // perpendicular
        Assert.assertTrue(PointXY.rayCircleIntersect(p(0, 0), p(1, 0), p(0, 1), 1));
        Assert.assertFalse(PointXY.rayCircleIntersect(p(0, 0), p(1, 0), p(0, 2), 1));
        Assert.assertTrue(PointXY.rayCircleIntersect(p(0, 0), p(1, 0), p(0, -1), 1));
        Assert.assertFalse(PointXY.rayCircleIntersect(p(0, 0), p(1, 0), p(0, -2), 1));

        // behind
        Assert.assertFalse(PointXY.rayCircleIntersect(p(0, 0), p(1, 0), p(-5, 0), 1));

        Assert.assertFalse(PointXY.rayCircleIntersect(p(0, 0), p(-1, 0), p(5, 0), 1));
        Assert.assertFalse(PointXY.rayCircleIntersect(p(0, 0), p(1, 1), p(5, 0), 1));
        Assert.assertFalse(PointXY.rayCircleIntersect(p(0, 0), p(-1, 1), p(5, 0), 1));
        Assert.assertFalse(PointXY.rayCircleIntersect(p(0, 0), p(-1, -1), p(5, 0), 1));
        Assert.assertFalse(PointXY.rayCircleIntersect(p(0, 0), p(0, -1), p(5, 0), 1));
        Assert.assertFalse(PointXY.rayCircleIntersect(p(0, 0), p(0, 1), p(5, 0), 1));

        Assert.assertTrue(PointXY.rayCircleIntersect(p(-4, -3), p(4, 8), p(1, 4), 4));
        Assert.assertTrue(PointXY.rayCircleIntersect(p(-4, -3), p(3, 8), p(1, 4), 4));
        Assert.assertTrue(PointXY.rayCircleIntersect(p(-4, -3), p(5, 8), p(1, 4), 4));

        Assert.assertFalse(PointXY.rayCircleIntersect(p(-4, -3), p(7, 4), p(1, 4), 3));
        Assert.assertFalse(PointXY.rayCircleIntersect(p(-4, -3), p(-7, 4), p(1, 4), 3));
        Assert.assertFalse(PointXY.rayCircleIntersect(p(-4, -3), p(7, -4), p(1, 4), 3));
        Assert.assertFalse(PointXY.rayCircleIntersect(p(-4, -3), p(0, 1), p(1, 4), 3));

        Assert.assertTrue(PointXY.rayCircleIntersect(p(-17920, -58368), p(-1, -9), p(-18432, -62976), 2));
    }

    @Test
    public void testIntersectSegmentsForPoly(){
        LineSegmentXY line1 = new LineSegmentXY(new PointXY(-15365, -46083), new PointXY(-15371, -34819));
        LineSegmentXY line2 = new LineSegmentXY(new PointXY(-18435, -38914), new PointXY(-11264, -38912));
        Assert.assertTrue(PointXY.segmentsIntersect(line1.getP1(), line1.getVector(), line2.getP1(), line2.getVector()));
        Assert.assertTrue(PointXY.intersectSementsForPoly(line1.getP1(), line1.getVector(), line2.getP1(), line2.getVector(), false, false));
    }

    @Test
    public void testMidpoint(){
        Assert.assertEquals(p(5, 0), PointXY.midpoint(p(0, 0), p(10, 0)));
        Assert.assertEquals(p(6, 5), PointXY.midpoint(p(6, 0), p(6, 10)));
        Assert.assertEquals(p(6, 5), PointXY.midpoint(p(6, 10), p(6, 0)));
        Assert.assertEquals(p(0, 0), PointXY.midpoint(p(10, 10), p(-10, -10)));
    }

    @Test
    public void testMultipliedBy(){
        Assert.assertEquals(p(0, 0), p(0, 0).multipliedBy(0));
        Assert.assertEquals(p(0, 0), p(0, 0).multipliedBy(100));
        Assert.assertEquals(p(0, 0), p(100, 100).multipliedBy(0));

        Assert.assertEquals(p(1, 2).multipliedBy(3), p(3, 6));
    }

    private Pair<Double, Double> intersectForRayTU(PointXY ray, PointXY rayvec, PointXY p0, PointXY p1){
        // EndingExclusive=false is used for sprite-wall testing
        return PointXY.intersectForTU(ray, rayvec, p0, p1.subtractedBy(p0), true, false, false);
    }

    @Test
    public void testIntersectForTU(){
        double delta = 0.00001;
        Pair<Double, Double> result = intersectForRayTU(p(0, 0), p(10, 0), p(10, 5), p(10, -5));
        Assert.assertNotNull(result);
        Assert.assertTrue(result.getLeft() - 1.0 < delta);
        Assert.assertTrue(result.getRight() - 0.5 < delta);
    }

    @Test
    public void testVectorRotatedCW(){
        // remember, y+ points "down"
        Assert.assertEquals(p(0, 0), p(0, 0).vectorRotatedCW());
        Assert.assertEquals(p(0, 1), p(1, 0).vectorRotatedCW());
        Assert.assertEquals(p(-1, 0), p(0, 1).vectorRotatedCW());
        Assert.assertEquals(p(0, -3), p(-3, 0).vectorRotatedCW());
        Assert.assertEquals(p(1, 0), p(0, -1).vectorRotatedCW());
    }

    @Test
    public void testVectorRotatedCCW(){
        // remember, y+ points "down"
        Assert.assertEquals(p(0, 0), p(0, 0).vectorRotatedCCW());
        Assert.assertEquals(p(0, -1), p(1, 0).vectorRotatedCCW());
        Assert.assertEquals(p(1, 0), p(0, 1).vectorRotatedCCW());
        Assert.assertEquals(p(0, -2), p(2, 0).vectorRotatedCCW());
        Assert.assertEquals(p(5, 0), p(0, 5).vectorRotatedCCW());
    }

    @Test
    public void testToHeading(){
        Assert.assertEquals(Heading.E, p(1, 0).toHeading());
        Assert.assertEquals(Heading.E, p(50, 0).toHeading());
        Assert.assertEquals(Heading.S, p(0, 2).toHeading());
        Assert.assertEquals(Heading.W, p(-1, 0).toHeading());
        Assert.assertEquals(Heading.N, p(0, -10).toHeading());
    }

    @Test(expected = RuntimeException.class)
    public void testToHeadingThrows(){
        p(0, 0).toHeading();
    }

    @Test(expected = RuntimeException.class)
    public void testToHeadingThrows2(){
        p(1, 2).toHeading();
    }
}
