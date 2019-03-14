package trn.prefab;

import trn.IdMap;
import trn.PointXYZ;

public abstract class RedwallConnector extends Connector {
    // TODO - maybe get rid of this...
    // (teleporters wont need a "transformto" ...
    public abstract PointXYZ getTransformTo(Connector c2);

    public abstract RedwallConnector translateIds(final IdMap idmap);

    public abstract int getWallId();

    public abstract short getConnectorType();
}
