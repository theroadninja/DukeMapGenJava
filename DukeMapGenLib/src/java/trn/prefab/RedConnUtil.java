package trn.prefab;

import trn.*;

import java.util.ArrayList;
import java.util.List;

public class RedConnUtil {

    static List<Integer> toList(int element){
        return new ArrayList<Integer>(){{
            add(element);
        }};
    }

    static List<Integer> toList(int element, int count){
        List<Integer> results = new ArrayList<>(count);
        for(int i = 0; i < count; ++i){
            results.add(element);
        }
        return results;
    }

    public static int headingForConnectorType(int connectorType){
        if(connectorType == ConnectorType.VERTICAL_SOUTH){
            return Heading.S;
        }else if(connectorType == ConnectorType.VERTICAL_NORTH){
            return Heading.N;
        }else if(connectorType == ConnectorType.HORIZONTAL_EAST){
            return Heading.E;
        }else if(connectorType == ConnectorType.HORIZONTAL_WEST){
            return Heading.W;
        }else{
            //throw new IllegalArgumentException("Invalid SimpleConnector type: " + connectorType);
            return -1;
        }
    }

    @Deprecated
    public static int connectorTypeForWall(WallView wall){
        PointXY vector = wall.getUnitVector();
        if(vector.x == 1) {
            return ConnectorType.VERTICAL_NORTH;
        }else if(vector.x == -1){
            return ConnectorType.VERTICAL_SOUTH;
        }else if(vector.y == 1){ // y is pointed down
            return ConnectorType.HORIZONTAL_EAST;
        }else if(vector.y == -1){ // y is pointed up
            return ConnectorType.HORIZONTAL_WEST;
        }else{
            return ConnectorType.MULTI_REDWALL;
            //throw new IllegalArgumentException("connector wall must be horizontal or vertical");
        }
    }

    @Deprecated
    public static int connectorTypeForWalls(List<WallView> walls){
        if(walls.size() < 0){
            throw new IllegalArgumentException();
        }else if(walls.size() == 1){
            return connectorTypeForWall(walls.get(0));
        }else{
            return ConnectorType.MULTI_REDWALL;
        }
    }

    /**
     * Calculates an anchor for complete loops by just picking a point in the "center"
     */
    public static PointXY getAnchorForLoop(List<Integer> wallIds, MapView map){
        if(wallIds.size() < 1){
            throw new IllegalArgumentException("wallIds is empty");
        }
        int totalX = 0;
        int totalY = 0;

        for(int i = 0; i < wallIds.size() - 1; ++i){
            int wallId = wallIds.get(i);
            Wall w = map.getWall(wallId);
            totalX += w.getX();
            totalY += w.getY();
        }
        return new PointXY(totalX / wallIds.size(), totalY / wallIds.size());
    }

    public static PointXY getAnchor(List<Integer> wallIds, MapView map){
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
            return getAnchorForLoop(wallIds, map);
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

    static long totalManhattanLength(List<Integer> wallIds, MapView map){
        long sum = 0;
        for(int wallId: wallIds){
            Wall w1 = map.getWall(wallId);
            Wall w2 = map.getWall(w1.getNextWallInLoop());
            sum += w1.getLocation().manhattanDistanceTo(w2.getLocation());
        }
        return sum;
    }

    static List<PointXY> allRelativeConnPoints(List<Integer> wallIds, MapView map, PointXYZ anchor, PointXY anchor2){
        List<PointXY> results = new ArrayList<>(wallIds.size() + 1);
        for(Integer i: wallIds){
            results.add(map.getWall(i).getLocation().subtractedBy(anchor));
        }
        results.add(anchor2.subtractedBy(anchor));
        return results;
    }

}
