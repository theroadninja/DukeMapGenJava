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

    private final Wall wall;

    private final int wallId;

    private final LineSegmentXY wallSegment;

    public final PointXY startPoint(){
        return wall.getLocation();
    }

    public final PointXY getVector(){
        return wallSegment.getVector();
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

    /** @returns true if this is a red wall and 'other' is the other redwall */
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
