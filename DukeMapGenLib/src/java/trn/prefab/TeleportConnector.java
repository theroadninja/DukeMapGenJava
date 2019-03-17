package trn.prefab;

import trn.*;

import java.util.List;

public class TeleportConnector extends Connector {

    private final int connectorId;
    private final int sectorId;


    private TeleportConnector(int connectorId, int sectorId){
        this.connectorId = connectorId;
        this.sectorId = sectorId;
    }

    public TeleportConnector(Sprite markerSprite) {
        if(markerSprite == null) throw new IllegalArgumentException("null params");

        this.sectorId = markerSprite.getSectorId();
        this.connectorId = markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1;

    }

    @Override
    public short getSectorId() {
        return (short)this.sectorId;
    }
    @Override
    public int getConnectorId() {
        return this.connectorId;
    }

    @Override
    public int getConnectorType() {
        return ConnectorType.TELEPORTER;
    }

    @Override
    public TeleportConnector translateIds(IdMap idmap) {
        return new TeleportConnector(
                this.connectorId,
                idmap.sector(this.sectorId)
        );
    }

    @Override
    public boolean isLinked(Map map) {
        List<Sprite> list = map.findSprites(DukeConstants.TEXTURES.SECTOR_EFFECTOR, DukeConstants.SE_LOTAGS.TELEPORT, this.sectorId);
        if(list.size() != 1){
            // TODO - this is actually b.s. -- you are allowed to have more than one SE 7 sprite,
            // for the falling type of teleports.
            throw new SpriteLogicException("wrong number of SE 7 sprites in teleporter sector.");
        }

        // if it has a hitag, assume it is linked
        return list.get(0).getHiTag() != 0;
    }

    @Override
    public boolean hasXYRequirements() {
        return false;
    }
}
