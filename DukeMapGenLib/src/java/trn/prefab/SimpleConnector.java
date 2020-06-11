package trn.prefab;

import trn.*;
import trn.duke.MapErrorException;
import trn.duke.TextureList;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;


// TODO - maybe this should turn into the simple ordinal (North, South, East, West) abstraction
// TODO - on top of the redwall connector, which could use a wall of any angle.
public class SimpleConnector extends RedwallConnector {

	public static PointXYZ getHorizontalConnectorAnchor(Wall w1, Wall w2, int z){
		// make sure its a VERTICAL line
		if(0 != w1.getUnitVector(w2).x){
			throw new IllegalArgumentException();
		}
		PointXY p1 = new PointXY(w1);
		PointXY p2 = new PointXY(w2);
		if(p1.x != p2.x){
			throw new IllegalArgumentException();
		}
		return new PointXYZ(p1.x, Math.min(p1.y, p2.y), z);
	}

	public static PointXYZ getVerticalConnectorAnchor(Wall w1, Wall w2, int z){
		// make sure its a HORIZONTAL line
		if(0 != w1.getUnitVector(w2).y){
			throw new IllegalArgumentException();
		}
		PointXY p1 = new PointXY(w1);
		PointXY p2 = new PointXY(w2);
		if(p1.y != p2.y){
			throw new SpriteLogicException();
		}
		return new PointXYZ(
				Math.min(p1.x, p2.x),
				p1.y,
				z);
	}

	public static List<Integer> toList(int element){
		return new ArrayList<Integer>(){{
			add(element);
		}};
	}

	public static int connectorTypeForHeading(int heading){
    	if(Heading.E == heading){
    		return ConnectorType.HORIZONTAL_EAST;
		}else if(Heading.W == heading){
    		return ConnectorType.HORIZONTAL_WEST;
		}else if(Heading.N == heading){
    		return ConnectorType.VERTICAL_NORTH;
		}else if(Heading.S == heading){
    		return ConnectorType.VERTICAL_SOUTH;
		}else{
    		throw new IllegalArgumentException();
		}
	}

	public static int headingForConnectorType(int connectorType){
		if(connectorType == ConnectorType.VERTICAL_SOUTH){
			return Heading.S;
		}else if(connectorType == ConnectorType.VERTICAL_NORTH){
			return Heading.N;
		}else if(connectorType == ConnectorType.HORIZONTAL_EAST){
			return Heading.E;
		}else if(connectorType == ConnectorType.HORIZONTAL_WEST){
			return Heading.W;
		}else{
			throw new IllegalArgumentException("Invalid SimpleConnector type: " + connectorType);
		}
	}

	public static ConnectorFilter SouthConnector = new ConnectorTypeFilter(ConnectorType.VERTICAL_SOUTH);
	public static ConnectorFilter NorthConnector = new ConnectorTypeFilter(ConnectorType.VERTICAL_NORTH);
	public static ConnectorFilter EastConnector = new ConnectorTypeFilter(ConnectorType.HORIZONTAL_EAST);
	public static ConnectorFilter WestConnector = new ConnectorTypeFilter(ConnectorType.HORIZONTAL_WEST);

	final int wallId;
	final int heading;

	public int getWallId(){
		return this.wallId;
	}

	public static SimpleConnector createSimpleConnector(Sprite markerSprite, Sector sector, int wallId, Map map) throws MapErrorException {
		int z = sector.getFloorZ();
		int heading = -1;
		int connectorType = -1;
		PointXYZ anchorPoint = null;
		Wall wall = map.getWall(wallId);
		Wall nextWallInLoop = map.getWall(wall.getPoint2Id());

		PointXY vector = wall.getUnitVector(nextWallInLoop);
		if(vector.x == 1) {
			// horizontal wall, vertical connector
			connectorType = ConnectorType.VERTICAL_NORTH;
			//heading = Heading.N;
			anchorPoint = SimpleConnector.getVerticalConnectorAnchor(wall, nextWallInLoop, z);
		}else if(vector.x == -1){
			connectorType = ConnectorType.VERTICAL_SOUTH;
			//heading = Heading.S;
			anchorPoint = SimpleConnector.getVerticalConnectorAnchor(wall, nextWallInLoop, z);
		}else if(vector.y == 1){ // y is pointed down
			// vertical wall, horizontal connector
			connectorType = ConnectorType.HORIZONTAL_EAST;
			//heading = Heading.E;
			anchorPoint = SimpleConnector.getHorizontalConnectorAnchor(wall, nextWallInLoop, z);
		}else if(vector.y == -1){ // y is pointed up
			connectorType = ConnectorType.HORIZONTAL_WEST;
			//heading = Heading.W;
			anchorPoint = SimpleConnector.getHorizontalConnectorAnchor(wall, nextWallInLoop, z);
		}else{
			throw new MapErrorException("connector wall must be horizontal or vertical");
		}
		heading = headingForConnectorType(connectorType);
		return new SimpleConnector(markerSprite, sector, wallId, map, connectorType, heading, anchorPoint, wall.getLocation(), nextWallInLoop.getLocation());
	}

