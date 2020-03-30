package trn;

/**
 * A read-only view of a wall.
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

    public WallView(Wall wall, int wallId, LineSegmentXY wallSegement){
        this.wall = wall;
        this.wallId = wallId;
        this.wallSegment = wallSegement;
    }
}
