package trn.prefab;

import trn.IdMap;
import trn.Map;
import trn.PointXYZ;
import trn.Sprite;

// TODO - or should the main feature of this class be that it matters where the sector is?
public abstract class RedwallConnector extends Connector {

    protected RedwallConnector(int connectorId){
        super(connectorId);
    }
    protected RedwallConnector(Sprite markerSprite){
        this(markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1);
    }

    public abstract PointXYZ getTransformTo(RedwallConnector c2);

    @Override
    public abstract RedwallConnector translateIds(final IdMap idmap, PointXYZ delta);

    /**
     * @returns the sum of the manhattan-distance length of each wall in the group
     */
    public abstract long totalManhattanLength(Map map);

    public final long totalManhattanLength(SectorGroup sg) {
        return totalManhattanLength(sg.getMap());
    }

    /**
     * If this is a simple connector and has a single wall that is aligned to x or y axis,
     * then return which side of the sector group it is on.
     *
     * Otherwise, returns null.
     *
     * e.g. a connector with heading "east" would be:
     *
     * +---------+
     * |         .
     * |  (20)-> .
     * |         .
     * +---------+
     */
    public Integer getSimpleHeading(){
        return null;
    }

    @Override
    public abstract int getConnectorType();

    /**
     * Tests if the connectors match, i.e. if they could mate.
     * The sector groups dont have to be already lined up, but there must exist
     * a transformation that will line the sectors up.
     *
     * TODO - support rotation also (or maybe not...)
     * @return
     */
    public abstract boolean isMatch(RedwallConnector c);

    /**
     * meant to be used for two connectors that have already been pasted, to see if they are in the same place
     * TODO - maybe this doesnt belong here (because it is specific to pasted connectors)
     */
    public final boolean isFullMatch(RedwallConnector c, Map map){
        return isMatch(c) && getTransformTo(c).equals(PointXYZ.ZERO) && !(isLinked(map) || c.isLinked(map));
    }

    public abstract void removeConnector(Map map);

    public abstract PointXYZ getAnchorPoint();

    public abstract void linkConnectors(Map map, RedwallConnector otherConn);
}
