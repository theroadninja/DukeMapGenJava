package trn.prefab;

import trn.*;
import trn.duke.MapErrorException;
import trn.duke.TextureList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;


// TODO - maybe this should turn into the simple ordinal (North, South, East, West) abstraction
// TODO - on top of the redwall connector, which could use a wall of any angle.
public class SimpleConnector extends RedwallConnector {

	public int getWallId(){
		return this.wallIds.get(0);
	}

	public static SimpleConnector createSimpleConnector(Sprite markerSprite, Sector sector, int wallId, Map map) throws MapErrorException {
		int z = sector.getFloorZ();
		int heading = -1;
		int connectorType = -1;
		PointXYZ anchorPoint = null;
		Wall wall = map.getWall(wallId);
		WallView wallView = map.getWallView(wallId);
		Wall nextWallInLoop = map.getWall(wall.getPoint2Id());

		anchorPoint = RedConnUtil.getAnchor(RedConnUtil.toList(wallId), map).withZ(z);
		connectorType = RedConnUtil.connectorTypeForWall(wallView);
		//heading = headingForConnectorType(connectorType);
		return new SimpleConnector(markerSprite, sector, wallId, wallView, map, connectorType, anchorPoint, wall.getLocation(), nextWallInLoop.getLocation());
	}

	private SimpleConnector(
			Sprite markerSprite,
			Sector sector,
			int wallId,
			WallView wall,
			Map map,
			int connectorType,
			PointXYZ anchorPoint,
			PointXY wallAnchor1,
			PointXY wallAnchor2
	) {
        super(
        		markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1,
				markerSprite.getSectorId(),
				RedConnUtil.toList(markerSprite.getSectorId()),
				RedConnUtil.totalManhattanLength(RedConnUtil.toList(wallId), map), //wallLength(wallId, map),
				anchorPoint,
                wallAnchor1,
				wallAnchor2,
				markerSprite.getLotag(),
				connectorType,
				RedConnUtil.toList(wallId),
				new ArrayList<WallView>(){{ add(wall); }},
				1,
				Collections.emptyList()
		);
        //this.heading = heading;
    }

    public static boolean isSimpleConnector(List<Integer> linkWallIds, Map map){
	    if(linkWallIds.size() != 1){
	    	return false;
		}
		int wallId = linkWallIds.get(0);
		Wall wall = map.getWall(wallId);
		Wall nextWallInLoop = map.getWall(wall.getPoint2Id());
		PointXY vector = wall.getUnitVector(nextWallInLoop);
		return Math.abs(vector.x) == 1 || Math.abs(vector.y) == 1;
	}

	private SimpleConnector(
			int connectorId,
			int sectorId,
			List<Integer> wallIds,
			List<WallView> walls,
			int connectorType,
			PointXYZ anchorPoint,
			PointXY wallAnchor1,
			PointXY wallAnchor2,
			int markerSpriteLotag,
			long totalLength
	){
	    super(connectorId, sectorId, RedConnUtil.toList(sectorId), totalLength,
				anchorPoint, wallAnchor1, wallAnchor2, markerSpriteLotag, connectorType, wallIds, walls, 1,
				Collections.emptyList()
				);
        //this.wallId = wallId;
	}

	@Override
	public SimpleConnector translateIds(final IdMap idmap, PointXYZ delta, Map map){
		List<Integer> newWallIds = idmap.wallIds(this.wallIds);
		List<WallView> newWalls = MapUtil.getWallViews(newWallIds, map);
		return new SimpleConnector(
				this.getConnectorId(),
				idmap.sector(spriteSectorId),
				newWallIds,
                newWalls,
                this.connectorType,
				this.anchor,
				this.wallAnchor1,
				this.wallAnchor2,
				this.markerSpriteLotag,
				this.totalLength);
	}

	/**
	 * Tests if the connectors match, i.e. if they could mate.
	 * The sector groups dont have to be already lined up, but there must exist
	 * a transformation that will line the sectors up.
	 *
	 * The connectors rotation must match.
	 * The connectors positions do not need to match.
	 * The connectors link status does not matter.
	 */
	//@Override
	//public boolean isMatch(RedwallConnector c){
	//	if(!(c instanceof SimpleConnector)){
	//		return false;
	//	}
	//	SimpleConnector sc = (SimpleConnector)c;
	//	return Heading.opposite(this.heading) == sc.heading && this.getWallCount() == sc.getWallCount()
	//			&& this.totalLength == sc.totalLength;
	//}

	@Override
	public boolean canLink(RedwallConnector other, Map map) {
		// this was added while consolidating SimpleConnector, MultiWall and MultiSector into RedwallConnector
		return isMatch(other);
	}

	//@Override
	//public void linkConnectors(Map map, RedwallConnector other) {
	//	if(other.isLinked(map)) throw new IllegalArgumentException("connector is already linked");
	//	if(this.isLinked(map)) throw new IllegalStateException("already linked");
	//    SimpleConnector c2 = (SimpleConnector)other;
	//	map.linkRedWalls(this.getSectorId(), this.getWallId(), c2.getSectorId(), c2.getWallId());
	//}

}