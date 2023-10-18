package trn.prefab;

import trn.Map;

/**
 * The abstract class `Connector` was an abstraction mistake:  I did not realize that 95% of
 * connector-related logic would be for redwall connectors (and in the beginning, the redwall
 * connector was actually three different classes).
 *
 * "redwall" and "teleport/elevator/water" connectors don't really have much in common.  They
 * shouldn't be chained together by an abstract class.
 *
 * This class is just a hack to make the `Connector` abstract class less annoying until I can
 * get rid of it.
 */
public abstract class OtherConnector extends Connector {
    protected OtherConnector(int connectorId) {
        super(connectorId);
    }
    /**
     * Should return True ONLY for the concrete TeleporterConnector class
     * @return
     */
    @Override
    public boolean isTeleporter() {
        return false;
    }

    /**
     * Should return True ONLY for the concrete ElevatorConnector class
     * @return
     */
    @Override
    public boolean isElevator() {
        return false;
    }

    @Override
    public boolean isRedwall() {
        return false;
    }

    @Override
    public boolean isLinked(Map map) {
        return false;
    }
}
