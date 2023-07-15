package trn.prefab;

import trn.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO - implement this using a `2` instead of a `1` for the wall lotag
public class MultiSectorConnector {

    public static int WALL_LOTAG = 2;

    /**
     * Calculates the `z` value of the anchor.  With other connectors this is the z of the sector with the marker,
     * however the marker of a MultiSectorConnector can be any sector, and the sector containing the marker is not
     * special, so instead we take the lowest floor, which is actually the "max" because z is upside down in Build.
     */
    public static int getAnchorZ(List<Integer> sectorIds, MapView map){
        if(sectorIds.size() < 1){
            throw new IllegalArgumentException();
        }
        int maxz = map.getSector(sectorIds.get(0)).getFloorZ();
        for(int sectorId: sectorIds){
            maxz = Math.max(maxz, map.getSector(sectorId).getFloorZ());
        }
        return maxz;
    }

    // calculate all wall positions relative to the anchor
    public static List<PointXY> getRelativeConnPoints(List<WallView> walls, PointXYZ anchor){
        List<PointXY> results = new ArrayList<>(walls.size() + 1);
        for(WallView wall: walls){
            results.add(wall.p1().subtractedBy(anchor));
        }
        results.add(walls.get(walls.size() - 1).p2().subtractedBy(anchor));
        return results;
    }

    public static RedwallConnector create(
            Sprite marker,
            List<Integer> sectorIds, // sorted and match wallIds and walls
            List<Integer> wallIds, // sorted
            List<WallView> walls, // sorted
            PointXY anchorXY,
            PointXY wallAnchor1,
            PointXY wallAnchor2,
            MapView map

    ){
        PointXYZ anchor = anchorXY.withZ(getAnchorZ(sectorIds, map));
        return new RedwallConnector(
                marker.getHiTag() > 0 ? marker.getHiTag() : -1,
                marker.getSectorId(),
                sectorIds,
                WallView.totalLength(walls),
                anchor,
                wallAnchor1,
                wallAnchor2,
                marker.getLotag(),
                ConnectorType.MULTI_SECTOR,
                wallIds,
                walls,
                WALL_LOTAG,
                Collections.unmodifiableList(getRelativeConnPoints(walls, anchor))
        );
    }

}
