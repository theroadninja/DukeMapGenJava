package trn.prefab;

public class RedwallJoinType {

    final int connectorType1;

    final int connectorType2;

    private RedwallJoinType(int firstConnectorType, int secondConnectorType){
        this.connectorType1 = firstConnectorType;
        this.connectorType2 = secondConnectorType;
    }

    /**
     * TODO - : sadly the first room is to the south, the "north" refers to the north connector
     * of the first room...
     */
    public static final RedwallJoinType NorthToSouth = new RedwallJoinType(
            ConnectorType.VERTICAL_NORTH,
            ConnectorType.VERTICAL_SOUTH
    );
    //SouthToNorth?

    public static final RedwallJoinType EastToWest = new RedwallJoinType(
            ConnectorType.HORIZONTAL_EAST,
            ConnectorType.HORIZONTAL_WEST
    );
    //WestToEast?
}
