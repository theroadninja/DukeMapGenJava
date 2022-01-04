package trn;

import org.junit.Assert;
import org.junit.Test;

public class WallViewTests {

    private PointXY p(int x, int y){
        return new PointXY(x, y);
    }

    private WallView wall(PointXY p1, PointXY p2){
        return new WallView(new Wall(p1, null), -1, new LineSegmentXY(p1, p2), -1, -1);
    }

    private WallView testWall(int wallId, PointXY p0, PointXY p1) {
        Wall w = new Wall(p0.x, p0.y);
        return new WallView(w, wallId, new LineSegmentXY(p0, p1), -1, -1);
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

    @Test
    public void testEquals(){
        Assert.assertTrue(wall(p(1, 2), p(3, 4)).equals(wall(p(1, 2), p(3, 4))));
        Assert.assertEquals(wall(p(1, 2), p(3, 4)), wall(p(1, 2), p(3, 4)));

        Assert.assertFalse(wall(p(1, 2), p(3, 4)).equals(wall(p(1, 2), p(3, 5))));
        Assert.assertNotEquals(wall(p(1, 2), p(3, 4)), wall(p(1, 2), p(3, 5)));
    }

    @Test
    public void testTranslated(){
        WallView w = wall(p(64, -48), p(128, -64));
        Assert.assertEquals(w, w.translated(new PointXYZ(0, 0, 0)));
        Assert.assertEquals(w, w.translated(new PointXYZ(0, 0, 10)));

        Assert.assertEquals(wall(p(74, -48), p(138, -64)), w.translated(new PointXYZ(10, 0, 0)));
        Assert.assertEquals(wall(p(54, -48), p(118, -64)), w.translated(new PointXYZ(-10, 0, 0)));
        Assert.assertEquals(wall(p(64, -38), p(128, -54)), w.translated(new PointXYZ(0, 10, 0)));
        Assert.assertEquals(wall(p(64, -58), p(128, -74)), w.translated(new PointXYZ(0, -10, 0)));
        Assert.assertEquals(wall(p(65, -50), p(129, -66)), w.translated(new PointXYZ(1, -2, 0)));
    }

    @Test
    public void testReversed(){
        WallView w = wall(p(64, -48), p(128, -64));
        WallView r = wall(p(128, -64), p(64, -48));
        Assert.assertTrue(w.reversed().equals(r));
        Assert.assertEquals(w.reversed(), r);
        Assert.assertEquals(w, r.reversed());
        Assert.assertEquals(w.reversed().reversed(), w);
    }
}
