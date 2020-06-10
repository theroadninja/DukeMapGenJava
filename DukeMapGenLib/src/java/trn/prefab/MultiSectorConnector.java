package trn.prefab;

import trn.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO - implement this using a `2` instead of a `1` for the wall lotag
public class MultiSectorConnector extends RedwallConnector {

    // calculate all wall positions relative to the anchor
    public static List<PointXY> getRelativeConnPoints(List<WallView> walls, PointXYZ anchor){
        List<PointXY> results = new ArrayList<>(walls.size() + 1);
        for(WallView wall: walls){
            results.add(wall.p1().subtractedBy(anchor));
        }
        results.add(walls.get(walls.size() - 1).p2().subtractedBy(anchor));
        return results;
    }

    private final List<Integer> sectorIds;
    private final int markerSectorId;
    private final List<Integer> wallIds;
    private final List<WallView> walls;
    private final PointXYZ anchor; // this is a PointXYZ in MultiWallConnector
    private final PointXY wallAnchor1;
    private final PointXY wallAnchor2;
    private final long totalManhattanLength;
    private final List<PointXY> relativeConnPoints;


    /**
     * @param sectorIds all sectors containing walls in this connector, in any order
     * @param wallIds  must be in order (e.g. wall(p0 -> p1) -> wall(p1 -> p2) -> ...)
     */
    public MultiSectorConnector(
            Sprite marker,
            List<Integer> sectorIds,
            List<Integer> wallIds,
            List<WallView> walls,
            PointXYZ anchor,
            PointXY wallAnchor1,
            PointXY wallAnchor2
    ) {
        super(marker.getHiTag());
        this.markerSectorId = marker.getSectorId();
        this.sectorIds = Collections.unmodifiableList(sectorIds);
        this.wallIds = Collections.unmodifiableList(wallIds);
        this.walls = Collections.unmodifiableList(walls);
        this.anchor = anchor;
        this.wallAnchor1 = wallAnchor1;
        this.wallAnchor2 = wallAnchor2;
        this.totalManhattanLength = WallView.totalLength(walls);
        this.relativeConnPoints = Collections.unmodifiableList(getRelativeConnPoints(walls, this.anchor));
    }

    @Override
    public short getSectorId() {
        return (short)this.markerSectorId;
    }

    @Override
    public List<Integer> getSectorIds() {
        return this.sectorIds;
    }

    @Override
    public long totalManhattanLength() {
        return this.totalManhattanLength;
    }

    @Override
    public PointXYZ getAnchorPoint() {
        return this.anchor;
    }

    @Override
    public int getConnectorType() {
        return ConnectorType.MULTI_SECTOR;
    }

    @Override
    public boolean isLinked(Map map) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public PointXYZ getTransformTo(RedwallConnector c2) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public RedwallConnector translateIds(IdMap idmap, PointXYZ delta) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public boolean isMatch(RedwallConnector c) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void removeConnector(Map map) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void linkConnectors(Map map, RedwallConnector otherConn) {
        throw new RuntimeException("Not implemented yet");
    }
}
