package trn;

import org.junit.Assert;
import org.junit.Test;

public class LineXYTests {


    private PointXY p(int x, int y){
        return new PointXY(x, y);
    }

    @Test
    public void testEquals(){
        LineXY line0 = new LineXY(p(0, 0), p(1, 0));
        LineXY line1 = new LineXY(p(0, 0), p(1, 0));
        Assert.assertTrue(line0.point.equals(line1.point));
        Assert.assertEquals(line0.point, line1.point);
        Assert.assertTrue(line0.vector.equals(line1.vector));
        Assert.assertEquals(line0.vector, line1.vector);
        Assert.assertTrue(line0.equals(line1));
        Assert.assertTrue(new LineXY(p(0, 0), p(1, 0)).equals(new LineXY(p(0, 0), p(1, 0))));
        Assert.assertEquals(new LineXY(p(0, 0), p(1, 0)), new LineXY(p(0, 0), p(1, 0)));
        Assert.assertNotEquals(new LineXY(p(0, 0), p(1, 0)), new LineXY(p(0, 0), p(0, 1)));
        Assert.assertEquals(line0.hashCode(), line1.hashCode());
    }

    @Test
    public void testRotateLine(){
        LineXY line = new LineXY(p(0, 0), p(-3, 2));
        Assert.assertEquals(new LineXY(p(0, 0), p(2, 3)), line.rotatedCW());
        Assert.assertEquals(new LineXY(p(0, 0), p(3, -2)), line.rotatedCW().rotatedCW());
    }

    @Test
    public void testFromPoints(){
        Assert.assertEquals(new LineXY(p(0, 0), p(5, 5)), LineXY.fromPoints(p(0, 0), p(5, 5)));
        Assert.assertEquals(new LineXY(p(1, 2), p(-6, -6)), LineXY.fromPoints(p(1, 2), p(-5, -4)));
    }
}
