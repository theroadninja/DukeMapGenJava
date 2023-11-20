package trn.prefab;

import trn.*;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static trn.prefab.ConnectorType.MULTI_REDWALL;

// TODO - or should the main feature of this class be that it matters where the sector is?
public class RedwallConnector extends Connector implements HasLocationXY {

    public static int WALL_LOTAG_1 = 1;
    public static int WALL_LOTAG_2 = 2;
    public static int WALL_LOTAG_3 = 3;

    static PointXY getWallAnchor1(List<Integer> wallIds, MapView map){
        return map.getWall(wallIds.get(0)).getLocation();
    }
    static PointXY getWallAnchor2(List<Integer> wallIds, MapView map){
        return map.getWall(
                map.getWall(wallIds.get(wallIds.size() - 1)).getNextWallInLoop()
        ).getLocation();
    }

    public static RedwallConnector create(
            Sprite markerSprite,
            Sector sector,
            List<Integer> wallIds,
            List<WallView> walls,
            MapView map
    ){
        PointXY wallAnchor1 = getWallAnchor1(wallIds, map);
        PointXY wallAnchor2 = getWallAnchor2(wallIds, map);
        PointXYZ anchor = RedConnUtil.getAnchor(wallIds, map).withZ(sector.getFloorZ());
        int connectorType = RedConnUtil.connectorTypeForWalls(walls);
        List<PointXY> relativeConnPoints = RedConnUtil.allRelativeConnPoints(wallIds, map, anchor, wallAnchor2);

        return new RedwallConnector(
                markerSprite,
                markerSprite.getSectorId(),
                RedConnUtil.toList(markerSprite.getSectorId(), wallIds.size()),
                RedConnUtil.totalManhattanLength(wallIds, map),
                anchor,
                wallAnchor1,
                wallAnchor2,
                markerSprite.getLotag(),
                connectorType,
                wallIds,
                walls,
                1, // TODO this might be a bug now that redwall connector walls can be 1, 2, or 3
                relativeConnPoints
        );
    }

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

    protected final List<WallView> walls;

    /**
     * TODO this is meaningless -- a single conn can have multiple lotags
     * */
    protected final int wallMarkerLotag;

    /** if this is an axis-aligned (a.k.a. compass, or SimpleConnector) then this returns the heading; otherwise -1 */
    protected final int heading;

    /**
     * The points of the walls relative to the first point, which we use to store the shape of the connector,
     * to verify whether it can fit to another connector.
     */
    protected final List<PointXY> relativePoints;

