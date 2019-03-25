package trn;

import org.junit.Assert;
import org.junit.Test;

public class CFStatTests {

    @Test
    public void testGet(){
        CFStat c = new CFStat(0);
        for(int i = 0; i < 7; ++i){
            int j = (int)Math.pow(2, i);
            Assert.assertFalse(c.get(j));
        }

        c = new CFStat(1);
        Assert.assertTrue(c.parallaxing());
        Assert.assertTrue(c.get(1));
        Assert.assertFalse(c.get(2));
        Assert.assertFalse(c.get(4));

        c = new CFStat(2);
        Assert.assertTrue(c.sloped());
        Assert.assertFalse(c.get(1));
        Assert.assertTrue(c.get(2));
        Assert.assertFalse(c.get(4));

        c = new CFStat(4);
        Assert.assertTrue(c.swapxy());
        Assert.assertFalse(c.doubleSmooshiness());

        c = new CFStat(8);
        Assert.assertFalse(c.swapxy());
        Assert.assertTrue(c.doubleSmooshiness());

        c = new CFStat(4 + 8);
        Assert.assertTrue(c.swapxy());
        Assert.assertTrue(c.doubleSmooshiness());

        c = new CFStat(16 + 32);
        Assert.assertFalse(c.parallaxing());
        Assert.assertTrue(c.xflip());
        Assert.assertTrue(c.yflip());
        Assert.assertFalse(c.get(1));
        Assert.assertFalse(c.get(2));
        Assert.assertFalse(c.get(4));
        Assert.assertFalse(c.get(8));
        Assert.assertTrue(c.get(16));
        Assert.assertTrue(c.get(32));

    }
}
