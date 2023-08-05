package trn.prefab;

import trn.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

// TODO - implement this using a `2` instead of a `1` for the wall lotag
public class MultiSectorConnector {

    // /**
    //  * Calculates the `z` value of the anchor.  With other connectors this is the z of the sector with the marker,
    //  * however the marker of a MultiSectorConnector can be any sector, and the sector containing the marker is not
    //  * special, so instead we take the lowest floor, which is actually the "max" because z is upside down in Build.
    //  */
    // public static int getAnchorZ(List<Integer> sectorIds, MapView map){
    //     if(sectorIds.size() < 1){
    //         throw new IllegalArgumentException();
    //     }
    //     int maxz = map.getSector(sectorIds.get(0)).getFloorZ();
    //     for(int sectorId: sectorIds){
    //         maxz = Math.max(maxz, map.getSector(sectorId).getFloorZ());
    //     }
    //     return maxz;
    // }

    // // calculate all wall positions relative to the anchor
    // public static List<PointXY> getRelativeConnPoints(List<WallView> walls, PointXYZ anchor){
    //     List<PointXY> results = new ArrayList<>(walls.size() + 1);
    //     for(WallView wall: walls){
    //         results.add(wall.p1().subtractedBy(anchor));
    //     }
    //     results.add(walls.get(walls.size() - 1).p2().subtractedBy(anchor));
    //     return results;
    // }

    // public static RedwallConnector create(
    //         int connectorId,
    //         int sectorId,
    //         List<Integer> sectorIds, // sorted and match wallIds and walls
    //         long totalWallLength,
    //         PointXYZ anchor,
    //         PointXY wallAnchor1,
    //         PointXY wallAnchor2,
    //         int markerSpriteLotag,
    //         int connectorType,
    //         List<Integer> wallIds, // sorted
    //         List<WallView> walls, // sorted
    //         int wallLotag,
    //         List<PointXY> relativePoints
    // ){
    //     return new RedwallConnector(
    //             connectorId,
    //             sectorId,
    //             sectorIds,
    //             totalWallLength,
    //             anchor,
    //             wallAnchor1,
    //             wallAnchor2,
    //             markerSpriteLotag,
    //             connectorType,
    //             wallIds,
    //             walls,
    //             wallLotag,
    //             relativePoints // Collections.unmodifiableList(getRelativeConnPoints(walls, anchor))
    //     );
    // }

}
