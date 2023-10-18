package trn.prefab;

import trn.*;
import trn.duke.MapErrorException;

import java.util.ArrayList;
import java.util.List;

// TODO:  consider making Connector an interface, and RedwallConnector the base class.
public abstract class Connector {

    /** optional number that identifies the connector within the sector group */
    protected final int connectorId;

    protected Connector(int connectorId){
        this.connectorId = connectorId;
    }

    public abstract int getConnectorType();

    // why this ugliness?  because elevators, teleporters, and redconns shouldnt have had a common base class.
    public abstract boolean isTeleporter();

    // why this ugliness?  because elevators, teleporters, and redconns shouldnt have had a common base class.
    public abstract boolean isElevator();

    // why this ugliness?  because elevators, teleporters, and redconns shouldnt have had a common base class.
    public abstract boolean isRedwall();

    /** used to get new connector when pasting sector group onto a map */
    public abstract Connector translateIds(final IdMap idmap, PointXYZ delta, MapView map);

    /**
     * the connector id is an optional id which is set as the hitag of the marker sprite
     * in order to let you name that specific connector.
     * @return
     */
    public final int getConnectorId(){
        return connectorId;
    }

    /** sector id of the marker sprite (connectors can have more than 1 sector) */
    public abstract short getSectorId();

    public abstract boolean isLinked(Map map);

    /** if the sprite has a hitag > 0, then returns the hitag, otherwise -1 */
    public static final int idOf(Sprite markerSprite){
        return PrefabUtils.hitagToId(markerSprite);
    }

}
