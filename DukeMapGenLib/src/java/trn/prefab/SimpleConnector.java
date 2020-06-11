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


	final int sectorId;
	final int wallId;
	final int connectorType;
	final int heading;
	final List<Integer> allSectorIds;

	/** length of the wall of this connector */
	final long length;

	// could be 20, or the specific type
	final int markerSpriteLotag;

	final BlueprintConnector blueprint;

	final long totalLength;

	public int getWallId(){
		return this.wallId;
	}
	
	// used for horizontal connector
	// int x;
	// int ymin;
	// int ymax;
	PointXYZ anchorPoint = null;
	int z;


	// // TODO - get rid of this?
	// SimpleConnector(Sprite markerSprite, int wallId, Wall wall, long length){
	// 	super(markerSprite);
	// 	//super(markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1);
	// 	if(markerSprite == null){
	// 		throw new IllegalArgumentException("markerSprite is null");
	// 	}
	// 	if(markerSprite.getTexture() != PrefabUtils.MARKER_SPRITE_TEX){
	// 		throw new IllegalArgumentException();
	// 	}
    //     this.connectorType = markerSprite.getLotag();
	// 	this.heading = headingForConnectorType(this.connectorType);
	// 	this.sectorId = markerSprite.getSectorId();
	// 	if(this.sectorId < 0){
	// 		throw new RuntimeException("SimpleConnector sectorId cannot be < 0");
	// 	}
	// 	this.wallId = wallId;
	// 	this.length = length;
	// 	this.markerSpriteLotag = markerSprite.getLotag();
	// }
	
	// SimpleConnector(Sprite markerSprite, int wallId, Wall wall, Sector sector){
	// 	this(markerSprite, wallId, wall);
	// 	this.z = sector.getFloorZ();
	// }

	public SimpleConnector(Sprite markerSprite, Sector sector, int wallId, Map map)
			throws MapErrorException {
        super(markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1);
		int z = sector.getFloorZ();
        this.wallId = wallId;
        this.length = wallLength(this.wallId, map);
        this.sectorId = markerSprite.getSectorId();
        this.allSectorIds = new ArrayList(1);
        this.allSectorIds.add(this.sectorId);
		Wall wall = map.getWall(wallId);
		Wall nextWallInLoop = map.getWall(wall.getPoint2Id());

        PointXY vector = wall.getUnitVector(nextWallInLoop);
        if(vector.x == 1) {
            // horizontal wall, vertical connector
            this.connectorType = ConnectorType.VERTICAL_NORTH;
            this.heading = Heading.N;
            this.setAnchorPoint(SimpleConnector.getVerticalConnectorAnchor(wall, nextWallInLoop, z));
        }else if(vector.x == -1){
            this.connectorType = ConnectorType.VERTICAL_SOUTH;
            this.heading = Heading.S;
            this.setAnchorPoint(SimpleConnector.getVerticalConnectorAnchor(wall, nextWallInLoop, z));
        }else if(vector.y == 1){ // y is pointed down
            // vertical wall, horizontal connector
            this.connectorType = ConnectorType.HORIZONTAL_EAST;
            this.heading = Heading.E;
            this.setAnchorPoint(SimpleConnector.getHorizontalConnectorAnchor(wall, nextWallInLoop, z));
        }else if(vector.y == -1){ // y is pointed up
            this.connectorType = ConnectorType.HORIZONTAL_WEST;
            this.heading = Heading.W;
            this.setAnchorPoint(SimpleConnector.getHorizontalConnectorAnchor(wall, nextWallInLoop, z));
        }else{
            throw new MapErrorException("connector wall must be horizontal or vertical");
        }

        this.markerSpriteLotag = markerSprite.getLotag();

        this.blueprint = BlueprintConnector.apply(new BlueprintWall(wall.getLocation(), nextWallInLoop.getLocation()));

        this.totalLength = totalManhattanLength(map);
    }

    public int getHeading(){
		return this.heading;
	}
	@Override
	public Integer getSimpleHeading(){
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



	private SimpleConnector(int connectorId, int sectorId, int wallId, int connectorType, int markerSpriteLotag, long length, BlueprintConnector bp,
							long totalLength){
	    super(connectorId);
		//TODO add more fields ...
        this.sectorId = sectorId;
		this.allSectorIds = new ArrayList(1);
		this.allSectorIds.add(this.sectorId);
        this.wallId = wallId;
        this.connectorType = connectorType;
        this.heading = headingForConnectorType(this.connectorType);
        this.markerSpriteLotag = markerSpriteLotag;
        this.length = length;
        this.blueprint = bp;
        this.totalLength = totalLength;
	}

	@Override
	public SimpleConnector translateIds(final IdMap idmap, PointXYZ delta, Map map){
		return new SimpleConnector(this.connectorId,
				idmap.sector(this.sectorId),
				idmap.wall(this.wallId),
                this.connectorType,
				this.markerSpriteLotag,
				this.length,
				this.blueprint,
				this.totalLength);
	}

	public static long wallLength(int wallId, Map map){
		Wall w1 = map.getWall(wallId);
		Wall w2 = map.getWall(w1.getNextWallInLoop());
		return w1.getLocation().manhattanDistanceTo(w2.getLocation());
	}

	@Override
	public BlueprintConnector toBlueprint(){
		return this.blueprint;

	}

	@Override
	public long totalManhattanLength(){
		return this.totalLength;
	}

	public long totalManhattanLength(Map map){
		return wallLength(this.wallId, map);
	}

	/**
	 * Remove this connector: delete the marker sprite and clear the wall lotag
	 * @param map
	 */
	@Override
	public void removeConnector(Map map) {
	    // clear the wall
	    Wall w = map.getWall(this.wallId);
	    if(w.getLotag() != 1) throw new SpriteLogicException();
	    w.setLotag(0);

	    // remove the marker sprite
		int d = map.deleteSprites((Sprite s) ->
			s.getTexture() == PrefabUtils.MARKER_SPRITE_TEX
					&& s.getSectorId() == sectorId
					&& s.getLotag() == markerSpriteLotag
		);
		if(d != 1) throw new SpriteLogicException();
	}


	@Override
	public boolean isLinked(Map map){
		return map.getWall(wallId).isRedWall();
	}

	@Override
	public int getConnectorType() {
		return this.connectorType;
	}


	public void setAnchorPoint(PointXYZ anchor){
		this.anchorPoint = anchor;
	}

	@Override
	public PointXYZ getAnchorPoint(){
		if(this.anchorPoint == null) throw new SpriteLogicException();
		return this.anchorPoint;
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
		    //System.out.println("NOT A SIMPLE CONNECTOR");
			return false;
		}
		SimpleConnector sc = (SimpleConnector)c;

		//System.out.println("" + this.heading + " vs " + sc.heading);
		//System.out.println("\t" + this.totalManhattanLength(map) + " vs " + sc.totalManhattanLength(map));

		return Heading.opposite(this.heading) == sc.heading
				&& this.length == sc.length;
	}

	/*
	public void setVerticalLinePoints(PointXY p1, PointXY p2){
		if(p1.x != p2.x){
			throw new IllegalArgumentException();
		}
		this.x = p1.x;
		this.ymin = Math.min(p1.y, p2.y);
		this.ymax = Math.max(p1.y, p2.y);
		if(this.ymin == this.ymax){
			throw new IllegalArgumentException();
		}
		
	}*/

	// @Override
	// public boolean canMate(Connector c){
	//     throw new RuntimeException("TODO - this doesnt work");
	//     /*
	// 	int x = Math.min(this.sprite.getLotag(), c.sprite.getLotag());
	// 	int y = Math.max(this.sprite.getLotag(), c.sprite.getLotag());

	// 	// TODO ...
	// 	if(x == PrefabUtils.MarkerSpriteLoTags.HORIZONTAL_CONNECTOR_EAST && y == PrefabUtils.MarkerSpriteLoTags.HORIZONTAL_CONNECTOR_WEST){
	// 		return true;
	// 	}
	// 	if(x == PrefabUtils.MarkerSpriteLoTags.VERTICAL_CONNECTOR_SOUTH && y == PrefabUtils.MarkerSpriteLoTags.VERTICAL_CONNECTOR_NORTH){
	// 		return true;
	// 	}
	// 	return false;
	// 	*/
	// }

	@Override
	public short getSectorId(){
		return (short)this.sectorId;
	}

	@Override
	public List<Integer> getSectorIds(){
	    return this.allSectorIds;
	}

	@Override
	public String toString(){
		StringBuilder sb = new StringBuilder();
		sb.append("{ connector\n");
		//sb.append("  x: ").append(x).append("\n");
		//sb.append(" ymin: ").append(ymin).append("\n");
		//sb.append(" ymax: ").append(ymax).append("\n");
		sb.append(" sectorId: ").append(sectorId).append("\n");
		sb.append(" wallId: ").append(wallId).append("\n");
		//sb.append(" wall nextSector: ").append(wall.nextSector).append("\n");
		return sb.toString();
	}

	@Override
	public PointXYZ getTransformTo(RedwallConnector other){
	    SimpleConnector c2 = (SimpleConnector)other;
		if(c2 == null){
			throw new IllegalArgumentException("c2 is null");
		}
		if(this.anchorPoint != null){
			if(c2.anchorPoint == null) throw new SpriteLogicException();
			
			return this.anchorPoint.getTransformTo(c2.anchorPoint); 
		}else{
		    throw new RuntimeException("deprecated");
		}
	}


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