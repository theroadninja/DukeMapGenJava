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

    /** @return true if this wall is vertical, aligned to the y axis */
    public final boolean isAlignedY(){
        PointXY p1 = getLineSegment().getP1();
        PointXY p2 = getLineSegment().getP2();
        return p1.x == p2.x && p1.y != p2.y;
    }

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
