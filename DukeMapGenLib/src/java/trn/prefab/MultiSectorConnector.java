package trn.prefab;

import trn.IdMap;
import trn.Map;
import trn.PointXYZ;

import java.util.List;

public class MultiSectorConnector extends RedwallConnector {

    protected MultiSectorConnector(int connectorId) {
        super(connectorId);
    }

    @Override
    public PointXYZ getTransformTo(RedwallConnector c2) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public RedwallConnector translateIds(IdMap idmap, PointXYZ delta) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public short getSectorId() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public List<Integer> getSectorIds() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public boolean isLinked(Map map) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public long totalManhattanLength(Map map) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public int getConnectorType() {
        return ConnectorType.MULTI_SECTOR;
    }

    @Override
    public boolean isMatch(RedwallConnector c) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void removeConnector(Map map) {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public PointXYZ getAnchorPoint() {
        throw new RuntimeException("Not implemented yet");
    }

    @Override
    public void linkConnectors(Map map, RedwallConnector otherConn) {
        throw new RuntimeException("Not implemented yet");
    }
}
