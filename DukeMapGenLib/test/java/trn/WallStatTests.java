package trn;

import org.junit.Assert;
import org.junit.Test;

public class WallStatTests {

    @Test
    public void testRemoveBits(){
        Assert.assertEquals(0, WallStat.removeBits(0, 1));
        Assert.assertEquals(0, WallStat.removeBits(1, 1));
        Assert.assertEquals(2, WallStat.removeBits(2, 1));
        Assert.assertEquals(2, WallStat.removeBits(3, 1));

        Assert.assertEquals(8, WallStat.removeBits(8, 1));
        Assert.assertEquals(6, WallStat.removeBits(7, 1));

        Assert.assertEquals(4, WallStat.removeBits(7, 3));
        Assert.assertEquals(4, WallStat.removeBits(7, 3));
        Assert.assertEquals(8, WallStat.removeBits(8, 3));
    }

    @Test
    public void testWallStat(){
        WallStat stat = new WallStat(0);
        Assert.assertFalse(stat.blockPlayer());
        Assert.assertFalse(stat.hitscan());

        WallStat stat2 = new WallStat(65);
        Assert.assertTrue(stat2.blockPlayer());
        Assert.assertTrue(stat2.hitscan());
        Assert.assertFalse(stat2.xflip());
    }

    @Test
    public void testFlagoff(){
        WallStat stat = new WallStat(WallStat.BLOCKABLE | WallStat.XFLIP | WallStat.HITSCAN);
        Assert.assertTrue(stat.blockPlayer());
        Assert.assertTrue(stat.hitscan());

        WallStat stat2 = stat.withValueChanged(WallStat.BLOCKABLE, false);
        Assert.assertFalse(stat2.blockPlayer());
        Assert.assertTrue(stat2.hitscan());

    }

}
