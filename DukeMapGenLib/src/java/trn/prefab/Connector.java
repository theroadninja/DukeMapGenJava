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
    public abstract Connector translateIds(final IdMap idmap, PointXYZ delta);

    /**
     * the connector id is an optional id which is set as the hitag of the marker sprite
     * in order to let you name that specific connector.
     * @return
     */
    public final int getConnectorId(){
        return connectorId;
    }

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

    public static final int idOf(Sprite markerSprite){
        //return (markerSprite != null && markerSprite.getHiTag() > 0) ? markerSprite.getHiTag() : -1;
        return PrefabUtils.hitagToId(markerSprite);
    }

}
