package trn;

import org.junit.Test;
import org.junit.Assert;

public class FVectorXYTests {

    private FVectorXY v(double x, double y) {
        return new FVectorXY(x, y);
    }

    private void assertEqual(FVectorXY expected, FVectorXY actual) {
        Assert.assertTrue(
            String.format("%s != %s", expected, actual),
            expected.almostEquals(actual)
        );
    }

    private void assertNotEqual(FVectorXY expected, FVectorXY actual) {
        Assert.assertFalse(
                String.format("%s != %s", expected, actual),
                expected.almostEquals(actual)
        );
    }

    @Test
    public void testAlmostEquals() {
        Assert.assertTrue(v(1.0, 0.0).almostEquals(v(1.0, 0.0)));
        Assert.assertTrue(v(-1.0, 0.0).almostEquals(v(-1.0, 0.0)));
        Assert.assertTrue(v(1.2, 3.4).almostEquals(v(1.2, 3.4)));

        Assert.assertFalse(v(-1.0, 0.0).almostEquals(v(1.0, 0.0)));
        Assert.assertFalse(v(1.0, 0.0).almostEquals(v(-1.0, 0.0)));
        Assert.assertFalse(v(1.2, 3.4).almostEquals(v(1.2, -3.4)));

        Assert.assertTrue(v(1.0, 0.0).almostEquals(v(1.0, 0.0)));
        Assert.assertTrue(v(-1.0, 0.0).almostEquals(v(-1.0, 0.0)));
        Assert.assertTrue(v(1.0000001, 0.0).almostEquals(v(1.0, 0.0)));

        Assert.assertTrue(v(1.00000009, 0.0).almostEquals(v(1.0, 0.0)));
        Assert.assertFalse(v(1.000002, 0.0).almostEquals(v(1.0, 0.0)));
        Assert.assertFalse(v(1.00001, 0.0).almostEquals(v(1.0, 0.0)));

        Assert.assertTrue(v(17.0, 1.00000009).almostEquals(v(17.0, 1.0)));
        Assert.assertTrue(v(17.0, 1.0).almostEquals(v(17.0, 1.0000009)));
        Assert.assertFalse(v(17.0, 1.000002).almostEquals(v(17.0, 1.0)));
        Assert.assertFalse(v(17.0, 1.0).almostEquals(v(17.0, 1.000002)));
        Assert.assertFalse(v(17.0, 1.00001).almostEquals(v(17.0, 1.0)));
        Assert.assertFalse(v(17.0, 1.0).almostEquals(v(17.0, 1.00001)));
    }


    @Test
    public void testRotateDegrees() {
        // FVectorXY v = new FVectorXY(1.0, 0.0);

        Assert.assertEquals(1.0, Math.sin(Math.toRadians(90)), 0.0001);
        Assert.assertEquals(0.0, Math.cos(Math.toRadians(90)), 0.0001);

        // NOTE:  rotatedDegresCW uses a formula that rotates ANTI-clockwise
        // in normal space.  It is only "clockwise" in build coordinates.
        // so this code will look wrong
        assertEqual(v(0.7071067, 0.7071067), v(1, 0).rotatedDegreesCW(45));
        assertEqual(v(0.7071067, -0.7071067), v(1, 0).rotatedDegreesCW(-45));
        assertEqual(v(0, 1), v(0.7071067, 0.7071067).rotatedDegreesCW(45));
        assertEqual(v(1, 0), v(0.7071067, 0.7071067).rotatedDegreesCW(-45));
        assertEqual(v(0, 1), v(1, 0).rotatedDegreesCW(90));
        assertEqual(v(-0.7071067, 0.7071067), v(1, 0).rotatedDegreesCW(90+45));
        assertEqual(v(-1, 0), v(1, 0).rotatedDegreesCW(180));
        for(double i = -720.0; i < 720 + 1; i += 360.0){
            assertEqual(v(0.7071067, 0.7071067), v(1, 0).rotatedDegreesCW(45 + i));
            assertEqual(v(0, 1), v(1, 0).rotatedDegreesCW(90 + i));
            assertEqual(v(-0.7071067, 0.7071067), v(1, 0).rotatedDegreesCW(90+45 + i));
            assertEqual(v(-1, 0), v(1, 0).rotatedDegreesCW(180 + i));
        }

        assertEqual(v(-1, 0), v(0, 1).rotatedDegreesCW(90));
        assertEqual(v(-0.7071067, 0.7071067), v(0, 1).rotatedDegreesCW(45));
        assertEqual(v(-1, 0), v(-0.7071067, 0.7071067).rotatedDegreesCW(45));

        assertEqual(v(0, -1), v(-1, 0).rotatedDegreesCW(90));
        assertEqual(v(1, 0), v(0, -1).rotatedDegreesCW(90));

        FVectorXY v0 = v(1.0, 0.0);
        for(int i = 0; i < 45; ++i){
            assertNotEqual(v(0.7071067, 0.7071067), v0);
            v0 = v0.rotatedDegreesCW(1.0);
        }
        assertEqual(v(0.7071067, 0.7071067), v0);
        v0 = v0.rotatedDegreesCW(1.0);
        assertNotEqual(v(0.7071067, 0.7071067), v0);

        FVectorXY v1 = v(1.0, 0.0);
        for(int i = 0; i < 360; ++i){
            v1 = v1.rotatedDegreesCW(1.0);
        }
        assertEqual(v(1.0, 0.0), v1);

        // pentagon
        FVectorXY v5 = v(0.0, 1.0);
        for(int i = 0; i < 5; ++i){
            // even though the inside angle is 108 degrees, the amount
            // we need to turn by (off the tangent) is 72   (108 + 72 = 180)
            v5 = v5.rotatedDegreesCW(72.0);
        }
        assertEqual(v(0.0, 1.0), v5);
    }

}
