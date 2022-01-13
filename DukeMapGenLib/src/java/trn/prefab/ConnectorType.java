package trn.prefab;

public class ConnectorType {

    public static boolean isRedwallType(int connectorType){
        return connectorType == HORIZONTAL_EAST
                || connectorType == HORIZONTAL_WEST
                || connectorType == VERTICAL_NORTH
                || connectorType == VERTICAL_SOUTH
                || connectorType == MULTI_REDWALL
                || connectorType == MULTI_SECTOR;
    }

    // public static int fromHeading(int heading){
    //     if(heading == Heading.E){
    //         return HORIZONTAL_EAST;
    //     }else if(heading == Heading.S){
    //         return VERTICAL_SOUTH;
    //     }else if(heading == Heading.W){
    //         return HORIZONTAL_WEST;
    //     }else if(heading == Heading.N){
    //         return VERTICAL_NORTH;
    //     }else{
    //         throw new IllegalArgumentException(String.format("invalid heading: %s", heading));
    //     }
    // }

    public static int TELEPORTER = 7; // TODO - maybe this should be 27 to match the prefab... (or that one should be 7)

    public static int ELEVATOR = 8; // TODO - actually this one should be 17

    /**
     * -+               +--------+
     *  |               |        |
     *  |----\ \--------|
     *  |     \ \       |
     *    EAST \ \
     *  |      / / WEST |
     *        / /
     *  |----/ /--------|
     *  |               |        |
     * -+               +--------+
     *
     */
    /** horizontal connector on the east side of the group */
    public static int HORIZONTAL_EAST = 16; // TODO get rid of this (but careful; its used for more than lotags)

    /** horizontal connector on the west side of the group */
    public static int HORIZONTAL_WEST = 17; // TODO get rid of this

    /**
     * ----| - - |----
     *     |SOUTH|
     *      \   /
     *     |\\ //|
     *     | \ / |
     *     |NORTH|
     * ----| - - |----
     */
    /** vertical connector on the south side of the sector */
    public static int VERTICAL_SOUTH = 18; // TODO get rid of this

    /** vertical connector on the north side of the sector */
    public static int VERTICAL_NORTH = 19; // TODO get rid of this

    // leave 20 for the auto marker

    public static int MULTI_REDWALL = 21;

    /** multi redwall that can be in more than one sector */
    public static int MULTI_SECTOR = 22;
}
