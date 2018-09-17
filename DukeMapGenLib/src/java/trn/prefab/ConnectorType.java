package trn.prefab;

public class ConnectorType {
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
}