    public RedwallConnector(
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
            List<WallView> walls,
            int wallMarkerLotag,
            List<PointXY> relativePoints
    ){
        super(connectorId);
        if(anchor == null){
            throw new IllegalArgumentException("anchor cannot be null");
        }
        if(wallIds == null || wallIds.size() < 1){
            throw new IllegalArgumentException("wallIds cannot be empty");
        }
        if(wallIds.size() != sectorIds.size() || wallIds.size() != walls.size()){
            throw new IllegalArgumentException(
                    String.format("size mismatch %s %s %s", wallIds.size(), sectorIds.size(), wallIds.size())
            );
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
        this.walls = walls;
        this.wallMarkerLotag = wallMarkerLotag;
        this.relativePoints = Collections.unmodifiableList(relativePoints);

        this.heading = RedConnUtil.headingForConnectorType(this.connectorType);
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
            List<WallView> walls,
            int wallMarkerLotag,
            List<PointXY> relativePoints
    ){
        this(markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1, spriteSectorId, sectorIds, totalLength, anchor,
                wallAnchor1, wallAnchor2, markerSpriteLotag, connectorType, wallIds, walls, wallMarkerLotag, relativePoints);
    }

    @Override
    public PointXY getLocationXY(){
        return this.getAnchorPoint().asXY();
    }

    public int getHeading(){
        return this.heading;
    }


    public final PointXYZ getTransformTo(RedwallConnector other) {
        if(other == null) throw new IllegalArgumentException();

        // TODO
        if(! canLink(other, null)){
            throwOnLinkFailure(other);
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
    public final RedwallConnector translateIds(final IdMap idmap, PointXYZ delta, MapView map){
        List<Integer> newWallIds = idmap.wallIds(this.wallIds);
        List<WallView> newWalls = MapUtil.getWallViews(newWallIds, map);
        return new RedwallConnector(
                this.connectorId,
                idmap.sector(this.spriteSectorId),
                idmap.sectorIds(this.allSectorIds),
                this.totalLength,
                this.anchor.add(delta),
                this.wallAnchor1.add(delta.asXY()),
                this.wallAnchor2.add(delta.asXY()),
                this.markerSpriteLotag,
                this.connectorType,
                newWallIds,
                newWalls,
                this.wallMarkerLotag,
                this.relativePoints
        );
    }

    /**
     * Returns a shallow copy of this Redwall Conn with the z coord of its anchor changed (the anchor that
     * is used when pasting and linking sector groups by their redwall conns.
     *
     * Since this changes the ANCHOR, it will have the opposite effect on the
     * final paste (e.g. moving the anchor up will cause the pasted group to
     * be lower) however also keep in mind that Build coordinates are reversed,
     * and positive Z goes down.  So: passing a negative number to this will make
     * the resulting group lower, and a positive number will make it higher.
     *
     * @param deltaZ amount to add to the z coordinate of this redwall conn's anchor (positive amounts
     *                 will cause the resulting pasted SG to be higher)
     * @returns a copy of this RedwallAnchor, but with the z coordinate of its anchor changed by `detlaZ`
     */
    public final RedwallConnector withAnchorZAdjusted(final int deltaZ){
        return new RedwallConnector(
                this.connectorId,
                this.spriteSectorId,
                this.allSectorIds,
                this.totalLength,
                this.anchor.add(new PointXYZ(0, 0, deltaZ)),
                this.wallAnchor1,
                this.wallAnchor2,
                this.markerSpriteLotag,
                this.connectorType,
                this.wallIds,
                this.walls,
                this.wallMarkerLotag,
                this.relativePoints
        );
    }

    /** @deprecated */
    public final RedwallConnector translateIds(final IdMap idmap, PointXYZ delta, Map map){
        return translateIds(idmap, delta, new MapView(map));
    }

    public BlueprintConnector toBlueprint(){
        throw new RuntimeException("not implemented yet");
    }

    public final List<Integer> getWallIds(){
        return Collections.unmodifiableList(this.wallIds);
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

    // TODO this is leftover from when "east" and "west" connectors were different "types" of connectors.
    @Override
    public final int getConnectorType(){
        return this.connectorType;
    }

    @Override
    public boolean isTeleporter() {
        return false;
    }

    @Override
    public boolean isElevator() {
        return false;
    }

    @Override
    public boolean isRedwall() {
        return true;
    }

    // convenience method
    public final boolean isEast() {
        return this.heading == Heading.E;
    }

    public final boolean isWest() {
        return this.heading == Heading.W;
    }

    public final boolean isNorth(){
        return this.heading == Heading.N;
    }

    public final boolean isSouth(){
        return this.heading == Heading.S;
    }

    public final boolean isCompassConn(int heading){
        if(heading == Heading.E){
            return this.isEast();
        }else if(heading == Heading.S){
            return this.isSouth();
        }else if(heading == Heading.W){
            return this.isWest();
        }else if(heading == Heading.N){
            return this.isNorth();
        }else{
            throw new IllegalArgumentException("invalid heading: " + heading);
        }
    }

    public final boolean isAxisAligned(){
        return (heading == Heading.E || heading == Heading.W || heading == Heading.N || heading == Heading.S);
    }


    /** temp, just to east the transition from having SimpleConnector vs MultiWallConnector */
    private boolean isSimpleConnector(){
        if(getConnectorType() == MULTI_REDWALL) {
            return false;
        }else if(getConnectorType() == ConnectorType.MULTI_SECTOR){
            return false;
        }else if(heading == -1){
            throw new RuntimeException("this should never happen");
        }else{
            return true;
        }

    }

    public BoundingBox getBoundingBox(){
        PointXY p = walls.get(0).p1();
        int minx = p.x;
        int miny = p.y;
        int maxx = minx;
        int maxy = miny;

        for(WallView wall: walls){
            minx = Math.min(minx, wall.p1().x);
            minx = Math.min(minx, wall.p2().x);
            maxx = Math.max(maxx, wall.p1().x);
            maxx = Math.max(maxx, wall.p2().x);

            miny = Math.min(miny, wall.p1().y);
            miny = Math.min(miny, wall.p2().y);
            maxy = Math.max(maxy, wall.p1().y);
            maxy = Math.max(maxy, wall.p2().y);
        }
        return new BoundingBox(minx, miny, maxx, maxy);
    }


    /**
     * throws if the other conn is not a match, for debugging
     */
    public final void expectMatch(RedwallConnector other){
        if(isSimpleConnector()){
            if(! other.isSimpleConnector()){
                throw new RuntimeException("one connector is simple, the other isnt");
            }
            if(!isSimpleMatch(other)){
                throw new RuntimeException("isSimpleMatch failed");
            }
        }else{
            // TODO - this has code duplicated from canLink()
            if(this.wallIds.size() != other.wallIds.size()){
                throw new RuntimeException(String.format("wall size mismatch %s ! %s", this.wallIds.size(), other.wallIds.size()));
            }
            PointXY p1 = this.wallAnchor1.subtractedBy(this.anchor);
            PointXY p2 = this.wallAnchor2.subtractedBy(this.anchor);
            PointXY otherP1 = other.wallAnchor1.subtractedBy(other.anchor);
            PointXY otherP2 = other.wallAnchor2.subtractedBy(other.anchor);
            if(! p1.equals(otherP2)) throw new RuntimeException(String.format("anchor matching failed A %s != %s", p1, otherP2));
            if(! p2.equals(otherP1)) throw new RuntimeException("anchor matching failed B");

            List<PointXY> list1 = this.relativePoints;
            List<PointXY> list2 = other.relativePoints;
            if(! list1.get(0).equals(p1)) throw new RuntimeException();
            if(! list2.get(0).equals(otherP1)) throw new RuntimeException();
            if(list1.size() != list2.size()) throw new RuntimeException();

            for(int i = 0; i < list1.size(); ++i){
                int j = list2.size() - 1 - i;
                if(! list1.get(i).equals(list2.get(j))){
                    // System.out.println("no match: " + list1.get(i).toString() + "," + list2.get(i).toString());
                    // System.out.println("i=" + i + " j=" + j);
                    throw new RuntimeException("relative points failed");
                }
            }

        }
    }

    private boolean isSimpleMatch(RedwallConnector c){
        return Heading.opposite(this.heading) == c.heading && this.getWallCount() == c.getWallCount()
                && this.totalLength == c.totalLength;
    }

    /**
     * Tests if the connectors match, i.e. if they could mate.
     * The sector groups dont have to be already lined up, but there must exist
     * a transformation that will line the sectors up.
     *
     * TODO - support rotation also (or maybe not...)
     * @return
     */
    public final boolean isMatch(RedwallConnector c){
        if(isSimpleConnector()){
            if(! c.isSimpleConnector()){
                return false;
            }
            return isSimpleMatch(c);
        }else{
            RedwallConnector other = c; // TODO cleanup

            // TODO - this has code duplicated from canLink()
            if(this.wallIds.size() != other.wallIds.size()){
                return false;
            }
            PointXY p1 = this.wallAnchor1.subtractedBy(this.anchor);
            PointXY p2 = this.wallAnchor2.subtractedBy(this.anchor);
            PointXY otherP1 = other.wallAnchor1.subtractedBy(other.anchor);
            PointXY otherP2 = other.wallAnchor2.subtractedBy(other.anchor);
            if(! p1.equals(otherP2)) return false;
            if(! p2.equals(otherP1)) return false;

            List<PointXY> list1 = this.relativePoints;
            List<PointXY> list2 = other.relativePoints;
            if(! list1.get(0).equals(p1)) throw new RuntimeException();
            if(! list2.get(0).equals(otherP1)) throw new RuntimeException();
            if(list1.size() != list2.size()) throw new RuntimeException();

            for(int i = 0; i < list1.size(); ++i){
                int j = list2.size() - 1 - i;
                if(! list1.get(i).equals(list2.get(j))){
                    // System.out.println("no match: " + list1.get(i).toString() + "," + list2.get(i).toString());
                    // System.out.println("i=" + i + " j=" + j);
                    return false;
                }
            }
            return true;
        }
    }

    /**
     * copy of isMatch() but with a more obvious name
     *
     * If you are looking for couldMatchIfRotated() it doesnt exist; see trn.prefab.experiments.Placement
     */
    public final boolean couldMatch(RedwallConnector c){
        return this.isMatch(c);
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
        if(d != 1) throw new SpriteLogicException("TODO sprites deleted=" + d + " expected lotag=" + this.markerSpriteLotag);
    }

    public final PointXYZ getAnchorPoint(){
        return this.anchor;
    }

    @Override
    public String toString(){
        StringBuilder sb = new StringBuilder();
        sb.append("{ connector\n");
        sb.append(" sectorId: ").append(spriteSectorId).append("\n");
        //sb.append(" wallId: ").append(wallId).append("\n");
        return sb.toString();
    }


    // TODO see also expectMatch
    private void throwOnLinkFailure(RedwallConnector other){
        // TODO clean this up / better error messages
        // (for now I'm just duplicating the logic inside canLink())
        if(this.wallIds.size() != other.wallIds.size()){
            String msg = String.format(
                    "cannot link to other connector; wall sizes dont match %s != %s",
                    this.wallIds.size(),
                    other.wallIds.size()
            );
            throw new SpriteLogicException(msg);
        }
        if(this.totalManhattanLength() != other.totalManhattanLength()){
            throw new SpriteLogicException("cannot link to other connector; total length is off");
        }
        PointXY p1 = this.wallAnchor1.subtractedBy(this.anchor);
        PointXY p2 = this.wallAnchor2.subtractedBy(this.anchor);

        PointXY otherP1 = other.wallAnchor1.subtractedBy(other.anchor);
        PointXY otherP2 = other.wallAnchor2.subtractedBy(other.anchor);
        if(! p1.equals(otherP2)) throw new SpriteLogicException(("p1 != otherP2 (did you forget to rotate the SG?"));
        if(! p2.equals(otherP1)) throw new SpriteLogicException(("p2 != otherP1"));

        // TODO a helpful test would be to throw all wall sizes into a set or a sorted list, and report any diffs

    }
    public final boolean canLink(RedwallConnector other, Map map) {
        // this was added while consolidating SimpleConnector, MultiWall and MultiSector into RedwallConnector
        if(isSimpleConnector()){
            return isMatch(other);
        }else{
            //MultiWallConnector other = (MultiWallConnector)other0;
            if(this.wallIds.size() != other.wallIds.size()){
                return false;
            }
            PointXY p1 = this.wallAnchor1.subtractedBy(this.anchor);
            PointXY p2 = this.wallAnchor2.subtractedBy(this.anchor);

            PointXY otherP1 = other.wallAnchor1.subtractedBy(other.anchor);
            PointXY otherP2 = other.wallAnchor2.subtractedBy(other.anchor);

            // check each point
            if(map != null){

                // 1. are any of the walls redwalls?
                List<Integer> tmp = new ArrayList<>(this.wallIds);
                tmp.addAll(other.wallIds);
                for(int id: tmp){
                    if(map.getWall(id).isRedWall()){
                        throw new SpriteLogicException("wall " + id + " is already a red wall");
                    }
                }

                // 2. are the walls lined up correclty?
                List<PointXY> list1 = this.relativePoints;
                List<PointXY> list2 = other.relativePoints;
                if(! list1.get(0).equals(p1)) throw new RuntimeException();
                if(! list2.get(0).equals(otherP1)) throw new RuntimeException();
                if(list1.size() != list2.size()) throw new RuntimeException();

                for(int i = 0; i < list1.size(); ++i){
                    int j = list2.size() - 1 - i;
                    if(! list1.get(i).equals(list2.get(j))){
                        return false;
                    }
                }
            }
            if(! p1.equals(otherP2)) return false;
            if(! p2.equals(otherP1)) return false;
            return true;

        }
    }

    public final void linkConnectors(Map map, RedwallConnector otherConn){
        if(map == null || otherConn == null) throw new IllegalArgumentException("argument is null");

        // TODO - should be able to use the same logic for both
        if(isSimpleConnector()){
            if(! otherConn.isSimpleConnector()) throw new IllegalArgumentException("connectors are not a match");
            if(otherConn.isLinked(map)) throw new IllegalArgumentException("connector is already linked");
            if(this.isLinked(map)) throw new IllegalStateException("already linked");

            //map.linkRedWalls(this.getSectorId(), wallIds.get(0), otherConn.getSectorId(), otherConn.wallIds.get(0));
            map.linkRedWalls(allSectorIds.get(0), wallIds.get(0), otherConn.allSectorIds.get(0), otherConn.wallIds.get(0));
        }else{
            if(otherConn.getConnectorType() != this.getConnectorType()){
                // public static int MULTI_REDWALL = 21;
                // public static int MULTI_SECTOR = 22;
                int t1 = getConnectorType(), t2 = otherConn.getConnectorType();
                if((t1 == 21 || t1 == 22) && (t2 == 21 || t2 == 22)){
                    // lets try allowing a multi-sector to connect to a multi-wall
                }else{
                    throw new IllegalArgumentException(String.format("connector type mismatch %s != %s", getConnectorType(), otherConn.getConnectorType()));
                }
            }
            //MultiWallConnector c2 = (MultiWallConnector)otherConn;
            if(! canLink(otherConn, map)) {
                throwOnLinkFailure(otherConn);
                throw new SpriteLogicException("cannot link connector (other=" + otherConn.getConnectorId() + ")");
            }

            // i think we just link up the walls in reverse order
            for(int i = 0; i < this.wallIds.size(); ++i){
                int j = this.wallIds.size() - 1 - i;

                //map.linkRedWalls(this.getSectorId(), this.wallIds.get(i), otherConn.getSectorId(), otherConn.wallIds.get(j));
                map.linkRedWalls(this.allSectorIds.get(i), this.wallIds.get(i), otherConn.allSectorIds.get(j), otherConn.wallIds.get(j));
            }

        }
    }
}
