package trn;

import org.junit.Assert;
import org.junit.Test;

public class MapUtilTests {

    private PointXY p(int x, int y){
        return new PointXY(x, y);
    }

    private Wall w(int x, int y){
        Wall w = new Wall();
        w.x = x;
        w.y = y;
        return w;
    }

    private Sprite s(int x, int y, int ang){
        Sprite s = new Sprite();
        s.x = x;
        s.y = y;
        s.ang = (short)ang;
        return s;
    }

    @Test
    public void testIntersect(){
        Sprite s = new Sprite();
        s.x = 1;
        s.y = 1;
        s.ang = AngleUtil.ANGLE_RIGHT;

        // remember: duke coordinations have y+ going DOWN (south)

        // wall is to the right
        Assert.assertTrue(MapUtil.isSpritePointedAtWall(s(1, 1, AngleUtil.ANGLE_RIGHT), w(2, -10), w(2, 10)));
        Assert.assertFalse(MapUtil.isSpritePointedAtWall(s(1, 11, AngleUtil.ANGLE_RIGHT), w(2, -10), w(2, 10)));
        Assert.assertFalse(MapUtil.isSpritePointedAtWall(s(1, 1, AngleUtil.ANGLE_DOWN), w(2, -10), w(2, 10)));
        Assert.assertFalse(MapUtil.isSpritePointedAtWall(s(1, 1, AngleUtil.ANGLE_LEFT), w(2, -10), w(2, 10)));
        Assert.assertFalse(MapUtil.isSpritePointedAtWall(s(1, 1, AngleUtil.ANGLE_UP), w(2, -10), w(2, 10)));

        // wall is to the left
        Assert.assertFalse(MapUtil.isSpritePointedAtWall(s(1, 1, AngleUtil.ANGLE_RIGHT), w(-200, -10), w(-200, 10)));
        Assert.assertFalse(MapUtil.isSpritePointedAtWall(s(1, 1, AngleUtil.ANGLE_DOWN), w(-200, -10), w(-200, 10)));
        Assert.assertTrue(MapUtil.isSpritePointedAtWall(s(1, 1, AngleUtil.ANGLE_LEFT), w(-200, -10), w(-200, 10)));
        Assert.assertTrue(MapUtil.isSpritePointedAtWall(s(1024, 1, AngleUtil.ANGLE_LEFT), w(-200, -10), w(-200, 10)));
        Assert.assertFalse(MapUtil.isSpritePointedAtWall(s(1, 1, AngleUtil.ANGLE_UP), w(-200, -10), w(-200, 10)));


        // wall is north
        Assert.assertTrue(MapUtil.isSpritePointedAtWall(s(0, 0, AngleUtil.ANGLE_UP), w(-1, -10), w(1, -10)));
        Assert.assertTrue(MapUtil.isSpritePointedAtWall(s(-1, 0, AngleUtil.ANGLE_UP), w(-1, -10), w(1, -10)));
        Assert.assertTrue(MapUtil.isSpritePointedAtWall(s(1, 0, AngleUtil.ANGLE_UP), w(-1, -10), w(1, -10)));
        Assert.assertFalse(MapUtil.isSpritePointedAtWall(s(-2, 0, AngleUtil.ANGLE_UP), w(-1, -10), w(1, -10)));
        Assert.assertFalse(MapUtil.isSpritePointedAtWall(s(2, 0, AngleUtil.ANGLE_UP), w(-1, -10), w(1, -10)));

    }

    @Test
    public void testDistanceToLineSpecial(){
        Assert.assertEquals(5, (int)MapUtil.distanceToLineSpecial(p(0, 0), p(0, 1), p(5, 10)));
        Assert.assertEquals(-5, (int)MapUtil.distanceToLineSpecial(p(0, 0), p(0, 1), p(-5, 10)));
        Assert.assertEquals(Math.sqrt(2*2 + 1*1), MapUtil.distanceToLineSpecial(p(0,0), p(2, 1), p(-1, -3)), 0.000001);
        Assert.assertEquals(-Math.sqrt(2*2 + 1*1), MapUtil.distanceToLineSpecial(p(0,0), p(2, 1), p(3, 4)), 0.000001);
    }

}
