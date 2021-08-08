package trn;

import org.junit.Assert;
import org.junit.Test;

public class SectorTests {

    @Test
    public void testGetSlopedZ(){
        int onePageDown = 512;  // remember, sloping "down" increases Z
        int twoPageDown = 1024;

        // these values were established by experimentation
        Assert.assertEquals(8192, Sector.getSlopedZ(onePageDown, 4096));
        Assert.assertEquals(-4096, Sector.getSlopedZ(onePageDown, -2048));

        Assert.assertEquals(16384, Sector.getSlopedZ(twoPageDown, 4096));
        Assert.assertEquals(-8192, Sector.getSlopedZ(twoPageDown, -2048));
    }
}
