package trn.prefab;

import trn.*;
import trn.duke.Lotags;
import trn.duke.TextureList;

import java.util.List;

// https://www.francoz.net/doc/duke3d/English/Ascenseurs.htm
// https://wiki.eduke32.com/wiki/Sector_Effector_Reference_Guide

// on the map format
//https://fabiensanglard.net/duke3d/BUILDINF.TXT
public class ElevatorConnector extends Connector {

    private final int sectorId;

    // this tracks whether we are using the "replace" behavior -- it does not track whether the replacement has happened
    private final boolean mustReplaceMarkerSprite;

    public ElevatorConnector(Sprite markerSprite){
        super(Connector.idOf(markerSprite));
        sectorId = markerSprite.getSectorId();
        this.mustReplaceMarkerSprite = markerSprite.getLotag() == PrefabUtils.MarkerSpriteLoTags.ELEVATOR_CONNECTOR;
    }

    private ElevatorConnector(int connectorId, int sectorId, boolean mustReplaceMarkerSprite){
        super(connectorId);
        this.sectorId = sectorId;
        this.mustReplaceMarkerSprite = mustReplaceMarkerSprite;
    }

    @Override
    public short getSectorId() {
        return (short) this.sectorId;
    }

    @Override
    public int getConnectorType() {
        return ConnectorType.ELEVATOR;
    }

    @Override
    public ElevatorConnector translateIds(IdMap idmap, PointXYZ delta, MapView map) {
        return new ElevatorConnector(connectorId, idmap.sector(this.sectorId), this.mustReplaceMarkerSprite);
    }

    /**
     * Uses hitag of SE sprite to determine if it is linked.
     */
    @Override
    public boolean isLinked(Map map) {
        if(this.mustReplaceMarkerSprite){
            List<Sprite> list = map.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.ELEVATOR_CONNECTOR, this.sectorId);
            return list.size() < 1;
        }else{
            List<Sprite> list = getSESprites(map);
            if(list.size() != 1){
                throw new SpriteLogicException("wrong number of SE 17 sprites in teleporter sector.");
            }
            return list.get(0).getHiTag() != 0;
        }
    }

    /**
     * @return the actual SE 17 sprite used to make the elevator work
     */
    public Sprite getSESprite(ISectorGroup sg){
        List<Sprite> list = getSESprites(sg);
        if(list.size() != 1) throw new SpriteLogicException("too many elevator sprites in sector");
        return list.get(0);
    }

    private List<Sprite> getSESprites(Map map){
        return map.findSprites(TextureList.SE, Lotags.SE.ELEVATOR, sectorId);
    }
    private List<Sprite> getSESprites(ISectorGroup sg){
        return sg.findSprites(TextureList.SE, Lotags.SE.ELEVATOR, sectorId);
    }

    // for auto marker 20, to distinguish between other uses of the auto marker
    @Deprecated
    public static boolean isElevatorMarker(MapView map, Sprite markerSprite){
        int sectorId = markerSprite.getSectorId();
        if(map.getSector(sectorId).getLotag() != Lotags.ST.ELEVATOR){
            return false;
        }
        return map.findSprites(TextureList.SE, Lotags.SE.ELEVATOR, sectorId).size() > 0;
    }


    /**
     * Provides a sort function
     *
     * @param conn
     * @param map
     * @returns a number that will be lower for elevators that are physically lower
     */
    public static int sortKey(ElevatorConnector conn, Map map){
        // we need to multiple by -1 because positive z goes down (i.e. z is higher when you go lower)
        return map.getSector(conn.getSectorId()).getFloorZ() * -1;
    }


    private void replaceMarkerSprite(ISectorGroup sg){
        if(!this.mustReplaceMarkerSprite) throw new IllegalStateException();
        List<Sprite> list = sg.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.ELEVATOR_CONNECTOR, sectorId);
        // List<Sprite> list = sg.getMap().findSprites(
        //         (Sprite s) -> s.getTexture() == PrefabUtils.MARKER_SPRITE_TEX
        //                 && s.getLotag() == PrefabUtils.MarkerSpriteLoTags.ELEVATOR_CONNECTOR
        //                 && s.getSectorId() == sectorId
        // );
        if(list.size() != 1) throw new SpriteLogicException("wrong number of elevator marker sprites in sector");
        Sprite markerSprite = list.get(0);
        markerSprite.setTexture(TextureList.SE);
        markerSprite.setLotag(Lotags.SE.ELEVATOR);
    }

    static void linkElevators(
            ElevatorConnector lowerConn,
            ISectorGroup lowerConnGroup,
            ElevatorConnector higherConn,
            ISectorGroup higherConnGroup,
            int hitag,
            boolean startLower
    ){
        if(lowerConn.mustReplaceMarkerSprite){
            lowerConn.replaceMarkerSprite(lowerConnGroup);
        }
        Sprite lowerSE = lowerConn.getSESprite(lowerConnGroup);
        if(higherConn.mustReplaceMarkerSprite){
            higherConn.replaceMarkerSprite(higherConnGroup);
        }
        Sprite higherSE = higherConn.getSESprite(higherConnGroup);

        if(startLower){
            // SE where elevator starts should be darker
            lowerSE.setShade(32);
            higherSE.setShade(0);
        }else{
            // TODO - some instructions say that the place where the elevator doesnt start gets
            // a sector hitag of 1
            lowerSE.setShade(0);
            higherSE.setShade(32);
        }

        lowerSE.setHiTag(hitag);
        higherSE.setHiTag(hitag);

        Sector lowerSector = lowerConnGroup.getMap().getSector(lowerConn.getSectorId());
        lowerSector.setHitag(0);

        Sector higherSector = higherConnGroup.getMap().getSector(higherConn.getSectorId());
        higherSector.setHitag(1);

        // NOTE:  positive Z goes DOWN!
        if(lowerSector.getFloorZ() <= higherSector.getFloorZ()){
            throw new SpriteLogicException("elevator sectors are the same height; elevator will run forever");
        }

        //lowerConnGroup.getMap().getSector(lowerConn.getSectorId()).setHitag(0);
        //higherConnGroup.getMap().getSector(higherConn.getSectorId()).setHitag(1);
    }
}
