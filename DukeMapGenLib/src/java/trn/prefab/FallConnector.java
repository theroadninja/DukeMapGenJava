package trn.prefab;

import trn.*;

/**
 * TODO:  I dont want to make this extends `Connector` however there is too much code to fix this now.
 * Note:  `HasTags` is fine.  I created that to help replace `Connector`
 */
public class FallConnector extends OtherConnector implements HasTags<FallConnector> {
    // this must be set, b/c there can be more than one fall conn in a sector
    private final int connectorId;
    private final int sectorId;
    private final int markerZ;
    private final int floorZ;
    private final int ceilZ;

    public static int getId(Sprite marker){
        int connId = marker.getHiTag();
        if(connId < 1){
            throw new SpriteLogicException("fall connector must have an id (nonzero hitag)", marker);
        }
        return connId;
    }

    public FallConnector(int connectorId, int sectorId, int markerZ, int floorZ, int ceilZ){
        super(connectorId);
        SpriteLogicException.throwIf(connectorId < 1, "fall connector missing id");
        this.connectorId = connectorId;
        this.sectorId = sectorId;
        this.markerZ = markerZ;
        this.floorZ = floorZ;
        this.ceilZ = ceilZ;
    }

    public FallConnector(Sprite marker, Sector sector) {
        this(getId(marker), marker.getSectorId(), marker.getLocation().z, sector.getFloorZ(), sector.getCeilingZ());
    }

    // TODO something like boolean isFloor() ... (closer to floor vs closer to ceiling)

    @Override
    public int getConnectorType() {
        return ConnectorType.FALL_CONNECTOR;
    }

    @Override
    public FallConnector translateIds(IdMap idmap, PointXYZ delta, MapView map) {
        return new FallConnector(
                this.connectorId,
                idmap.sector(this.sectorId),
                this.markerZ,
                this.floorZ,
                this.ceilZ
        );
    }

    @Override
    public short getSectorId() {
        return (short)this.sectorId;
    }
}
