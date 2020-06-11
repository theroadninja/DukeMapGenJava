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

    /** used to get new connector when pasting sector group onto a map */
    public abstract Connector translateIds(final IdMap idmap, PointXYZ delta, Map map);

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

    public static List<Connector> findConnectors(Map map) throws MapErrorException {
        return findConnectors(map, null);
    }

    public static Iterable<Connector> findConnectors(Iterable<Connector> connectors, ConnectorFilter ... cf){
        List<Connector> results = new ArrayList<Connector>();
        for(Connector c: connectors){
            if(cf == null ||  ConnectorFilter.allMatch(c, cf)){
                results.add(c);
            }
        }
        return results;
    }
    public static List<Connector> findConnectors(Map map, ConnectorFilter ... cf) throws MapErrorException {
        List<Connector> results = new ArrayList<Connector>();
        for(Sprite s: map.findSprites(
                PrefabUtils.MARKER_SPRITE)){

            Sector sector = map.getSector(s.getSectorId());

            Connector connector = ConnectorFactory.create(map, s);
            if(connector != null && (cf == null ||  ConnectorFilter.allMatch(connector, cf))){
                results.add(connector);
            }
        } // for sprite

        return results;
    }

    public static List<Connector> findConnectorsInPsg(Map map, MapUtil.CopyState copystate) throws MapErrorException {
        ConnectorFilter cf = new ConnectorFilter(){
            @Override
            public boolean matches(Connector c) {
                return copystate.destSectorIds().contains(c.getSectorId());
            }
        };
        return findConnectors(map, cf);
    }

    /** if the sprite has a hitag > 0, then returns the hitag, otherwise -1 */
    public static final int idOf(Sprite markerSprite){
        return PrefabUtils.hitagToId(markerSprite);
    }

}
