package trn.prefab;

import trn.*;
import trn.duke.Lotags;
import trn.duke.TextureList;

import java.util.List;

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
    private final boolean closerToFlr;

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

        int df = Math.abs(floorZ - markerZ);
        int dc = Math.abs(markerZ - ceilZ);
        this.closerToFlr = df < dc;

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

    /**
     *
     * @return True if the sprite is closer to floor (something you fall into),
     *  False if the sprite is closer to the ceiling (something you fall out of)
     */
    public boolean closerToFloor() {
        return this.closerToFlr;
    }

    Sprite getMarkerSprite(Map map){
        List<Sprite> list = map.findSprites(
            Marker.MARKER_SPRITE_TEX,
            Marker.Lotags.FALL_CONNECTOR,
            sectorId
        );
        Sprite marker = null;
        for(Sprite s: list){
            if(s.getHiTag() == connectorId){
                if (marker != null){
                    throw new SpriteLogicException(String.format("more than one fall connector marker with id %s", connectorId));
                }
                marker = s;
            }
        }
        if(marker == null){
            throw new SpriteLogicException(String.format("cannot find marker with connector id %s", connectorId));
        }
        return marker;
    }

    public void replaceMarkerSprite(Map map, int channel){
        Sprite marker = getMarkerSprite(map);
        marker.setTexture(TextureList.SE);
        marker.setLotag(Lotags.SE.TELEPORT);
        marker.setHiTag(channel);
    }

    /**
     * "links" the two FallConnectors together, though you have to provide the "channel" (unique hitag)
     *
     * This doesnt really do much, but other connectors have a method like this so it makes the API easier to
     * understand.
     */
    public void linkConnectors(Map map, FallConnector other, int channel) {
        replaceMarkerSprite(map, channel);
        other.replaceMarkerSprite(map, channel);
    }

    public static boolean isFallConn(Connector c){
        return c.getConnectorType() == ConnectorType.FALL_CONNECTOR;
    }
}
