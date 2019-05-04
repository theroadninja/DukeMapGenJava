package trn.prefab;

import org.junit.Assert;
import org.junit.Test;
import trn.Wall;
import trn.WallContainer;

import java.util.*;

class SimpleWallContainer implements WallContainer {

    final int maxWall = 10;

    //final Set<Integer> connectorWalls;


    // public SimpleWallContainer(Collection<Integer> markerWalls){
    //     //this.connectorWalls = new TreeSet<>(markerWalls);
    // }
    //
    // public SimpleWallContainer(Integer... ids){
    //     this(Arrays.asList(ids));
    // }

    @Override
    public Wall getWall(int i) {
        if(i > maxWall || i < 0) throw new IllegalArgumentException();
        Wall w = new Wall();
        w.setPoint2Id(i+1);
        return w;
    }
}

public class ConnectorFactoryTests {

    private List<Integer> flatten(List<List<Integer>> lists){
        List<Integer> results = new LinkedList<>();
        for(List<Integer> list : lists){
            results.addAll(list);
        }
        return results;
    }

    @Test
    public void testParitionWallsSimple(){
        SimpleWallContainer map = new SimpleWallContainer();
        List<List<Integer>> results = ConnectorFactory.partitionWalls(Arrays.asList(2, 5), map);
        Assert.assertEquals(2, results.size());
        Assert.assertEquals(1, results.get(0).size());
        Assert.assertEquals(1, results.get(1).size());
        Assert.assertTrue(flatten(results).contains(2));
        Assert.assertTrue(flatten(results).contains(5));
    }

    @Test
    public void testPartitionWalls(){
        SimpleWallContainer map = new SimpleWallContainer();
        List<List<Integer>> results = ConnectorFactory.partitionWalls(Arrays.asList(2, 3, 5), map);
        Assert.assertEquals(2, results.size());
        Assert.assertTrue(flatten(results).contains(2));
        Assert.assertTrue(flatten(results).contains(3));
        Assert.assertTrue(flatten(results).contains(5));

    }
}
