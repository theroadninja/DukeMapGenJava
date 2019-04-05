package trn;


/**
 * This is a utility class for dealing with the build engines integer-based angles.
 *
 * Angles have the range [0, 2048)  where 0 points to the right and it goes clockwise around (512 is down).
 *
 * So PI = 1024.
 *
 * Apparently BUILD used a lookup table of ints where -1 = -16383 and 1 = 16383 (15 bits, including sign?)
 */
public class AngleUtil {


    /*
     * Angle, in weird duke angle units(pi = 1024), of a sprite that is facing "up" or "north" or towards -y
     * Using "up" in the name because I think the build documentation uses "up" and "down" when talking
     * about the angles of certain SE sprites.
     *
     * Fun fact:  the reason these angles are weird like this is because the build engine used a table
     * of precalculated SIN and COS values.
     *  */
    public static final int ANGLE_UP = 1536;

    public static final int ANGLE_RIGHT = 0;

    public static final int ANGLE_DOWN = 512;

    public static final int ANGLE_LEFT = 1024;

    // TODO - build a real table of ints?
    //java.util.Math.pi

    public static final int RANGE = 16383;

    public static double toRadians(int ang){
        return (Math.PI/1024.0) * (double)ang;
    }
    public static int fromRadians(double radians){
        return (int)(radians * 1024/Math.PI);
    }

    public static int angleOf(PointXY vector){
        // TODO - can i made this work with non-unit vectors?
        // normal version: double radians = Math.atan2(vector.y, vector.x);
        double radians = Math.atan2(-vector.y, vector.x); // -y because build has y+ going down
        int ang = fromRadians(radians);
        if(ang < 0){
            ang += 2048; // atan2 returns values in [-PI, PI]
        }
        return ang;
    }

    /**
     *
     * @param ang in duke-radians ( pi=1024 )
     * @return
     */
    public static PointXY unitVector(int ang){
        while(ang < 0){
            ang += 2048;
        }
        ang = ang % 2048;
        // this is a normal unit vector (in a world where Y+ is up)
        // return new PointXY(
        //         (int)(Math.cos(toRadians(ang)) * RANGE),
        //         (int)(Math.sin(toRadians(ang)) * RANGE)
        // );

        // this is a unit vector in a world where Y+ is down
        return new PointXY(
                (int)(Math.cos(toRadians(ang)) * RANGE),
                -(int)(Math.sin(toRadians(ang)) * RANGE)
        );

    }
}