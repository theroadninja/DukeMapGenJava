package trn;

import org.junit.Assert;
import org.junit.Test;

public class LineSegmentXYTests {

    private static PointXY p(int x, int y){
        return new PointXY(x, y);
    }
    private static LineSegmentXY line(PointXY p1, PointXY p2){
        return new LineSegmentXY(p1, p2);
    }

    @Test(expected = IllegalArgumentException.class)
    public void noSingularities(){
        new LineSegmentXY(p(5, 5), p(5, 5));
    }

    @Test
    public void testMidpoint(){
        Assert.assertEquals(p(5, 0), line(p(0, 0), p(10, 0)).midpoint());
        Assert.assertEquals(p(5, 0), line(p(0, 0), p(11, 0)).midpoint());
        Assert.assertEquals(p(5, 0), line(p(10, 0), p(0, 0)).midpoint());
        Assert.assertEquals(p(-5, 0), line(p(0, 0), p(-10, 0)).midpoint());
        Assert.assertEquals(p(0, 5), line(p(0, 0), p(0, 10)).midpoint());
        Assert.assertEquals(p(0, -5), line(p(0, 0), p(0, -10)).midpoint());
        Assert.assertEquals(p(0, -5), line(p(0, 0), p(0, -11)).midpoint());
        Assert.assertEquals(p(15, 30), line(p(10, 20), p(20, 40)).midpoint());
        Assert.assertEquals(p(15, -10), line(p(10, 20), p(20, -40)).midpoint());
        Assert.assertEquals(p(-5, 30), line(p(10, 20), p(-20, 40)).midpoint());
        Assert.assertEquals(p(-5, -10), line(p(10, 20), p(-20, -40)).midpoint());
    }

    @Test
    public void testGetVector(){
        Assert.assertEquals(line(p(0, 0), p( 1,  1)).getVector(), p( 1,  1));
        Assert.assertEquals(line(p(1, 1), p( 0, 0)).getVector(), p(-1, -1));
        Assert.assertEquals(line(p(0, 0), p( 2,  1)).getVector(), p( 2,  1));
        Assert.assertEquals(line(p(0, 0), p( 1,  2)).getVector(), p( 1,  2));
        Assert.assertEquals(line(p(0, 0), p( 1,  0)).getVector(), p( 1,  0));
        Assert.assertEquals(line(p(0, 0), p(-1,  0)).getVector(), p(-1,  0));
        Assert.assertEquals(line(p(0, 0), p(-1, -1)).getVector(), p(-1, -1));

        Assert.assertEquals(line(p(5, 5), p( 6,  5)).getVector(), p( 1, 0));
        Assert.assertEquals(line(p(5, 5), p( 6,  2)).getVector(), p( 1, -3));
        Assert.assertEquals(line(p(5, 5), p( 5,-10)).getVector(), p( 0,-15));
        Assert.assertEquals(line(p(5, 5), p( 1,-1)).getVector(), p(-4,-6));
        Assert.assertEquals(line(p(5, 5), p( 1, 5)).getVector(), p(-4, 0));
        Assert.assertEquals(line(p(5, 5), p( 1, 100)).getVector(), p(-4,95));
        Assert.assertEquals(line(p(5, 5), p( 5, 60)).getVector(), p( 0,55));

        Assert.assertEquals(line(p(50, -10), p( 55,  5)).getVector(), p( 5, 15));
        Assert.assertEquals(line(p(50, -10), p( 55, -75)).getVector(), p( 5,-65));
        Assert.assertEquals(line(p(50, -10), p(0, 0)).getVector(), p(-50, 10));
    }

    @Test
    public void testEquals(){
        Assert.assertTrue(line(p(0, 0), p(1, 1)).equals(line(p(0, 0), p(1, 1))));
        Assert.assertEquals(line(p(0, 0), p(1, 1)), line(p(0, 0), p(1, 1)));
        Assert.assertTrue(line(p(1, 2), p(3, 4)).equals(line(p(1, 2), p(3, 4))));
        Assert.assertEquals(line(p(1, 2), p(3, 4)), line(p(1, 2), p(3, 4)));

        Assert.assertFalse(line(p(0, 0), p(1, 1)).equals(line(p(0, 0), p(1, 2))));
        Assert.assertNotEquals(line(p(0, 0), p(1, 1)), line(p(0, 0), p(1, 2)));
        Assert.assertFalse(line(p(5, 0), p(1, 1)).equals(line(p(0, 0), p(1, 1))));
        Assert.assertNotEquals(line(p(5, 0), p(1, 1)), line(p(0, 0), p(1, 1)));
    }

    @Test
    public void testIntersect(){
        Assert.assertTrue(line(p(0, 0), p(1, 1)).intersects(line(p(0, 0), p(2, 1)))); // TODO - this seems wrong

        // NOTE: this is the weird case - no intersect because they are parallel
        // although it seems counter intuitive
        Assert.assertFalse(line(p(-2, 4), p(2, 4)).intersects(line(p(3, 4), p(5, 4))));
        Assert.assertFalse(line(p(-2, 4), p(3, 4)).intersects(line(p(3, 4), p(5, 4))));

        Assert.assertTrue(line(p(-2, 4), p(3, 4)).intersects(line(p(3, 4), p(5, 3))));

        for(int x = -30; x < 25; ++x){
            Assert.assertTrue(line(p(-20, 5), p(19, 6)).intersects(line(p(x, -500), p(1, 500))));
            Assert.assertTrue(line(p(-20, 5), p(19, 6)).intersects(line(p(x, -500), p(-x, 500))));
        }
    }

    @Test
    public void testIntersectRay(){
        Assert.assertTrue(line(p(1,0), p(1, 1)).intersectsRay(p(0, 0), p(1, 0), false));
        Assert.assertTrue(line(p(1,0), p(-10, 1)).intersectsRay(p(0, 0), p(1, 0), false));
        Assert.assertTrue(line(p(1,0), p(10, 1)).intersectsRay(p(0, 0), p(1, 0), false));

        Assert.assertFalse(line(p(1,0), p(1, 1)).intersectsRay(p(0, 0), p(-1, 0), false));
        Assert.assertFalse(line(p(1,0), p(1, 1)).intersectsRay(p(0, 0), p(0, 1), false));
        Assert.assertFalse(line(p(1,0), p(1, 1)).intersectsRay(p(0, 0), p(0, -1), false));
        Assert.assertFalse(line(p(1,1), p(1, 2)).intersectsRay(p(0, 0), p(1, 0), false));
        Assert.assertFalse(line(p(-10,1), p(1, 2)).intersectsRay(p(0, 0), p(1, 0), false));
    }

    @Test
    public void testReversed(){
        Assert.assertTrue(line(p(1365, 2048), p(1365, 0)).equals(line(p(1365, 0), p(1365, 2048)).reversed()));
    }

    @Test
    public void testTranslated(){
        Assert.assertTrue(line(p(10, 15), p(20, 15)).equals(line(p(0, 0), p(10, 0)).translated(p(10, 15))));
        Assert.assertTrue(line(p(-10, 15), p(0, 15)).equals(line(p(0, 0), p(10, 0)).translated(p(-10, 15))));
        Assert.assertTrue(line(p(0, -15), p(10, -15)).equals(line(p(0, 0), p(10, 0)).translated(p(0, -15))));
        Assert.assertTrue(line(p(-9, -15), p(1, -15)).equals(line(p(0, 0), p(10, 0)).translated(p(-9, -15))));
    }
}
