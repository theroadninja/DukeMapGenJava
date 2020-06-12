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

    //private final List<PointXY> relativeConnPoints;


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
        super(marker.getHiTag(), marker.getSectorId(), sectorIds, WallView.totalLength(walls),
                anchor, wallAnchor1, wallAnchor2, marker.getLotag(), ConnectorType.MULTI_SECTOR, wallIds, walls, 2,
                Collections.unmodifiableList(getRelativeConnPoints(walls, anchor))
                );
        //this.relativeConnPoints = Collections.unmodifiableList(getRelativeConnPoints(walls, this.anchor));
    }

    @Override
    public boolean canLink(RedwallConnector other, Map map) {
        throw new RuntimeException("not implemented yet");
    }

    @Override
    public RedwallConnector translateIds(IdMap idmap, PointXYZ delta, Map map) {
        throw new RuntimeException("Not implemented yet");
    }
}
