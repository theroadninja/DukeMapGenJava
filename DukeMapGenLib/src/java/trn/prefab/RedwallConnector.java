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

    //public abstract int getWallId();

    @Override
    public abstract int getConnectorType();

    @Override
    public boolean hasXYRequirements() {
        return true;
    }

    public abstract void removeConnector(Map map);

    public abstract PointXYZ getAnchorPoint();

    public abstract void linkConnectors(Map map, RedwallConnector otherConn);
}
