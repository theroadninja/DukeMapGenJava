package trn.prefab;

import trn.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * what uses this?  hyper4.map
 */
public class MultiWallConnector extends RedwallConnector {

    private MultiWallConnector(
            int connectorId,
            int sectorId,
            List<Integer> wallIds,
            List<WallView> walls,
            PointXYZ anchor,
            PointXY wallAnchor1,
            PointXY wallAnchor2,
            int markerSpriteLotag,
            List<PointXY> relativePoints,
            long totalLength
    ){
        super(connectorId, sectorId, RedConnUtil.toList(sectorId), totalLength, anchor,
                wallAnchor1, wallAnchor2, markerSpriteLotag, ConnectorType.MULTI_REDWALL, wallIds, walls, 1, relativePoints);
    }

    public MultiWallConnector(Sprite markerSprite, Sector sector, List<Integer> wallIds, List<WallView> walls, Map map){
        super(markerSprite, markerSprite.getSectorId(),
                RedConnUtil.toList(markerSprite.getSectorId()),
                RedConnUtil.totalManhattanLength(wallIds, map),
                RedConnUtil.getAnchor(wallIds, map).withZ(sector.getFloorZ()),
                map.getWall(wallIds.get(0)).getLocation(),
                map.getWall(
                        // TODO dry with endWallId below
                        map.getWall(wallIds.get(wallIds.size() - 1)).getNextWallInLoop()
                ).getLocation(),
                markerSprite.getLotag(),
                ConnectorType.MULTI_REDWALL,
                wallIds,
                walls,
                1,

                // TODO - duplicate calls to getAnchor!
                RedConnUtil.allRelativeConnPoints(wallIds, map, RedConnUtil.getAnchor(wallIds, map).withZ(sector.getFloorZ()),
                    map.getWall(
                        // TODO dry with endWallId below
                        map.getWall(wallIds.get(wallIds.size() - 1)).getNextWallInLoop()
                ).getLocation() )
                );
        if(markerSprite == null || sector == null) throw new IllegalArgumentException();

        int endWallId = map.getWall(wallIds.get(wallIds.size() - 1)).getNextWallInLoop();
        if(endWallId == wallIds.get(0)){
            throw new IllegalArgumentException("walls are a complete loop");
        }
        //this.walls = walls;
        //this.relativePoints = allRelativeConnPoints(wallIds, map, this.anchor, this.wallAnchor2);
    }

    @Override
    public MultiWallConnector translateIds(final IdMap idmap, PointXYZ delta, Map map) {
        List<Integer> newWallIds = idmap.wallIds(this.wallIds);
        List<WallView> newWalls = MapUtil.getWallViews(newWallIds, map);
        return new MultiWallConnector(
                this.getConnectorId(),
                idmap.sector(spriteSectorId),
                newWallIds,
                newWalls,
                this.anchor.add(delta),
                this.wallAnchor1.add(delta.asXY()),
                this.wallAnchor2.add(delta.asXY()),
                this.markerSpriteLotag,
                this.relativePoints,
                this.totalLength
        );
    }

    /**
     *
     * @param other0
     * @param map  the map containing both connectors (pass null if the connectors are not in the same map yet)
     * @return
     */
    @Override
    public boolean canLink(RedwallConnector other0, Map map) {
        MultiWallConnector other = (MultiWallConnector)other0;
        if(this.wallIds.size() != other.wallIds.size()){
            return false;
        }
        PointXY p1 = this.wallAnchor1.subtractedBy(this.anchor);
        PointXY p2 = this.wallAnchor2.subtractedBy(this.anchor);

        PointXY otherP1 = other.wallAnchor1.subtractedBy(other.anchor);
        PointXY otherP2 = other.wallAnchor2.subtractedBy(other.anchor);

        // check each point
        if(map != null){

            // 1. are any of the walls redwalls?
            List<Integer> tmp = new ArrayList<>(this.wallIds);
            tmp.addAll(other.wallIds);
            for(int id: tmp){
                if(map.getWall(id).isRedWall()){
                    throw new SpriteLogicException("wall " + id + " is already a red wall");
                }
            }

            // 2. are the walls lined up correclty?
            List<PointXY> list1 = this.relativePoints;
            List<PointXY> list2 = other.relativePoints;
            if(! list1.get(0).equals(p1)) throw new RuntimeException();
            if(! list2.get(0).equals(otherP1)) throw new RuntimeException();
            if(list1.size() != list2.size()) throw new RuntimeException();

            for(int i = 0; i < list1.size(); ++i){
                int j = list2.size() - 1 - i;
                if(! list1.get(i).equals(list2.get(j))){
                    return false;
                }
            }
        }
        if(! p1.equals(otherP2)) return false;
        if(! p2.equals(otherP1)) return false;
        return true;
    }
}
