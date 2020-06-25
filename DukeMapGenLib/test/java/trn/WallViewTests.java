package trn;

import org.junit.Assert;
import org.junit.Test;

public class WallViewTests {

    private PointXY p(int x, int y){
        return new PointXY(x, y);
    }

    private WallView wall(PointXY p1, PointXY p2){
        return new WallView(new Wall(), -1, new LineSegmentXY(p1, p2));
    }

    private WallView testWall(int wallId, PointXY p0, PointXY p1) {
        Wall w = new Wall(p0.x, p0.y);
        return new WallView(w, wallId, new LineSegmentXY(p0, p1));
    }

    @Test
    public void testContiguous(){
        PointXY a = new PointXY(0, 0);
        PointXY b = new PointXY(5, 5);
        PointXY bb = new PointXY(5, 5); // b.c of java == bullshit
        PointXY c = new PointXY(5, 10);
        PointXY d = new PointXY(10, 20);

        Assert.assertTrue(wall(a, b).contiguous(wall(b, c)));
        Assert.assertTrue(wall(b, c).contiguous(wall(a, bb)));
        Assert.assertTrue(wall(b, c).contiguous(wall(a, b)));

        Assert.assertTrue(wall(a, b).contiguous(wall(b, d)));
        Assert.assertTrue(wall(a, bb).contiguous(wall(b, d)));
        Assert.assertTrue(wall(b, d).contiguous(wall(a, b)));
        Assert.assertTrue(wall(bb, d).contiguous(wall(a, bb)));

        Assert.assertTrue(wall(a, b).contiguous(wall(b, a)));
        Assert.assertTrue(wall(b, c).contiguous(wall(c, d)));

        Assert.assertFalse(wall(a, b).contiguous(wall(c, b)));
        Assert.assertTrue(wall(a, b).contiguous(wall(c, b), false));
        Assert.assertFalse(wall(b, a).contiguous(wall(b, c)));
        Assert.assertTrue(wall(b, a).contiguous(wall(b, c), false));
        Assert.assertFalse(wall(b, a).contiguous(wall(b, d)));
        Assert.assertTrue(wall(b, a).contiguous(wall(b, d), false));
        Assert.assertFalse(wall(b, a).contiguous(wall(b, a)));
        Assert.assertTrue(wall(b, a).contiguous(wall(b, a), false));
        Assert.assertFalse(wall(a, b).contiguous(wall(c, d)));
        Assert.assertFalse(wall(a, b).contiguous(wall(c, d), false));

        WallView w0 = testWall(1, p(10, 40), p(30, 40));
        WallView w1 = testWall(2, p(30, 40), p(30, 30));
        Assert.assertTrue(w0.contiguous(w1));
    }
}
