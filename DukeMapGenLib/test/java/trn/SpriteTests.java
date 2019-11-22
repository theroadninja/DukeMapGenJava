package trn;

import org.junit.Assert;
import org.junit.Test;

public class SpriteTests {

    private static PointXY p(int x, int y){
        return new PointXY(x, y);
    }

    private static Sprite sp(PointXY loc, PointXY angleVector){
        Sprite s = new Sprite();
        s.setAngle(AngleUtil.angleOf(angleVector));
        s.setLocation(loc);
        return s;
    }

    @Test
    public void testIntersectsSegment(){
        PointXY z = p(0, 0);
        Sprite s = sp(z, p(1, 0));

        Assert.assertTrue(s.intersectsSegment(p(0, 0), p(0, -2)));
        Assert.assertTrue(s.intersectsSegment(p(0, 0), p(0, 2)));
        Assert.assertFalse(s.intersectsSegment(p(0, 1), p(0, 2)));
        Assert.assertTrue(s.intersectsSegment(p(1, 0), p(1, -2)));
        Assert.assertTrue(s.intersectsSegment(p(1, 0), p(1, 2)));
        Assert.assertFalse(s.intersectsSegment(p(1, 1), p(1, 2)));

        Assert.assertFalse(sp(z, p(-1, 0)).intersectsSegment(p(1, 0), p(1, 2)));
        Assert.assertFalse(sp(z, p(-1, -1)).intersectsSegment(p(1, 0), p(1, 2)));

        Assert.assertFalse(sp(z, p(1, -1)).intersectsSegment(p(1, 0), p(1, 2)));
        Assert.assertFalse(sp(z, p(10, -1)).intersectsSegment(p(1, 0), p(1, 2)));
        Assert.assertFalse(sp(z, p(100, -1)).intersectsSegment(p(1, 0), p(1, 2)));
        Assert.assertTrue(sp(z, p(1, 1)).intersectsSegment(p(1, 0), p(1, 2)));
        Assert.assertTrue(sp(z, p(10, 1)).intersectsSegment(p(1, 0), p(1, 2)));
        Assert.assertTrue(sp(z, p(100, 1)).intersectsSegment(p(1, 0), p(1, 2)));
        Assert.assertFalse(sp(p(0, 2), p(1, 1)).intersectsSegment(p(1, 0), p(1, 2)));
        Assert.assertFalse(sp(p(0, 2), p(10, 1)).intersectsSegment(p(1, 0), p(1, 2)));
        Assert.assertFalse(sp(p(0 ,2), p(100, 1)).intersectsSegment(p(1, 0), p(1, 2)));
    }
}
