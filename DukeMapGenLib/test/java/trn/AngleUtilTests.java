package trn;

import org.junit.Assert;
import org.junit.Test;

public class AngleUtilTests {

    final int R = AngleUtil.RANGE;

    private PointXY unit(int ang){
        return AngleUtil.unitVector(ang);
    }

    @Test
    public void testUnitVector(){
        // for 45 degree angles, the unit vector values will be 11584 (R * sqrt(0.5))

        Assert.assertEquals(new PointXY(R, 0), unit(0));
        Assert.assertEquals(new PointXY(R, 0), unit(-2048));
        Assert.assertEquals(new PointXY(R, 0), unit(2048));
        Assert.assertEquals(new PointXY(R, 0), unit(AngleUtil.ANGLE_RIGHT));

        Assert.assertEquals(new PointXY(0, R), unit(512));
        Assert.assertEquals(new PointXY(0, R), unit(512-2048));
        Assert.assertEquals(new PointXY(0, R), unit(512+2048));
        Assert.assertEquals(new PointXY(0, R), unit(AngleUtil.ANGLE_DOWN));

        Assert.assertEquals(new PointXY(-R, 0), unit(1024));
        Assert.assertEquals(new PointXY(-R, 0), unit(1024-2048));
        Assert.assertEquals(new PointXY(-R, 0), unit(1024+2048));
        Assert.assertEquals(new PointXY(-R, 0), unit(AngleUtil.ANGLE_LEFT));

        Assert.assertEquals(new PointXY(0, -R), unit(1536));
        Assert.assertEquals(new PointXY(0, -R), unit(1536-2048));
        Assert.assertEquals(new PointXY(0, -R), unit(1536+2048));
        Assert.assertEquals(new PointXY(0, -R), unit(AngleUtil.ANGLE_UP));
    }

    @Test
    public void testVectorToAngle(){
        Assert.assertEquals(0, AngleUtil.angleOf(new PointXY(R, 0)));

        //Assert.assertEquals(512, AngleUtil.angleOf(new PointXY(0, -R)));
        Assert.assertEquals(1536, AngleUtil.angleOf(new PointXY(0, -R)));

        Assert.assertEquals(1024, AngleUtil.angleOf(new PointXY(-R, 0)));

        //Assert.assertEquals(1536, AngleUtil.angleOf(new PointXY(0, R)));
        Assert.assertEquals(512, AngleUtil.angleOf(new PointXY(0, R)));
    }
}
