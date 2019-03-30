package trn.prefab;

import trn.*;
import trn.duke.Lotags;
import trn.duke.TextureList;

import java.util.List;

public class TeleportConnector extends Connector {

    private final int sectorId;

    private TeleportConnector(int connectorId, int sectorId){
        super(connectorId);
        this.sectorId = sectorId;
    }

    public TeleportConnector(Sprite markerSprite) {
        super(markerSprite != null && markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1);
        if(markerSprite == null) throw new IllegalArgumentException("null params");

        this.sectorId = markerSprite.getSectorId();
        //this.connectorId = markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1;

    }

    @Override
    public short getSectorId() {
        return (short)this.sectorId;
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


    /**
     * @return the actual SE 17 sprite used to make the elevator work
     */
    public Sprite getSESprite(ISectorGroup sg){
        List<Sprite> list = getSESprites(sg.getMap());
        if(list.size() != 1) throw new SpriteLogicException("too many elevator sprites in sector");
        return list.get(0);
    }

    private List<Sprite> getSESprites(Map map) {
        return map.findSprites(TextureList.SE, Lotags.SE.TELEPORT, sectorId);
    }

    public static void linkTeleporters(
            TeleportConnector conn1,
            ISectorGroup group1,
            TeleportConnector conn2,
            ISectorGroup group2,
            int hitag
    ){
        conn1.getSESprite(group1).setHiTag(hitag);
        conn2.getSESprite(group2).setHiTag(hitag);
    }
}
