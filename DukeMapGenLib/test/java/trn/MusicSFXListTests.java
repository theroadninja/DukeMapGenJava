package trn;

import org.junit.Assert;
import org.junit.Test;
import trn.duke.MusicSFXList;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class MusicSFXListTests {

    private static boolean noOverlap(List<Integer> list1, List<Integer> list2){
        Set<Integer> set1 = new HashSet<>(list1);
        Set<Integer> set2 = new HashSet<>(list2);

        for(Integer i: set1){
            if(set2.contains(i)) return false;
        }
        for(Integer i: set2){
            if(set1.contains(i)) return false;
        }
        return true;
    }

    @Test
    public void testNoOverlap(){
        List<List<Integer>> all = MusicSFXList.ALL_LISTS;
        for(int i = 0; i < all.size(); ++i){
            for(int j = 0; j < all.size(); ++j){
                if(i != j){
                    Assert.assertTrue(noOverlap(all.get(i), all.get(j)));
                }
            }
        }
    }

    @Test
    public void testTotalCount(){
        // the total number of defined sounds I could find in the original duke3d game (no expansion)
        Assert.assertEquals(276, MusicSFXList.ALL.size());
    }
}
