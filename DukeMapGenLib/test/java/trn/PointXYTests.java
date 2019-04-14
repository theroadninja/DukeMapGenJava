package trn;

import org.junit.Assert;
import org.junit.Test;

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
}
