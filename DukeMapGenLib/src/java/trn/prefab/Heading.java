package trn.prefab;

import java.util.Arrays;
import java.util.List;

public class Heading {
    public static int EAST = 0;
    public static int SOUTH = 1;
    public static int WEST = 2;
    public static int NORTH = 3;

    public static int E = EAST;
    public static int S = SOUTH;
    public static int W = WEST;
    public static int N = NORTH;

    public static List<Integer> all = Arrays.asList(EAST, SOUTH, WEST, NORTH);

    public static int rotateCW(int heading){
        if(heading < 0 || heading > 3) throw new IllegalArgumentException();
        return (heading + 1) % 4;
    }

    public static int flipX(int heading){
        if(heading < 0 || heading > 3) throw new IllegalArgumentException();
        if(heading == E){
            return W;
        }else if(heading == W){
            return E;
        }else{
            return heading;
        }
    }

    public static int flipY(int heading){
        if(heading < 0 || heading > 3) throw new IllegalArgumentException();
        if(heading == S){
            return N;
        }else if(heading == N){
            return S;
        }else{
            return heading;
        }
    }
}