	private SimpleConnector(
			Sprite markerSprite,
			Sector sector,
			int wallId,
			Map map,
			int connectorType,
			int heading,
			PointXYZ anchorPoint,
			PointXY wallAnchor1,
			PointXY wallAnchor2
	) {
        super(
        		markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1,
				markerSprite.getSectorId(),
				toList(markerSprite.getSectorId()),
				MultiWallConnector.totalManhattanLength(toList(wallId), map), //wallLength(wallId, map),
				anchorPoint,
                wallAnchor1,
				wallAnchor2,
				markerSprite.getLotag(),
				connectorType,
				toList(wallId),
				1
		);
        this.wallId = wallId;
        this.heading = heading;
    }

    public int getHeading(){
		return this.heading;
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
			int wallId,
			int connectorType,
			PointXYZ anchorPoint,
			PointXY wallAnchor1,
			PointXY wallAnchor2,
			int markerSpriteLotag,
			//long length,
			//BlueprintConnector bp,
			long totalLength
	){
	    super(connectorId, sectorId, toList(sectorId), totalLength, anchorPoint, wallAnchor1, wallAnchor2, markerSpriteLotag, connectorType, toList(wallId), 1);
        this.wallId = wallId;
        this.heading = headingForConnectorType(this.connectorType);
	}

	@Override
	public SimpleConnector translateIds(final IdMap idmap, PointXYZ delta, Map map){
		return new SimpleConnector(this.connectorId,
				idmap.sector(spriteSectorId),
				idmap.wall(this.wallId),
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
	 *
	 * The connectors positions do not need to match.
	 *
	 * The connectors link status does not matter.
	 *
	 * TODO - support rotation also?
	 * @return
	 */
	@Override
	public boolean isMatch(RedwallConnector c){
		if(!(c instanceof SimpleConnector)){
			return false;
		}
		SimpleConnector sc = (SimpleConnector)c;

		return Heading.opposite(this.heading) == sc.heading && this.getWallCount() == sc.getWallCount()
				&& this.totalLength == sc.totalLength;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("{ connector\n");
		//sb.append("  x: ").append(x).append("\n");
		//sb.append(" ymin: ").append(ymin).append("\n");
		//sb.append(" ymax: ").append(ymax).append("\n");
		sb.append(" sectorId: ").append(spriteSectorId).append("\n");
		sb.append(" wallId: ").append(wallId).append("\n");
		//sb.append(" wall nextSector: ").append(wall.nextSector).append("\n");
		return sb.toString();
	}

	@Override
	public boolean canLink(RedwallConnector other, Map map) {
		// this was added while consolidating SimpleConnector, MultiWall and MultiSector into RedwallConnector
		return isMatch(other);
	}

	@Override
	public void linkConnectors(Map map, RedwallConnector other) {
		if(other.isLinked(map)) throw new IllegalArgumentException("connector is already linked");
		if(this.isLinked(map)) throw new IllegalStateException("already linked");
	    SimpleConnector c2 = (SimpleConnector)other;
		map.linkRedWalls(this.getSectorId(), this.getWallId(), c2.getSectorId(), c2.getWallId());
	}

	public static void linkConnectors(SimpleConnector c1, SimpleConnector c2, Map map){
		map.linkRedWallsStrict(c1.getSectorId(), c1.getWallId(), c2.getSectorId(), c2.getWallId());
	}

}