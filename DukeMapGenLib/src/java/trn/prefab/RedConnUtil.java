package trn.prefab;

import trn.*;

import java.util.ArrayList;
import java.util.List;

public class RedConnUtil {

    public static ConnectorFilter SouthConnector = new ConnectorTypeFilter(ConnectorType.VERTICAL_SOUTH);
    public static ConnectorFilter NorthConnector = new ConnectorTypeFilter(ConnectorType.VERTICAL_NORTH);
    public static ConnectorFilter EastConnector = new ConnectorTypeFilter(ConnectorType.HORIZONTAL_EAST);
    public static ConnectorFilter WestConnector = new ConnectorTypeFilter(ConnectorType.HORIZONTAL_WEST);


    public static List<Integer> toList(int element){
        return new ArrayList<Integer>(){{
            add(element);
        }};
    }

    public static int connectorTypeForHeading(int heading){
        if(Heading.E == heading){
            return ConnectorType.HORIZONTAL_EAST;
        }else if(Heading.W == heading){
            return ConnectorType.HORIZONTAL_WEST;
        }else if(Heading.N == heading){
            return ConnectorType.VERTICAL_NORTH;
        }else if(Heading.S == heading){
            return ConnectorType.VERTICAL_SOUTH;
        }else{
            throw new IllegalArgumentException();
        }
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
            throw new IllegalArgumentException("connector wall must be horizontal or vertical");
        }
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

    static long totalManhattanLength(List<Integer> wallIds, Map map){
        long sum = 0;
        for(int wallId: wallIds){
            Wall w1 = map.getWall(wallId);
            Wall w2 = map.getWall(w1.getNextWallInLoop());
            sum += w1.getLocation().manhattanDistanceTo(w2.getLocation());
        }
        return sum;
    }

    static List<PointXY> allRelativeConnPoints(List<Integer> wallIds, Map map, PointXYZ anchor, PointXY anchor2){
        List<PointXY> results = new ArrayList<>(wallIds.size() + 1);
        for(Integer i: wallIds){
            results.add(map.getWall(i).getLocation().subtractedBy(anchor));
        }
        results.add(anchor2.subtractedBy(anchor));
        return results;
    }

}
