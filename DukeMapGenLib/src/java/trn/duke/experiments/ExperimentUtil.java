package trn.duke.experiments;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import trn.Map;
import trn.Wall;

import java.util.ArrayList;
import java.util.List;
import java.util.Random;

public final class ExperimentUtil {
    // private static final Random random = new Random();

    // public static Random getRandom(){
    //     return random;
    // }


    /**
     *
     * @param map
     * @param sector0
     * @param sector1
     * @return wall indexes in each sector that match (by x,y coords) a wall in the other sector
     */
    public static Pair<List<Integer>, List<Integer>> filterOverlappingPoints(trn.Map map, int sector0, int sector1){

        List<Integer> sector0walls = map.getSectorWallIndexes(sector0);
        List<Integer> sector1walls = map.getSectorWallIndexes(sector1);

        List<Integer> sector0wallsOut = new ArrayList<Integer>(sector0walls.size());
        List<Integer> sector1wallsOut = new ArrayList<Integer>(sector1walls.size());


        for(int w0i : sector0walls){
            for(int w1i : sector1walls){

                if(map.getWall(w0i).sameXY(map.getWall(w1i))){
                    sector0wallsOut.add(w0i);
                    sector1wallsOut.add(w1i);
                }
            }
        }

        return new ImmutablePair<List<Integer>, List<Integer>>(sector0wallsOut, sector1wallsOut);
    }
    // /**
    //  * orders the walls so that wall 0 points to wall 1
    //  * @param map
    //  * @param walls
    //  */
    // public static void orderWalls(Map map, Integer ... walls){
    //     if(walls[0] == null || walls[1] == null) throw new RuntimeException();


    //     Wall w0 = map.getWall(walls[0]);
    //     Wall w1 = map.getWall(walls[1]);

    //     if(w0.getPoint2Id() == walls[1]){
    //         //ok
    //     }else if(w1.getPoint2Id() == walls[0]){
    //         Integer tmp = walls[0];
    //         walls[0] = walls[1];
    //         walls[1] = tmp;
    //     }else{
    //         throw new RuntimeException("walls are not adjacent");
    //     }
    // }

    /**
     * orders the walls so that wall 0 points to wall 1 -- list versino
     * @param map
     * @param walls
     */
    public static void orderWalls(Map map, List<Integer> walls){
        if(walls == null || walls.size() != 2) throw new IllegalArgumentException();


        Wall w0 = map.getWall(walls.get(0));
        Wall w1 = map.getWall(walls.get(1));

        if(w0.getPoint2Id() == walls.get(1)){
            //ok
        }else if(w1.getPoint2Id() == walls.get(0)){
            Integer tmp = walls.get(0);
            walls.set(0, walls.get(1));
            walls.set(1, tmp);
        }else{
            throw new RuntimeException("walls are not adjacent");
        }
    }
}
