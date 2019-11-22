package trn.prefab;

import trn.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class MultiWallConnector extends RedwallConnector {

    // dont put this in RedwallConnector because some redwall connectors might have multiple sectors
    private final int sectorId;
    private final List<Integer> wallIds;


    /**
     * a point with x = min x of all wall points and y = min y of all wall points
     */
    private final PointXYZ anchor;

    /**
     * the anchor at the beginning of the sequence of walls (with a point that matches the first wall in
     * the sequence).
     */
    private final PointXY wallAnchor1;

    /**
     * the anchor at the end of the sequence of walls (with a point matching the wall that the last wall
     * of the sequence points to).
     */
    private final PointXY wallAnchor2;

    private final int markerSpriteLotag;

    /**
     * The points of the walls relative to the first point, which we use to store the shape of the connector,
     * to verify whether it can fit to another connector.
     */
    private final List<PointXY> relativePoints;

    private MultiWallConnector(int connectorId, int sectorId, List<Integer> wallIds, PointXYZ anchor, PointXY wallAnchor1, PointXY wallAnchor2, int markerSpriteLotag, List<PointXY> relativePoints){
        super(connectorId);
        this.sectorId = sectorId;
        this.wallIds = Collections.unmodifiableList(new ArrayList<>(wallIds));
        if(this.wallIds.size() < 2) throw new IllegalArgumentException();
        this.anchor = anchor;
        this.wallAnchor1 = wallAnchor1;
        this.wallAnchor2 = wallAnchor2;
        this.markerSpriteLotag = markerSpriteLotag;
        this.relativePoints = relativePoints;
    }

    public MultiWallConnector(Sprite markerSprite, Sector sector, List<Integer> wallIds, Map map){
        super(markerSprite);
        this.markerSpriteLotag = markerSprite.getLotag();
        if(markerSprite == null || sector == null) throw new IllegalArgumentException();
        // if(wallIds == null || wallIds.size() < 2) throw new IllegalArgumentException();
        this.sectorId = markerSprite.getSectorId();
        //this.wallIds = new ArrayList<>(wallIds.size());
        wallIds = MapUtil.sortWallSection(wallIds, map);

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

        int z = sector.getFloorZ();
        this.anchor = new PointXYZ(minX, minY, z);
        this.wallAnchor1 = map.getWall(wallIds.get(0)).getLocation();
        this.wallAnchor2 = map.getWall(endWallId).getLocation();

        this.wallIds = Collections.unmodifiableList(wallIds);
        if(this.wallIds.size() < 1) throw new RuntimeException();
        this.relativePoints = allRelativeConnPoints(this.wallIds, map, this.anchor, this.wallAnchor2);
    }


    @Override
    public PointXYZ getTransformTo(RedwallConnector other) {
        if(other == null) throw new IllegalArgumentException();
        MultiWallConnector c2 = (MultiWallConnector)other;
        if(! canLink(c2, null)){
            throw new SpriteLogicException("cannot link to other connector");
        }

        // if the sectors are facing each other, the anchor sides at opposite ends of the wall
        // sequence should match, because walls always go clockwise.


        return this.getAnchorPoint().getTransformTo(c2.getAnchorPoint());
    }

    @Override
    public MultiWallConnector translateIds(IdMap idmap, PointXYZ delta) {
        return new MultiWallConnector(
                this.getConnectorId(),
                idmap.sector(this.sectorId),
                idmap.wallIds(this.wallIds),
                this.anchor.add(delta),
                this.wallAnchor1.add(delta.asXY()),
                this.wallAnchor2.add(delta.asXY()),
                this.markerSpriteLotag,
                this.relativePoints
        );
    }

    @Override
    public PointXYZ getAnchorPoint() {
        return this.anchor;
    }

    @Override
    public short getSectorId() {
        return (short)sectorId;
    }

    @Override
    public boolean isLinked(Map map) {
        // return map.getWall(wallId).isRedWall();
        // return true if all the walls are red walls
        Boolean linked = null;
        assert this.wallIds.size() > 0;
        for(int wallId: this.wallIds){
            boolean b = map.getWall(wallId).isRedWall();
            if(linked == null){
                linked = b;
            }else if(linked != b){
                //some are linked and some are not
                throw new SpriteLogicException("redwall connector inconsistent linkage");
            }
        }
        return linked;
    }

    @Override
    public int getConnectorType() {
        return ConnectorType.MULTI_REDWALL;
    }

    @Override
    public long totalManhattanLength(Map map){
        long sum = 0;
        for(int wallId: this.wallIds){
            Wall w1 = map.getWall(wallId);
            Wall w2 = map.getWall(w1.getNextWallInLoop());
            sum += w1.getLocation().manhattanDistanceTo(w2.getLocation());
        }
        return sum;
    }

    @Override
    public void removeConnector(Map map) {
        //TODO - merge this with the one in SimpleConnector

        // clear the wall
        for(int wallId: this.wallIds){
            Wall w = map.getWall(wallId);
            if(w.getLotag() != 1) throw new SpriteLogicException();
            w.setLotag(0);
        }

        // remove the marker sprite
        int d = map.deleteSprites((Sprite s) ->
                s.getTexture() == PrefabUtils.MARKER_SPRITE_TEX
                        && s.getSectorId() == sectorId
                        && s.getLotag() == this.markerSpriteLotag
        );
        if(d != 1) throw new SpriteLogicException();
    }

    @Override
    public boolean isMatch(RedwallConnector c){
        System.out.println("MONKEY 0");
        if(!(c instanceof MultiWallConnector)){
            return false;
        }
        System.out.println("MONKEY 1");
        MultiWallConnector other = (MultiWallConnector)c;

        // TODO - this has code duplicated from canLink()
        if(this.wallIds.size() != other.wallIds.size()){
            return false;
        }
        System.out.println("MONKEY");
        PointXY p1 = this.wallAnchor1.subtractedBy(this.anchor);
        PointXY p2 = this.wallAnchor2.subtractedBy(this.anchor);
        PointXY otherP1 = other.wallAnchor1.subtractedBy(other.anchor);
        PointXY otherP2 = other.wallAnchor2.subtractedBy(other.anchor);
        if(! p1.equals(otherP2)) return false;
        if(! p2.equals(otherP1)) return false;
        System.out.println("MADE IT HERE");

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
     * @param other
     * @param map  the map containing both connectors (pass null if the connectors are not in the same map yet)
     * @return
     */
    public boolean canLink(MultiWallConnector other, Map map) {
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


    /**
     * @return an XY point for each wall, AND the xy point where the last wall terminates.
     */
    private List<PointXY> allRelativeConnPoints(Map map){
        return allRelativeConnPoints(this.wallIds, map, this.anchor, this.wallAnchor2);
        // List<PointXY> results = new ArrayList<>(this.wallIds.size() + 1);
        // for(Integer i: wallIds){
        //     results.add(map.getWall(i).getLocation().subtractedBy(this.anchor));
        // }
        // results.add(this.wallAnchor2.subtractedBy(this.anchor));
        // return results;
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
