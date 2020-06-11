package trn.prefab;

import trn.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * what uses this?  hyper4.map
 */
public class MultiWallConnector extends RedwallConnector {

    //private final List<WallView> walls;

    /**
     * The points of the walls relative to the first point, which we use to store the shape of the connector,
     * to verify whether it can fit to another connector.
     */
    private final List<PointXY> relativePoints;

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
        super(connectorId, sectorId, SimpleConnector.toList(sectorId), totalLength, anchor, wallAnchor1, wallAnchor2, markerSpriteLotag, ConnectorType.MULTI_REDWALL, wallIds, 1);
        //this.walls = Collections.unmodifiableList(walls);
        this.relativePoints = relativePoints;
    }

    public MultiWallConnector(Sprite markerSprite, Sector sector, List<Integer> wallIds, List<WallView> walls, Map map){
        super(markerSprite, markerSprite.getSectorId(),
                SimpleConnector.toList(markerSprite.getSectorId()),
                totalManhattanLength(wallIds, map),
                getAnchor(wallIds, map).withZ(sector.getFloorZ()),
                map.getWall(wallIds.get(0)).getLocation(),
                map.getWall(
                        // TODO dry with endWallId below
                        map.getWall(wallIds.get(wallIds.size() - 1)).getNextWallInLoop()
                ).getLocation(),
                markerSprite.getLotag(),
                ConnectorType.MULTI_REDWALL,
                wallIds,
                1
                );
        if(markerSprite == null || sector == null) throw new IllegalArgumentException();

        int endWallId = map.getWall(wallIds.get(wallIds.size() - 1)).getNextWallInLoop();
        if(endWallId == wallIds.get(0)){
            throw new IllegalArgumentException("walls are a complete loop");
        }
        //this.walls = walls;
        this.relativePoints = allRelativeConnPoints(wallIds, map, this.anchor, this.wallAnchor2);
    }

    public static PointXY getAnchor(List<Integer> wallIds, Map map){
        // TODO - duplicate anchor logic in ConnectorScanner
        int minX = map.getWall(wallIds.get(0)).getX();
        int minY = map.getWall(wallIds.get(0)).getY();

        // make sure the walls are actually in a loop
        for(int i = 0; i < wallIds.size() - 1; ++i){
            int wallId = wallIds.get(i);
            int nextWallId = wallIds.get(i + 1);
            Wall w = map.getWall(wallId);
            if(w.getNextWallInLoop() != nextWallId) throw new IllegalArgumentException("walls are not in a sequential loop");
            if(w.getX() < minX){
                minX = w.getX();
            }
            if(w.getY() < minY){
                minY = w.getY();
            }
        }

        // the wall after the last wall in the sequence, because we need its X,Y coords
        int endWallId = map.getWall(wallIds.get(wallIds.size() - 1)).getNextWallInLoop();
        if(endWallId == wallIds.get(0)){
            throw new IllegalArgumentException("walls are a complete loop");
        }
        Wall end = map.getWall(endWallId);
        if(end.getX() < minX){
            minX = end.getX();
        }
        if(end.getY() < minY){
            minY = end.getY();
        }
        return new PointXY(minX, minY);
    }

    @Override
    public MultiWallConnector translateIds(IdMap idmap, PointXYZ delta, Map map) {
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

    static long totalManhattanLength(List<Integer> wallIds, Map map){
        long sum = 0;
        for(int wallId: wallIds){
            Wall w1 = map.getWall(wallId);
            Wall w2 = map.getWall(w1.getNextWallInLoop());
            sum += w1.getLocation().manhattanDistanceTo(w2.getLocation());
        }
        return sum;
    }

    @Override
    public boolean isMatch(RedwallConnector c){
        if(!(c instanceof MultiWallConnector)){
            return false;
        }
        MultiWallConnector other = (MultiWallConnector)c;

        // TODO - this has code duplicated from canLink()
        if(this.wallIds.size() != other.wallIds.size()){
            return false;
        }
        PointXY p1 = this.wallAnchor1.subtractedBy(this.anchor);
        PointXY p2 = this.wallAnchor2.subtractedBy(this.anchor);
        PointXY otherP1 = other.wallAnchor1.subtractedBy(other.anchor);
        PointXY otherP2 = other.wallAnchor2.subtractedBy(other.anchor);
        if(! p1.equals(otherP2)) return false;
        if(! p2.equals(otherP1)) return false;

        List<PointXY> list1 = this.relativePoints;
        List<PointXY> list2 = other.relativePoints;
        if(! list1.get(0).equals(p1)) throw new RuntimeException();
        if(! list2.get(0).equals(otherP1)) throw new RuntimeException();
        if(list1.size() != list2.size()) throw new RuntimeException();

        for(int i = 0; i < list1.size(); ++i){
            int j = list2.size() - 1 - i;
            if(! list1.get(i).equals(list2.get(j))){
                // System.out.println("no match: " + list1.get(i).toString() + "," + list2.get(i).toString());
                // System.out.println("i=" + i + " j=" + j);
                return false;
            }
        }

        return true;
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


        // System.out.println("p1=" + this.wallAnchor1 + ", anchor=" + this.anchor);
        // System.out.println("p2=" + this.wallAnchor2);
        // System.out.println("-----");
        // System.out.println("p1=" + p1.toString());
        // System.out.println("p2=" + p2.toString());
        // System.out.println("otherP1=" + otherP1.toString());
        // System.out.println("otherP2=" + otherP2.toString());

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
            // List<PointXY> list1 = allRelativeConnPoints(map);
            List<PointXY> list1 = this.relativePoints;
            // List<PointXY> list2 = other.allRelativeConnPoints(map);
            List<PointXY> list2 = other.relativePoints;
            // System.out.println("list1");
            // for(PointXY p : list1){
            //     System.out.print(p.toString() + ", ");
            // }
            // System.out.println("anchor=" + this.anchor);
            // System.out.println("list2");
            // for(PointXY p : list2){
            //     System.out.print(p.toString() + ", ");
            // }
            // System.out.println("anchor=" + other.anchor);
            if(! list1.get(0).equals(p1)) throw new RuntimeException();
            if(! list2.get(0).equals(otherP1)) throw new RuntimeException();
            if(list1.size() != list2.size()) throw new RuntimeException();

            for(int i = 0; i < list1.size(); ++i){
                int j = list2.size() - 1 - i;
                if(! list1.get(i).equals(list2.get(j))){
                    // System.out.println("no match: " + list1.get(i).toString() + "," + list2.get(i).toString());
                    // System.out.println("i=" + i + " j=" + j);
                    return false;
                }
            }
        }
        if(! p1.equals(otherP2)) return false;
        if(! p2.equals(otherP1)) return false;
        return true;
    }

    static List<PointXY> allRelativeConnPoints(List<Integer> wallIds, Map map, PointXYZ anchor, PointXY anchor2){
        List<PointXY> results = new ArrayList<>(wallIds.size() + 1);
        for(Integer i: wallIds){
            results.add(map.getWall(i).getLocation().subtractedBy(anchor));
        }
        results.add(anchor2.subtractedBy(anchor));
        return results;
    }

    @Override
    public void linkConnectors(Map map, RedwallConnector otherConn) {
        if(map == null || otherConn == null) throw new IllegalArgumentException("argument is null");
        if(otherConn.getConnectorType() != this.getConnectorType()) throw new IllegalArgumentException("connector type mismatch");
        MultiWallConnector c2 = (MultiWallConnector)otherConn;
        if(! canLink(c2, map)) throw new SpriteLogicException("cannot link connector (other=" + c2.getConnectorId() + ")");

        // i think we just link up the walls in reverse order
        for(int i = 0; i < this.wallIds.size(); ++i){
            int j = this.wallIds.size() - 1 - i;
            map.linkRedWalls(this.getSectorId(), this.wallIds.get(i), c2.getSectorId(), c2.wallIds.get(j));
        }
    }
}
