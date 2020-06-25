package trn;

import java.util.ArrayList;
import java.util.List;

/**
 * A read-only view of a wall that includes things a normal `Wall` object doesnt have, like wall id.
 *
 * If you want to know if a wall loop is the outer loop or not, see MapUtil
 *
 *
 * I believe walls are specified clockwise, because "point2" is "to the right" according to buildhlp.
 */
public class WallView {

    public static long totalLength(Iterable<WallView> walls){
        long sum = 0;
        for(WallView w: walls){
            sum += w.getLineSegment().getLength();
        }
        return sum;
    }

    protected final Wall wall;

    private final int wallId;

    private final LineSegmentXY wallSegment;

    public final PointXY startPoint(){
        return wall.getLocation();
    }

    public final PointXY getVector(){
        return wallSegment.getVector();
    }

    public final PointXY getUnitVector(){
        return wall.getUnitVector(p2());
    }

    public final LineSegmentXY getLineSegment(){
        return wallSegment;
    }

    public final double length(){
        return wallSegment.getLength();
    }

    public WallView(Wall wall, int wallId, LineSegmentXY wallSegement){
        this.wall = wall;
        this.wallId = wallId;
        this.wallSegment = wallSegement;
    }

    public final int getWallId(){
        return this.wallId;
    }

    public List<PointXY> points(){
        return getLineSegment().toList();
    }

    /**
     * Tests if this wall is "contiguous" with another wall, based on the point locations alone, meaning that they
     * share a point.  If strict==true, they must not only share a point but one wall must start where the other ends
     * (walls are directional, going from p1 to p2).
     *
     * Contiguous:
     *   +------>+------>
     *
     * Not Contiguous if strict == true:
     *   +------><-------+
     *   <------++------->
     *
     * @param other the other wall to compare to this
     * @param strict if true, only returns true if one wall starts where another wall ends.  if false, two walls
     *               that start at the same point count as contiguous
     * @return true if this wall shared a point with the other wall AND they are both going in the same direction.
     */
    public boolean contiguous(WallView other, boolean strict){
        if(strict){
            return this.p1().equals(other.p2()) || this.p2().equals(other.p1());
        }else{
            return this.p1().equals(other.p1()) || this.p1().equals(other.p2()) || this.p2().equals(other.p1()) || this.p2().equals(other.p2());
        }
    }

    public boolean contiguous(WallView other){
        return contiguous(other, true);
    }

    // @Override
    // public boolean equals(Object obj){
    //     if(!(obj instanceof WallView)){
    //         return false;
    //     }
    //     WallView other = (WallView)obj;
    //     return this.wallId == other.wallId;
    // }

    // @Override
    // public int hashCode(){
    //     return this.wallId;
    // }


    // ----------------------------------

    /** @return true if this wall is vertical, aligned to the y axis */
    public final boolean isAlignedY(){
        PointXY p1 = getLineSegment().getP1();
        PointXY p2 = getLineSegment().getP2();
        return p1.x == p2.x && p1.y != p2.y;
    }

    public final boolean isRedwall(){
        return wall.isRedWall();
    }

    /** @returns true if this is a red wall and 'other' is the other redwall (based on its pointer, not its location) */
    public final boolean isOtherSide(WallView other){
        if(wall.nextWall == -1 && other.wall.nextWall == -1){
            return false;
        }

        boolean matchL = wall.nextWall == other.getWallId();
        boolean matchR = other.wall.nextWall == getWallId();
        if(matchL == matchR){
            return matchL;
        }else{
            String msg = String.format("walls dont match wall %s nextWall=%s vs wall %s nextWall=%s",
                    getWallId(), wall.nextWall, other.getWallId(), other.wall.nextWall
            );
            throw new RuntimeException(msg);
        }
        //if(matchL && matchR){
        //    return true;
        //}else{
        //    String msg = String.format("walls dont match wall %s nextWall=%s vs wall %s nextWall=%s",
        //            getWallId(), wall.nextWall, other.getWallId(), other.wall.nextWall
        //    );
        //    throw new RuntimeException(msg);
        //}
    }

    public final int otherWallId(){
        return wall.nextWall;
    }

    public final int tex(){
        return wall.getTexture();
    }

    public final int xRepeat(){
        return wall.getXRepeat();
    }

    public final int xPan(){
        return wall.getXPanning();
    }

    public final int lotag(){
        return wall.getLotag();
    }

    public final PointXY p1(){
        return getLineSegment().getP1();
    }

    public final PointXY p2(){
        return getLineSegment().getP2();
    }
}
