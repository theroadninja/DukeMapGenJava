package trn.prefab;

import trn.*;
import trn.duke.MapErrorException;

import java.util.ArrayList;
import java.util.List;

public abstract class Connector {

    public abstract short getConnectorType();

    // TODO - get rid of this!
    public abstract int getWallId();

    // TODO - maybe get rid of this...
    // (teleporters wont need a "transformto" ...
    public abstract PointXYZ getTransformTo(Connector c2);




    /** used to get new connector when pasting sector group onto a map */
    public abstract Connector translateIds(final IdMap idmap);

    public abstract short getSectorId();

    public abstract int getConnectorId();



    public abstract boolean isLinked(Map map);


    public abstract boolean canMate(Connector c);

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
            if(s.getLotag() == PrefabUtils.SpriteLoTags.HORIZONTAL_CONNECTOR_EAST){

                Connector connector = ConnectorFactory.create(map, s);
                if(cf == null ||  ConnectorFilter.allMatch(connector, cf)){
                    results.add(connector);
                }

            }else{

                Connector connector = ConnectorFactory.create(map, s);
                if(connector != null && (cf == null ||  ConnectorFilter.allMatch(connector, cf))){
                    results.add(connector);
                }
            }


            //
        } // for sprite

        return results;
    }

    public static List<Connector> matchConnectors(Iterable<Connector> connectors, ConnectorFilter ... cf){
        List<Connector> results = new ArrayList<Connector>();
        for(Connector c: connectors){
            if(cf == null || ConnectorFilter.allMatch(c, cf)){
                results.add(c);
            }
        }
        return results;
    }
}
