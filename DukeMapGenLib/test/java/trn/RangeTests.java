package trn;

import org.junit.Assert;
import org.junit.Test;

public class RangeTests {

    @Test
    public void testEmptyRange(){
        Range r = new Range(42, 42);
        for(int i = 40; i < 44; ++i){
            Assert.assertFalse(r.contains(i));
        }

        Assert.assertFalse(r.containsAny(41, 42, 43));
    }

    @Test
    public void testRange(){
        Range r = new Range(0, 5);
        for(int i = 0; i < 5; ++i){
            Assert.assertTrue(r.contains(i));
        }
        Assert.assertFalse(r.contains(-1));
        Assert.assertFalse(r.contains(-2));
        Assert.assertFalse(r.contains(5));
        Assert.assertFalse(r.contains(6));

        Assert.assertFalse(r.containsAny(-1, -2, 5, 6));
        Assert.assertTrue(r.containsAny(-1, -2, 5, 6, 3));
    }

    @Test
    public void testNegativeRange(){
        Range r = new Range(-10, -4);
        for(int i = -10; i < -4; ++i){
            Assert.assertTrue(r.contains(i));
        }
        Assert.assertFalse(r.contains(-12));
        Assert.assertFalse(r.contains(-11));
        Assert.assertFalse(r.contains(-4));
        Assert.assertFalse(r.contains(-3));

        Assert.assertFalse(r.containsAny(-12, -11, -4, -3));
        Assert.assertTrue(r.containsAny(-1, 10000, -8));
    }

    @Test
    public void testInclusive(){
        Range r = Range.inclusive(10, 2);
        for(int i = 2; i < 11; ++i){
            Assert.assertTrue(r.contains(i));
        }
        Assert.assertFalse(r.contains(0));
        Assert.assertFalse(r.contains(1));
        Assert.assertFalse(r.contains(11));
        Assert.assertFalse(r.contains(12));

        Assert.assertFalse(r.containsAny(0, 1, 11, 12));
        Assert.assertTrue(r.containsAny(5));
    }

    @Test
    public void testNegativeInclusive(){
        Range r = Range.inclusive(-10, 2);
        for(int i = -10; i < 3; ++i){
            Assert.assertTrue(r.contains(i));
        }
        Assert.assertFalse(r.contains(-12));
        Assert.assertFalse(r.contains(-11));
        Assert.assertFalse(r.contains(3));
        Assert.assertFalse(r.contains(4));

        Assert.assertFalse(r.containsAny(-13, -12, -11, 3, 4, 5));
        Assert.assertTrue(r.containsAny(0));
    }

}
