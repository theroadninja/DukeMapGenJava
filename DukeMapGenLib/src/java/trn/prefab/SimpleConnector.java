package trn.prefab;

import trn.IdMap;
import trn.Map;
import trn.PointXY;
import trn.PointXYZ;
import trn.Sector;
import trn.Sprite;
import trn.Wall;
import trn.duke.MapErrorException;



// TODO - maybe this should turn into the simple orginal (North, South, East, West) abstraction
// TODO - on top of the redwall connector, which could use a wall of any angle.
public class SimpleConnector extends RedwallConnector {

	
	
	public static ConnectorFilter SouthConnector = new ConnectorTypeFilter(
			PrefabUtils.SpriteLoTags.VERTICAL_CONNECTOR_SOUTH);
	
	public static ConnectorFilter NorthConnector = new ConnectorTypeFilter(
			PrefabUtils.SpriteLoTags.VERTICAL_CONNECTOR_NORTH);

	public static ConnectorFilter EastConnector = new ConnectorTypeFilter(
			PrefabUtils.SpriteLoTags.HORIZONTAL_CONNECTOR_EAST);
	
	public static ConnectorFilter WestConnector = new ConnectorTypeFilter(
			PrefabUtils.SpriteLoTags.HORIZONTAL_CONNECTOR_WEST);


	final int sectorId;
	final int wallId;
	final int connectorType;

	@Override
	public int getWallId(){
		return this.wallId;
	}
	
	// used for horizontal connector
	int x;
	int ymin;
	int ymax;
	PointXYZ anchorPoint = null;
	int z;


	public SimpleConnector(Sprite markerSprite, int wallId, Wall wall){
		super(markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1);
		if(markerSprite == null){
			throw new IllegalArgumentException("markerSprite is null");
		}
		if(markerSprite.getTexture() != PrefabUtils.MARKER_SPRITE_TEX){
			throw new IllegalArgumentException();
		}
        this.connectorType = markerSprite.getLotag();
		this.sectorId = markerSprite.getSectorId();
		if(this.sectorId < 0){
			throw new RuntimeException("SimpleConnector sectorId cannot be < 0");
		}
		this.wallId = wallId;
	}
	
	public SimpleConnector(Sprite markerSprite, int wallId, Wall wall, Sector sector){
		this(markerSprite, wallId, wall);
		this.z = sector.getFloorZ();
	}

	public SimpleConnector(Sprite markerSprite, int wallId, Wall wall, Wall nextWallInLoop, int z) throws MapErrorException {
        super(markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1);
        this.wallId = wallId;
        this.sectorId = markerSprite.getSectorId();

        PointXY vector = wall.getUnitVector(nextWallInLoop);
        if(vector.x == 1) {
            // horizontal wall, vertical connector
            this.connectorType = ConnectorType.VERTICAL_NORTH;
            this.setAnchorPoint(SimpleConnector.getVerticalConnectorAnchor(wall, nextWallInLoop, z));
        }else if(vector.x == -1){
            this.connectorType = ConnectorType.VERTICAL_SOUTH;
            this.setAnchorPoint(SimpleConnector.getVerticalConnectorAnchor(wall, nextWallInLoop, z));
        }else if(vector.y == 1){ // y is pointed down
            // vertical wall, horizontal connector
            this.connectorType = ConnectorType.HORIZONTAL_EAST;
            this.setAnchorPoint(SimpleConnector.getHorizontalConnectorAnchor(wall, nextWallInLoop, z));
        }else if(vector.y == -1){ // y is pointed up
            this.connectorType = ConnectorType.HORIZONTAL_WEST;
            this.setAnchorPoint(SimpleConnector.getHorizontalConnectorAnchor(wall, nextWallInLoop, z));
        }else{
            throw new MapErrorException("connector wall must be horizontal or vertical");
        }

    }

	private SimpleConnector(int connectorId, int sectorId, int wallId, int connectorType){
	    super(connectorId);
		//TODO add more fields ...
        this.sectorId = sectorId;
        this.wallId = wallId;
        this.connectorType = connectorType;
	}

	public SimpleConnector translateIds(final IdMap idmap){
		//TODO:  also do sprite ...
		//this.sectorId = idmap.sector(this.sectorId);
		//this.wallId = idmap.wall(this.wallId);
		return new SimpleConnector(this.connectorId,
				idmap.sector(this.sectorId),
				idmap.wall(this.wallId),
                this.connectorType);
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
	
	public void setAnchorPoint(PointXY anchor){
		setAnchorPoint(new PointXYZ(anchor, this.z));
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
	// 	if(x == PrefabUtils.SpriteLoTags.HORIZONTAL_CONNECTOR_EAST && y == PrefabUtils.SpriteLoTags.HORIZONTAL_CONNECTOR_WEST){
	// 		return true;
	// 	}
	// 	if(x == PrefabUtils.SpriteLoTags.VERTICAL_CONNECTOR_SOUTH && y == PrefabUtils.SpriteLoTags.VERTICAL_CONNECTOR_NORTH){
	// 		return true;
	// 	}
	// 	return false;
	// 	*/
	// }
	
	public short getSectorId(){
		return (short)this.sectorId;
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
	public PointXYZ getTransformTo(Connector FIXME){
	    SimpleConnector c2 = (SimpleConnector)FIXME; // TODO
		if(c2 == null){
			throw new IllegalArgumentException("c2 is null");
		}
		if(this.anchorPoint != null){
			if(c2.anchorPoint == null) throw new SpriteLogicException();
			
			return this.anchorPoint.getTransformTo(c2.anchorPoint); 
		}else{
		    throw new RuntimeException("deprecated");
			//return new PointXYZ(
			//		c2.x - this.x,
			//		c2.ymin - this.ymin,
			//		c2.z - this.z);


			//this code is even older:
					//c2.sector.getFloorZ() - this.sector.getFloorZ());
			//return new PointXYZ(-1024*5, c2.ymin - this.ymin, c2.sector.getFloorZ() - this.sector.getFloorZ());	
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

}