package trn.duke.experiments.prefab;

import trn.*;
import trn.prefab.*;

import java.util.ArrayList;
import java.util.List;

public class MapBuilder {

    final Map outMap;

    final PrefabPalette palette;

    List<PastedSectorGroup> pastedGroups = new ArrayList<PastedSectorGroup>();

    public MapBuilder(Map outMap, PrefabPalette palette){
        this.outMap = outMap;
        this.palette = palette;
    }

    public Connector findFirstUnlinkedConnector(ConnectorFilter cf){
        for(PastedSectorGroup psg : pastedGroups){
            for(Connector c : psg.connectors){
                if(cf.matches(c) && !psg.isConnectorLinked(c)){
                    return c;
                }
            }
        }
        return null;
    }

    public PastedSectorGroup pasteSectorGroup(int sectorGroupId, PointXYZ rawTrasform){
        PastedSectorGroup psg = palette.pasteSectorGroup(sectorGroupId, this.outMap, rawTrasform);
        pastedGroups.add(psg);
        return psg;
    }

    private PastedSectorGroup add(PastedSectorGroup psg){
        this.pastedGroups.add(psg);
        return psg;
    }

    public PastedSectorGroup pasteAndLink(int sectorGroupId, Connector destConnector){
        if(1==1) throw new RuntimeException("TODO - this doesnt work");
        if(isConnectorLinked(destConnector)){
            throw new IllegalArgumentException("connector already connected");
        }

        SectorGroup sg = palette.getSectorGroup(sectorGroupId);
        Connector paletteConnector = sg.findFirstMate(destConnector);
        return add(palette.pasteAndLink(sectorGroupId, paletteConnector, outMap, destConnector));
    }

    public PastedSectorGroup pasteAndLink(
            int sectorGroupId,
            ConnectorFilter paletteConnectorFilter,
            Connector destConnector){
        SectorGroup sg = palette.getSectorGroup(sectorGroupId);
        if(isConnectorLinked(destConnector)){
            throw new IllegalArgumentException("connector already connected");
        }
        return add(palette.pasteAndLink(sg, paletteConnectorFilter, outMap, destConnector));
    }
    public PastedSectorGroup pasteAndLink(
            SectorGroup sg,
            ConnectorFilter paletteConnectorFilter,
            Connector destConnector){
        if(isConnectorLinked(destConnector)){
            throw new IllegalArgumentException("connector already connected");
        }
        return add(palette.pasteAndLink(sg, paletteConnectorFilter, outMap, destConnector));
    }

    public boolean isConnectorLinked(Connector c){

        // TODO - will not work with teleporer connectors, etc
        return this.outMap.getWall(c.getWallId()).isRedWall();
    }



    /**
     * pick a "player start" marker sprite and set the actual player start location there
     */
    public void selectPlayerStart(){
        ISpriteFilter psfilter = SpriteFilter.playerstart();
        List<Sprite> sprites = outMap.findSprites(psfilter);

        System.out.println("filter matches = " + psfilter.matches(sprites.get(0)));
        System.out.println("sprite is: " + sprites.get(0));

        if(sprites.size() != 1){
            throw new RuntimeException("wft? sprite count is " + sprites.size());
        }
        Sprite pstart = outMap.findSprites(psfilter).iterator().next();

        outMap.setPlayerStart(new PlayerStart(pstart.getLocation(),0));
    }

    public void clearMarkers(){
        outMap.deleteSprites(SpriteFilter.texture(PrefabUtils.MARKER_SPRITE_TEX));

    }
}
