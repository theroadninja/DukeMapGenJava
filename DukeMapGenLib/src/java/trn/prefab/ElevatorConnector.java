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

    public ElevatorConnector(Sprite markerSprite){
        super(Connector.idOf(markerSprite));
        sectorId = markerSprite.getSectorId();
    }

    private ElevatorConnector(int connectorId, int sectorId){
        super(connectorId);
        this.sectorId = sectorId;
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
    public ElevatorConnector translateIds(IdMap idmap) {
        return new ElevatorConnector(connectorId, idmap.sector(this.sectorId));
    }

    /**
     * Uses hitag of SE sprite to determine if it is linked.
     */
    @Override
    public boolean isLinked(Map map) {
        List<Sprite> list = getSESprites(map);
        if(list.size() != 1){
            throw new SpriteLogicException("wrong number of SE 17 sprites in teleporter sector.");
        }
        return list.get(0).getHiTag() != 0;
    }

    @Override
    public boolean hasXYRequirements() {
        return false;  // NOTE:  it DOES have z requirements though ...
    }

    /**
     * @return the actual SE 17 sprite used to make the elevator work
     */
    public Sprite getSESprite(ISectorGroup sg){
        List<Sprite> list = getSESprites(sg.getMap());
        if(list.size() != 1) throw new SpriteLogicException("too many elevator sprites in sector");
        return list.get(0);
    }

    private List<Sprite> getSESprites(Map map){
        return map.findSprites(TextureList.SE, Lotags.SE.ELEVATOR, sectorId);
    }

    public static boolean isElevatorMarker(Map map, Sprite markerSprite){
        int sectorId = markerSprite.getSectorId();
        if(map.getSector(sectorId).getLotag() != Lotags.ST.ELEVATOR){
            return false;
        }
        return map.findSprites(TextureList.SE, Lotags.SE.ELEVATOR, sectorId).size() > 0;
    }


    public static void linkElevators(
            ElevatorConnector lowerConn,
            ISectorGroup lowerConnGroup,
            ElevatorConnector higherConn,
            ISectorGroup higherConnGroup,
            int hitag,
            boolean startLower
    ){
        Sprite lowerSE = lowerConn.getSESprite(lowerConnGroup);
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
