package trn.prefab;

import duchy.sg.SimpleConnectorScanner$;
import trn.*;
import trn.duke.MapErrorException;

import java.util.*;
import java.util.Map;

/**
 * Temporary class I am creating in order to migrate code from ConnectorFactory.java to SimpleConnectorScanner.scala
 *
 * NOTE:  the original ConnectorFactory has been deleted.
 *
 * I don't want circular references going back and forth between the two classes, so I am moving the lower level methods
 * here first.
 *
 * THE NEWEST VERSION OF THIS CODE IS:  RedwallConnectorScanner
 */
public class ConnectorFactory2 {


    // TODO - compare to MapUtil.sortWallSection()
    // TODO - compare to ConnectorScanner.sortContinuousWalls() which should become the canonical form of this algorithm
    static List<List<Integer>> partitionWalls(List<Integer> wallIds, WallContainer map){
        //System.out.print("parition input: " );
        //for(int i = 0; i < wallIds.size(); ++i){
        //	System.out.print("" + wallIds.get(i) + ", ");
        //}
        //System.out.println();

        // TODO - this is a classic topological, so optimize this enough to not be embarassing
        if(wallIds == null) throw new IllegalArgumentException();
        java.util.Map<Integer, Integer> walls = new TreeMap<>();
        java.util.Map<Integer, Integer> wallsReversed = new TreeMap<>();
        for(Integer wallId : wallIds) {
            Wall w = map.getWall(wallId);
            if (walls.containsKey(wallId)) {
                throw new IllegalArgumentException("list of wall ids contains duplicate");
            } else {
                walls.put(wallId, w.getPoint2Id());
                wallsReversed.put(w.getPoint2Id(), wallId);
            }
        }
        Set<Integer> openList = new TreeSet<>(wallIds);

        List<List<Integer>> results = new ArrayList<>(wallIds.size());
        while(!openList.isEmpty()){
            LinkedList<Integer> segment = new LinkedList<>();
            Iterator<Integer> it = openList.iterator();
            segment.add(it.next());
            it.remove();
            while(wallsReversed.containsKey(segment.getFirst())){
                segment.addFirst(wallsReversed.get(segment.getFirst()));
                openList.remove(segment.getFirst());
            }
            while(walls.containsKey(segment.getLast())){
                segment.addLast(walls.get(segment.getLast()));
                openList.remove(segment.getLast());
            }
            segment.removeLast();
            results.add(segment);
        }

        return results;
    }

    // TODO I think this function can suffer from an infinite loop
    public static RedwallConnector redWallConn(Sprite s, Sector sector, List<Integer> wallIds, MapView map) throws MapErrorException {
        List<Integer> wallIds2 = MapUtil.sortWallSection(wallIds, map);
        List<WallView> walls = MapUtil.getWallViews(wallIds2, map);
        return RedwallConnector.create(s, sector, wallIds, walls, map);
    }

    /** returns true if the sprite is pointed at any of the walls */
    private static boolean matches(Sprite marker, List<Integer> walls, MapView map){
        //System.out.println("entering matches()");
        for(int i = 0; i < walls.size(); ++i){
            //System.out.println("wall id=" + walls.get(i) + " location: " + map.getWall(walls.get(i)).getLocation());
            if(MapUtil.isSpritePointedAtWall(marker, walls.get(i), map)){
                return true;
            }else{
            }
        }
        return false;
    }

    public static Connector create(MapView map, Sprite markerSprite) throws MapErrorException {
        Sprite s = markerSprite;
        Sector sector = map.getSector(s.getSectorId());

        if(s.getLotag() == PrefabUtils.MarkerSpriteLoTags.SIMPLE_CONNECTOR) {
            // TODO this branch is deprecated -- replaced by RedwallConnectorScanner


            // TODO - this code has a bug that causes it to run forever if every wall in the sector is marked with 1

            List<Integer> linkWallIds = SimpleConnectorScanner$.MODULE$.getLinkWallIdsJava(map, s.getSectorId());

            if (linkWallIds.size() == 0) {
                throw new SpriteLogicException("missing link wall for marker sprite at " + s.getLocation().toString());
            }
            // how many different groups of contiguous lotag-1 walls are there?
            List<List<Integer>> partitions = ConnectorFactory2.partitionWalls(linkWallIds, map);

            if (partitions.size() < 1) {
                throw new RuntimeException("programming error");
            } else if (partitions.size() == 1) {
                // only one wall segment, assume the connector matches

                // TODO get rid of this ability (valid connector without pointing the sprite at the wall) ?
                if (!matches(s, partitions.get(0), map)) {
                    throw new SpriteLogicException("redwall connector sprite is not pointed at a connector wall", s);
                }

                return redWallConn(s, sector, partitions.get(0), map);
            } else {
                // figure out which group the  marker sprite is pointing to
                for (int partIdx = 0; partIdx < partitions.size(); ++partIdx) {
                    if (matches(s, partitions.get(partIdx), map)) {
                        return redWallConn(s, sector, partitions.get(partIdx), map);
                    }
                }
                throw new SpriteLogicException("Cannot match connector to its walls (point the sprite at the correct wall(s)");
            }
        }else{
            return createOther(map, markerSprite);
        // }else if(s.getLotag() == PrefabUtils.MarkerSpriteLoTags.TELEPORT_CONNECTOR) {
        //     return new TeleportConnector(s, sector.getLotag());
        // }else if(s.getLotag() == PrefabUtils.MarkerSpriteLoTags.ELEVATOR_CONNECTOR){
        //     if(sector.getLotag() != 15){
        //         throw new SpriteLogicException("elevector connector in sector with lotag != 15");
        //     }
        //     return new ElevatorConnector(s);
        // }else{
        //     return null;
        }
    }

    public static Connector createOther(MapView map, Sprite markerSprite) throws MapErrorException {
        Sector sector = map.getSector(markerSprite.getSectorId());
        if(markerSprite.getLotag() == PrefabUtils.MarkerSpriteLoTags.TELEPORT_CONNECTOR) {
            return new TeleportConnector(markerSprite, sector.getLotag());
        }else if(markerSprite.getLotag() == PrefabUtils.MarkerSpriteLoTags.ELEVATOR_CONNECTOR){
            if(sector.getLotag() != 15){
                throw new SpriteLogicException("elevector connector in sector with lotag != 15");
            }
            return new ElevatorConnector(markerSprite);
        }else{
            return null;
        }
    }

}
