package trn.prefab;

public class ConnectorType {

    public static boolean isRedwallType(int connectorType){
        return connectorType == HORIZONTAL_EAST
                || connectorType == HORIZONTAL_WEST
                || connectorType == VERTICAL_NORTH
                || connectorType == VERTICAL_SOUTH
                || connectorType == MULTI_REDWALL;
    }

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
    public static int HORIZONTAL_EAST = 16;

    /** horizontal connector on the west side of the group */
    public static int HORIZONTAL_WEST = 17;

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
    public static int VERTICAL_SOUTH = 18;

    /** vertical connector on the north side of the sector */
    public static int VERTICAL_NORTH = 19;

    // leave 20 for the auto marker

    public static int MULTI_REDWALL = 21;

    /** multi redwall that can be in more than one sector */
    public static int MULTI_SECTOR = 22;
}
