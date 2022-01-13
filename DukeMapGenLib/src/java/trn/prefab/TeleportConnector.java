package trn.prefab;

import trn.*;
import trn.duke.Lotags;
import trn.duke.TextureList;

import java.util.List;


/**
 * NOTE:  apparently the difference between a normal teleporter and water teleporters
 * is simply the lotags of the sector!
 *
 * lotag 0 = teleporter
 * lotag 1 = above water
 * lotag 2 = below water
 */
public class TeleportConnector extends Connector {

    public static final int CONNECTOR_TYPE = ConnectorType.TELEPORTER;

    private final int sectorId;

    // this tracks whether we are using the "replace" behavior -- it does not track whether the replacement has happened
    private final boolean mustReplaceMarkerSprite;

    private final boolean water;

    private TeleportConnector(int connectorId, int sectorId, boolean replaceMarkerSprite, boolean water){
        super(connectorId);
        this.sectorId = sectorId;
        this.mustReplaceMarkerSprite = replaceMarkerSprite;
        this.water = water;
    }

    public TeleportConnector(Sprite markerSprite, int sectorLotag) {
        super(markerSprite != null && markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1);
        if(markerSprite == null) throw new IllegalArgumentException("null params");

        this.sectorId = markerSprite.getSectorId();
        //this.connectorId = markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1;

        this.mustReplaceMarkerSprite = markerSprite.getLotag() == PrefabUtils.MarkerSpriteLoTags.TELEPORT_CONNECTOR;
        this.water = (sectorLotag == Lotags.ST.WATER_ABOVE || sectorLotag == Lotags.ST.WATER_BELOW);
    }

    public boolean isWater(){
        return this.water;
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
    public TeleportConnector translateIds(IdMap idmap, PointXYZ delta, MapView map) {
        return new TeleportConnector(
                this.connectorId,
                idmap.sector(this.sectorId),
                this.mustReplaceMarkerSprite,
                this.water
        );
    }

    @Override
    public boolean isLinked(Map map) {
        if(this.mustReplaceMarkerSprite){
            List<Sprite> list = map.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.TELEPORT_CONNECTOR, this.sectorId);
            return list.size() < 1;
        }else{
            // TODO - use this.getSESprite()
            List<Sprite> list = map.findSprites(TextureList.SE, Lotags.SE.TELEPORT, this.sectorId);
            if(list.size() != 1){
                // TODO - this is actually b.s. -- you are allowed to have more than one SE 7 sprite,
                // for the falling type of teleports.
                throw new SpriteLogicException("wrong number of SE 7 sprites in teleporter sector.");
            }

            // if it has a hitag, assume it is linked
            return list.get(0).getHiTag() != 0;
        }
    }

    /**
     * NOTE:  should not be public, in case we dont have an SE sprite until the connection is made.
     *
     * @return the actual SE 7 sprite used to make the elevator work
     */
    Sprite getSESprite(ISectorGroup sg){
        List<Sprite> list = getSESprites(sg);
        if(list.size() != 1) throw new SpriteLogicException(String.format("wrong number(%s) of teleporter sprites in sector", list.size()));
        return list.get(0);
    }

    private List<Sprite> getSESprites(ISectorGroup sg) {
        return sg.findSprites(TextureList.SE, Lotags.SE.TELEPORT, sectorId);
    }


    /**
     * Returns the marker sprite, but only in the case where we are replacing the marker sprite
     * with the SE sprite.
     * @param sg
     * @return
     */
    private Sprite getMarker27Sprite(ISectorGroup sg){
        if(!mustReplaceMarkerSprite) throw new IllegalStateException();

        List<Sprite> list = sg.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.TELEPORT_CONNECTOR, sectorId);
        if(list.size() > 1) throw new SpriteLogicException("wrong number of teleporter marker sprites in sector");
        return list.size() > 0 ? list.get(0) : null;
    }

    private void replaceMarkerSprite(ISectorGroup sg){
       if(!mustReplaceMarkerSprite) throw new IllegalStateException();
       Sprite markerSprite = getMarker27Sprite(sg);
       if(markerSprite == null) throw new SpriteLogicException("no teleporter marker sprites to replace");
       markerSprite.setTexture(TextureList.SE);
       markerSprite.setLotag(Lotags.SE.TELEPORT);
    }

    public PointXYZ getSELocation(ISectorGroup sg){
        if(mustReplaceMarkerSprite){
            Sprite marker = getMarker27Sprite(sg);
            if(marker != null){
                return marker.getLocation();
            }
        }

        return getSESprite(sg).getLocation();

    }

    // TODO - the groups are only needed because they contain the map.  We actually only need
    // one ISectorGroup ...
    public static void linkTeleporters( // Intellij might show no usages, but SgMapBuilder calls this
            TeleportConnector conn1,
            ISectorGroup group1,
            TeleportConnector conn2,
            ISectorGroup group2,
            int hitag
    ){
        if(conn1.mustReplaceMarkerSprite){
            conn1.replaceMarkerSprite(group1);
        }
        if(conn2.mustReplaceMarkerSprite){
            conn2.replaceMarkerSprite(group2);
        }
        conn1.getSESprite(group1).setHiTag(hitag);
        conn2.getSESprite(group2).setHiTag(hitag);
    }
    public static void linkTeleporters(
            Connector conn1,
            ISectorGroup group1,
            Connector conn2,
            ISectorGroup group2,
            int hitag){
        linkTeleporters(
                (TeleportConnector)conn1,
                group1,
                (TeleportConnector)conn2,
                group2,
                hitag);
    }

}
