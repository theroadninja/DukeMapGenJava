package trn.prefab;

import trn.IdMap;
import trn.PointXYZ;

// TODO - or should the main feature of this class be that it matters where the sector is?
public abstract class RedwallConnector extends Connector {

    public abstract PointXYZ getTransformTo(Connector c2);

    public abstract RedwallConnector translateIds(final IdMap idmap);

    public abstract int getWallId();

    @Override
    public abstract int getConnectorType();

    @Override
    public boolean hasXYRequirements() {
        return true;
    }
}
