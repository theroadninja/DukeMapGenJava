package trn;

import org.junit.Assert;
import org.junit.Test;

import java.util.ArrayList;
import java.util.List;

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

    @Test
    public void testNormalizedVector(){
        LineXY lineA = LineXY.fromPoints(p(2, 2), p(4, 2));
        Assert.assertEquals(1, (int)lineA.getNormalizedVector().x);
        Assert.assertEquals(0, (int)lineA.getNormalizedVector().y);

        LineXY lineB = LineXY.fromPoints(p(4, 2), p(2, 2));
        Assert.assertEquals(-1, (int)lineB.getNormalizedVector().x);
        Assert.assertEquals(0, (int)lineB.getNormalizedVector().y);

        LineXY lineC = LineXY.fromPoints(p(-5, -5), p(-5, 1));
        Assert.assertEquals(0, (int)lineC.getNormalizedVector().x);
        Assert.assertEquals(1, (int)lineC.getNormalizedVector().y);

        LineXY lineD = LineXY.fromPoints(p(-5, -5), p(-5, -6));
        Assert.assertEquals(0, (int)lineD.getNormalizedVector().x);
        Assert.assertEquals(-1, (int)lineD.getNormalizedVector().y);
    }

    @Test
    public void testDistanceTo(){

        //   (2,2)-------------(4,2)
        LineXY lineA = LineXY.fromPoints(p(2, 2), p(4, 2));
        Assert.assertEquals(0, (int)lineA.distanceTo(p(1, 2)));
        Assert.assertEquals(0, (int)lineA.distanceTo(p(2, 2)));
        Assert.assertEquals(0, (int)lineA.distanceTo(p(3, 2)));
        Assert.assertEquals(0, (int)lineA.distanceTo(p(4, 2)));
        Assert.assertEquals(0, (int)lineA.distanceTo(p(5, 2)));

        Assert.assertEquals(1, (int)lineA.distanceTo(p(1, 1)));
        Assert.assertEquals(1, (int)lineA.distanceTo(p(2, 1)));
        Assert.assertEquals(1, (int)lineA.distanceTo(p(3, 1)));
        Assert.assertEquals(1, (int)lineA.distanceTo(p(4, 1)));
        Assert.assertEquals(1, (int)lineA.distanceTo(p(5, 1)));

        Assert.assertEquals(1, (int)lineA.distanceTo(p(1, 3)));
        Assert.assertEquals(1, (int)lineA.distanceTo(p(2, 3)));
        Assert.assertEquals(1, (int)lineA.distanceTo(p(3, 3)));
        Assert.assertEquals(1, (int)lineA.distanceTo(p(4, 3)));
        Assert.assertEquals(1, (int)lineA.distanceTo(p(5, 3)));

        //                    X
        //              X
        //        X
        //  X
        // Line goes over 2 and up 1
        LineXY lineB = LineXY.fromPoints(p(0, 0), p(2, 1));
        LineXY lineC = LineXY.fromPoints(p(0, 0), p(-2, -1));
        List<LineXY> mOneHalf = new ArrayList<>(2);
        mOneHalf.add(lineB);
        mOneHalf.add(lineC);
        PointXY[] points = new PointXY[]{p(-1, -3), p(1, -2), p(3, -1), p(5, 0), p(-3, 1), p(-1, 2), p(1, 3), p(3, 4)};
        for(LineXY line : mOneHalf){
            for(PointXY p : points){
                Assert.assertEquals(Math.sqrt(2*2 + 1*1), line.distanceTo(p), 0.0000001);
            }

        }



    }
}
