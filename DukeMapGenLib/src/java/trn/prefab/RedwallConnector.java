package trn.prefab;

import trn.*;

import java.util.Collections;
import java.util.List;

// TODO - or should the main feature of this class be that it matters where the sector is?
public abstract class RedwallConnector extends Connector {

    /** the "main" sectorId of the connector, where the connector sprite is located */
    protected final int spriteSectorId;
    protected final List<Integer> allSectorIds;

    /** a point with x = min x of all wall points and y = min y of all wall points */
    protected final PointXYZ anchor;

    /** Total manhattan length */
    protected final long totalLength;

    /** First point of first wall in the sequence of walls. */
    protected final PointXY wallAnchor1;

    /** Last point of last wall in the sequence of walls */
    protected final PointXY wallAnchor2;

    protected final int markerSpriteLotag;

    protected final int connectorType;

    protected final List<Integer> wallIds;

    /** the lotag that marks walls as connector walls:  1 or 2 */
    protected final int wallMarkerLotag;

    protected RedwallConnector(
            int connectorId,
            int spriteSectorId,
            List<Integer> sectorIds,
            long totalLength,
            PointXYZ anchor,
            PointXY wallAnchor1,
            PointXY wallAnchor2,
            int markerSpriteLotag,
            int connectorType,
            List<Integer> wallIds,
            int wallMarkerLotag
    ){
        super(connectorId);
        if(anchor == null){
            throw new IllegalArgumentException("anchor cannot be null");
        }
        if(wallIds == null || wallIds.size() < 1){
            throw new IllegalArgumentException("wallIds cannot be empty");
        }
        this.spriteSectorId = spriteSectorId;
        this.allSectorIds = Collections.unmodifiableList(sectorIds);
        this.totalLength = totalLength;
        this.anchor = anchor;
        this.wallAnchor1 = wallAnchor1;
        this.wallAnchor2 = wallAnchor2;
        this.markerSpriteLotag = markerSpriteLotag;
        this.connectorType = connectorType;
        this.wallIds = Collections.unmodifiableList(wallIds);
        this.wallMarkerLotag = wallMarkerLotag;
    }
    protected RedwallConnector(
            Sprite markerSprite,
            int spriteSectorId,
            List<Integer> sectorIds,
            long totalLength,
            PointXYZ anchor,
            PointXY wallAnchor1,
            PointXY wallAnchor2,
            int markerSpriteLotag,
            int connectorType,
            List<Integer> wallIds,
            int wallMarkerLotag
    ){
        this(markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1, spriteSectorId, sectorIds, totalLength, anchor,
                wallAnchor1, wallAnchor2, markerSpriteLotag, connectorType, wallIds, wallMarkerLotag);
    }

    public abstract boolean canLink(RedwallConnector other, Map map);

    public final PointXYZ getTransformTo(RedwallConnector other) {
        if(other == null) throw new IllegalArgumentException();

        // TODO
        if(! canLink(other, null)){
            throw new SpriteLogicException("cannot link to other connector");
        }

        // if the sectors are facing each other, the anchor sides at opposite ends of the wall
        // sequence should match, because walls always go clockwise.
        return this.getAnchorPoint().getTransformTo(other.getAnchorPoint());
    }

    public final int getWallCount(){
        return this.wallIds.size();
    }

    @Override
    public abstract RedwallConnector translateIds(final IdMap idmap, PointXYZ delta, Map map);

    public BlueprintConnector toBlueprint(){
        throw new RuntimeException("not implemented yet");
    }

    public final List<Integer> getSectorIds(){
        return this.allSectorIds;
    }

    @Override
    public final short getSectorId() { // TODO this should return an int. also rename to getSpiteSectorId or getMainSectorId
        return (short)spriteSectorId;
    }


    /**
     * @returns the sum of the manhattan-distance length of each wall in the group
     */
    public final long totalManhattanLength(){
        return this.totalLength;
    }

    /**
     * @deprecated
     */
    public final long totalManhattanLength(SectorGroup sg) {
        return totalManhattanLength();
    }

    @Override
    public final int getConnectorType(){
        return this.connectorType;
    }

    // convenience method
    public final boolean isEast() {
        return getConnectorType() == ConnectorType.HORIZONTAL_EAST;
    }

    public final boolean isWest() {
        return getConnectorType() == ConnectorType.HORIZONTAL_WEST;
    }

    public final boolean isNorth(){
        return getConnectorType() == ConnectorType.VERTICAL_NORTH;
    }

    public final boolean isSouth(){
        return getConnectorType() == ConnectorType.VERTICAL_SOUTH;
    }



    /**
     * Tests if the connectors match, i.e. if they could mate.
     * The sector groups dont have to be already lined up, but there must exist
     * a transformation that will line the sectors up.
     *
     * TODO - support rotation also (or maybe not...)
     * @return
     */
    public abstract boolean isMatch(RedwallConnector c);

    /**
     * copy of isMatch() but with a more obvious name
     */
    public final boolean couldMatch(RedwallConnector c){
        return this.isMatch(c);
    }

    /**
     * @deprecated
     * The connector is on the left side of the sector, will connect to another sector to the west.
     * @return
     */
    public final boolean isWestConn(){
        return SimpleConnector.WestConnector.matches(this);
    }

    /** @deprecated */
    public final boolean isEastConn(){
        return SimpleConnector.EastConnector.matches(this);
    }

    /**
     * meant to be used for two connectors that have already been pasted, to see if they are in the same place
     * TODO - maybe this doesnt belong here (because it is specific to pasted connectors)
     */
    public final boolean isFullMatch(RedwallConnector c, Map map){
        return isMatch(c) && getTransformTo(c).equals(PointXYZ.ZERO) && !(isLinked(map) || c.isLinked(map));
    }

    @Override
    public final boolean isLinked(Map map) {
        // return map.getWall(wallId).isRedWall();
        // return true if all the walls are red walls
        Boolean linked = null;
        assert this.wallIds.size() > 0;
        for(int wallId: this.wallIds){
            boolean b = map.getWall(wallId).isRedWall();
            if(linked == null){
                linked = b;
            }else if(linked != b){
                //some are linked and some are not
                throw new SpriteLogicException("redwall connector inconsistent linkage");
            }
        }
        return linked;
    }

    public final void removeConnector(Map map) {
        //TODO - merge this with the one in SimpleConnector

        // clear the wall
        for(int wallId: this.wallIds){
            Wall w = map.getWall(wallId);
            if(w.getLotag() != wallMarkerLotag) throw new SpriteLogicException();
            w.setLotag(0);
        }

        // remove the marker sprite
        int d = map.deleteSprites((Sprite s) ->
                s.getTexture() == PrefabUtils.MARKER_SPRITE_TEX
                        && s.getSectorId() == spriteSectorId
                        && s.getLotag() == this.markerSpriteLotag
        );
        // TODO - this happens when a child connects to a parent group, and the parent groups connector
        // is in a sector with more than 1 connector
        if(d != 1) throw new SpriteLogicException("TODO");
    }

    public final PointXYZ getAnchorPoint(){
        return this.anchor;
    }

    public abstract void linkConnectors(Map map, RedwallConnector otherConn);
}
