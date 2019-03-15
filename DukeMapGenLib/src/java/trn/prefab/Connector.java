package trn.prefab;

import trn.*;
import trn.duke.MapErrorException;

import java.util.ArrayList;
import java.util.List;

// TODO:  consider making Connector an interface, and RedwallConnector the base class.
public abstract class Connector {
    /** used to get new connector when pasting sector group onto a map */
    public abstract Connector translateIds(final IdMap idmap);

    /**
     * the connector id is an optional id which is set as the hitag of the marker sprite
     * in order to let you name that specific connector.
     * @return
     */
    public abstract int getConnectorId();

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
        //PrefabUtils.findConnector(outMap, PrefabUtils.JoinType.VERTICAL_JOIN, 1);
        //Map map = numberedSectorGroups.get(sectorGroupId);

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

}
