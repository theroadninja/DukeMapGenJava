package trn.prefab;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;
import trn.AngleUtil;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/**
 * TODO - remove all references to AngleUtil so that I can move this (and PointXY, which depends on this) to a new
 * math-only package.
 */
public class Heading {
    // TODO DRY with Point3d.scala
    public static final int EAST = 0;
    public static final int SOUTH = 1;
    public static final int WEST = 2;
    public static final int NORTH = 3;

    public static final int E = EAST;
    public static final int S = SOUTH;
    public static final int W = WEST;
    public static final int N = NORTH;

    public static List<Integer> all = Arrays.asList(EAST, SOUTH, WEST, NORTH);

    // this map is protected by a unit test -- see AngleUtilScalaTests.scala
    private static Map<Integer, Integer> dukeAngleToHeading = new TreeMap<Integer, Integer>(){{
        put(AngleUtil.ANGLE_RIGHT, E);
        put(AngleUtil.ANGLE_DOWN, S);
        put(AngleUtil.ANGLE_LEFT, W);
        put(AngleUtil.ANGLE_UP, N);
    }};

    private static Map<Integer, Integer> opposites = new TreeMap<Integer, Integer>(){{
        put(E, W);
        put(W, E);
        put(N, S);
        put(S, N);
    }};

    /**
     * Convert the heading to a unit vector of Pair<dx, dy>.
     * Note that this matches the Build coordinate system, so N = <0, -1> and E = <1, 0>
     *
     * @return a unit vector pointing in the direction of this heading.
     *
     * Compare to maze/Heading.java
     */
    public static Pair<Integer, Integer> toUnitVector(int heading){
        if(heading == N){
            return new ImmutablePair<>(0, -1);
        }else if(heading == E){
            return new ImmutablePair<>(1, 0);
        }else if(heading == S){
            return new ImmutablePair<>(0, 1);
        }else if(heading == W){
            return new ImmutablePair<>(-1, 0);
        }else{
            throw new IllegalArgumentException();
        }
    }

    public static Integer fromDukeAngle(int dukeAng){
        return dukeAngleToHeading.get(dukeAng);
    }

    public static int opposite(int heading){
        return opposites.get(heading);
    }


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
