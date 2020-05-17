package trn;

/**
 * A read-only view of a wall that includes things a normal `Wall` object doesnt have, like wall id.
 *
 * If you want to know if a wall loop is the outer loop or not, see MapUtil
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


    // ----------------------------------

    public final boolean isRedwall(){
        return wall.isRedWall();
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
}
