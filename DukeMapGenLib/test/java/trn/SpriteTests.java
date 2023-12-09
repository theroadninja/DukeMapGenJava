package trn;

import org.junit.Assert;
import org.junit.Test;

import java.io.IOException;
import java.util.NoSuchElementException;

public class SpriteTests {
    public static final String FILENAME = "spritestat.map";

    private static PointXY p(int x, int y){
        return new PointXY(x, y);
    }

    private static Sprite sp(PointXY loc, PointXY angleVector){
        Sprite s = new Sprite();
        s.setAngle(AngleUtil.angleOf(angleVector));
        s.setLocation(loc);
        return s;
    }

    private static Sprite getFirst(Map map, int tex) {
        for(Sprite sprite: map.sprites){
            if(sprite.tex() == tex){
                return sprite;
            }
        }
        throw new NoSuchElementException(String.format("so sprite with tex %s", tex));
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

    @Test
    public void testStat() throws IOException {
        Map map = JavaTestUtils.readTestMap(FILENAME);

        final int LIGHT = 685;
        final int CAMERA = 686;
        final int ALPHA = 520;
        final int BETA = 521;
        final int DELTA = 523;
        final int GAMMA = 522;
        final int BLUE_CARPET = 898;
        final int RED_CARPET = 899;
        final int BROWN_CARPET = 900;
        final int BILLBOARD = 826;
        final int PHONE = 967;
        final int CRATE = 884;

        // blocking
        Assert.assertTrue(getFirst(map, LIGHT).getStat().isBlocking());
        Assert.assertFalse(getFirst(map, CAMERA).getStat().isBlocking());

        // flipping
        Assert.assertFalse(getFirst(map, ALPHA).getStat().isXFlipped());
        Assert.assertFalse(getFirst(map, ALPHA).getStat().isYFlipped());
        Assert.assertTrue(getFirst(map, BETA).getStat().isXFlipped());
        Assert.assertFalse(getFirst(map, BETA).getStat().isYFlipped());
        Assert.assertTrue(getFirst(map, DELTA).getStat().isXFlipped());
        Assert.assertTrue(getFirst(map, DELTA).getStat().isYFlipped());
        Assert.assertFalse(getFirst(map, GAMMA).getStat().isXFlipped());
        Assert.assertTrue(getFirst(map, GAMMA).getStat().isYFlipped());

        // alignment
        Assert.assertEquals(SpriteStat.ALIGN_NORMAL, getFirst(map, BLUE_CARPET).getStat().getAlignment());
        Assert.assertEquals(SpriteStat.ALIGN_WALL, getFirst(map, RED_CARPET).getStat().getAlignment());
        Assert.assertEquals(SpriteStat.ALIGN_FLOOR, getFirst(map, BROWN_CARPET).getStat().getAlignment());

        // one sided
        Assert.assertTrue(getFirst(map, BILLBOARD).getStat().isOneSided());

        // floor centered
        Assert.assertTrue(getFirst(map, PHONE).getStat().isCenteredOnFloor());

        // hitscan blocking
        Assert.assertTrue(getFirst(map, CRATE).getStat().isHitscanBlocking());

    }
}
