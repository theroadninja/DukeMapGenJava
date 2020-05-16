package trn.javax;

import org.junit.Assert;
import org.junit.Test;

import java.util.*;

public class MultiIteratorTests {

    @Test
    public void testEmpty(){
        MultiIterator<Integer> mi = new MultiIterator<>();
        Assert.assertFalse(mi.hasNext());
    }

    @Test(expected = NoSuchElementException.class)
    public void testEmptyThrows(){
        MultiIterator<Integer> mi = new MultiIterator<>();
        mi.next();
    }

    @Test(expected = NoSuchElementException.class)
    public void testOne(){
        List<Integer> list1 = Arrays.asList(new Integer[]{1,2,3});
        MultiIterator<Integer> mi = new MultiIterator<>(list1);
        Assert.assertTrue(mi.hasNext());
        Assert.assertEquals(Integer.valueOf(1), mi.next());
        Assert.assertTrue(mi.hasNext());
        Assert.assertEquals(Integer.valueOf(2), mi.next());
        Assert.assertTrue(mi.hasNext());
        Assert.assertEquals(Integer.valueOf(3), mi.next());
        Assert.assertFalse(mi.hasNext());
        mi.next();
    }

    @Test
    public void testTwo(){
        List<Integer> list1 = Arrays.asList(new Integer[]{5,6,7});
        List<Integer> list2 = Arrays.asList(new Integer[]{1,2,3});
        MultiIterator<Integer> mi = new MultiIterator<>(list1, list2);
        Assert.assertTrue(mi.hasNext());
        Assert.assertEquals(Integer.valueOf(5), mi.next());
        Assert.assertTrue(mi.hasNext());
        Assert.assertEquals(Integer.valueOf(6), mi.next());
        Assert.assertTrue(mi.hasNext());
        Assert.assertEquals(Integer.valueOf(7), mi.next());

        Assert.assertTrue(mi.hasNext());
        Assert.assertEquals(Integer.valueOf(1), mi.next());
        Assert.assertTrue(mi.hasNext());
        Assert.assertEquals(Integer.valueOf(2), mi.next());
        Assert.assertTrue(mi.hasNext());
        Assert.assertEquals(Integer.valueOf(3), mi.next());

        Assert.assertFalse(mi.hasNext());
    }

    @Test
    public void firstIsEmpty(){
        Collection<Integer> list1 = new HashMap<Integer, Integer>().values();
        List<Integer> list2 = Arrays.asList(new Integer[]{1,2,3});
        MultiIterator<Integer> mi = new MultiIterator<>(list1, list2);
        Assert.assertTrue(mi.hasNext());

    }
}
