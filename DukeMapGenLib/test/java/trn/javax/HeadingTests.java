package trn.javax;

import org.junit.Assert;
import org.junit.Test;
import trn.prefab.Heading;

public class HeadingTests {

    @Test
    public void testRotate(){
        Assert.assertEquals(Heading.E, Heading.rotateCW(Heading.N));
        Assert.assertEquals(Heading.S, Heading.rotateCW(Heading.E));
        Assert.assertEquals(Heading.W, Heading.rotateCW(Heading.S));
        Assert.assertEquals(Heading.N, Heading.rotateCW(Heading.W));

        Assert.assertEquals(Heading.E, Heading.rotateCW(Heading.rotateCW(Heading.W)));
        Assert.assertEquals(Heading.W, Heading.rotateCW(Heading.rotateCW(Heading.E)));
        Assert.assertEquals(Heading.N, Heading.rotateCW(Heading.rotateCW(Heading.S)));
        Assert.assertEquals(Heading.S, Heading.rotateCW(Heading.rotateCW(Heading.N)));

        Assert.assertEquals(Heading.S, Heading.rotateCW(Heading.rotateCW(Heading.rotateCW(Heading.W))));
    }

    @Test
    public void testFlipX(){
        Assert.assertEquals(Heading.E, Heading.flipX(Heading.W));
        Assert.assertEquals(Heading.W, Heading.flipX(Heading.E));
        Assert.assertEquals(Heading.N, Heading.flipX(Heading.N));
        Assert.assertEquals(Heading.S, Heading.flipX(Heading.S));
    }

    @Test
    public void testFlipY(){
        Assert.assertEquals(Heading.N, Heading.flipY(Heading.S));
        Assert.assertEquals(Heading.S, Heading.flipY(Heading.N));
        Assert.assertEquals(Heading.E, Heading.flipY(Heading.E));
        Assert.assertEquals(Heading.W, Heading.flipY(Heading.W));
    }
}
