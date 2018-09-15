package trn.prefab;

import java.util.ArrayList;
import java.util.List;

import trn.IdMap;
import trn.Map;
import trn.PointXY;
import trn.PointXYZ;
import trn.Sector;
import trn.Sprite;
import trn.Wall;

public class SimpleConnector extends Connector {

	
	
	/**
	 * ----| - - |----
	 *     |SOUTH|
	 *      \   /
	 *     |\\ //|
	 *     | \ / |
	 *     |NORTH|
	 * ----| - - |----
	 */
	public static ConnectorFilter SouthConnector = new SpriteLotagConnectorFilter(
			PrefabUtils.SpriteLoTags.VERTICAL_CONNECTOR_SOUTH);
	
	public static ConnectorFilter NorthConnector = new SpriteLotagConnectorFilter(
			PrefabUtils.SpriteLoTags.VERTICAL_CONNECTOR_NORTH);

	/**
	 * -+               +--------+
	 *  |               |        |
	 *  |----\ \--------|
	 *  |     \ \       |
	 *    EAST \ \    
	 *  |      / / WEST |
	 *        / /       
	 *  |----/ /--------|
	 *  |               |        |
	 * -+               +--------+
	 * 
	 */
	public static ConnectorFilter EastConnector = new SpriteLotagConnectorFilter(
			PrefabUtils.SpriteLoTags.HORIZONTAL_CONNECTOR_EAST);
	
	public static ConnectorFilter WestConnector = new SpriteLotagConnectorFilter(
			PrefabUtils.SpriteLoTags.HORIZONTAL_CONNECTOR_WEST);


	int connectorId = -1;
	public Sprite sprite;
	int sectorId = -1;
	int wallId = -1;

	@Override
	public int getWallId(){
		return this.wallId;
	}
	
	public Wall wall;  // TODO - make private

	// used for horizontal connector
	int x;
	int ymin;
	int ymax;
	PointXYZ anchorPoint = null;
	int z;

	@Override
	public int getConnectorId(){
		return connectorId;
	}


	public SimpleConnector(Sprite markerSprite, Wall wall){
		if(markerSprite == null){
			throw new IllegalArgumentException("markerSprite is null");
		}
		if(markerSprite.getTexture() != PrefabUtils.MARKER_SPRITE_TEX){
			throw new IllegalArgumentException();
		}
		this.connectorId = markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1;
		this.sprite = markerSprite;
		this.sectorId = markerSprite.getSectorId();
		if(this.sectorId < 0){
			throw new RuntimeException("SimpleConnector sectorId cannot be < 0");
		}
		this.wall = wall;
	}
	
	public SimpleConnector(Sprite markerSprite, Wall wall, Sector sector){
		this(markerSprite, wall);
		this.z = sector.getFloorZ();
	}

	private SimpleConnector(int connectorId, int sectorId, int wallId){
		//TODO add more fields ...
        this.connectorId = connectorId;
        this.sectorId = sectorId;
        this.wallId = wallId;
	}

	public SimpleConnector translateIds(final IdMap idmap){
		//TODO:  also do sprite ...
		//this.sectorId = idmap.sector(this.sectorId);
		//this.wallId = idmap.wall(this.wallId);
		return new SimpleConnector(this.connectorId,
				idmap.sector(this.sectorId),
				idmap.wall(this.wallId));
	}


	@Override
	public boolean isLinked(Map map){
		// TODO - will not work with teleporer connectors, etc
		return map.getWall(wallId).isRedWall();
	}

	@Override
	public short getMarkerSpriteLotag() {
		return this.sprite.getLotag();
	}
	
	public void setAnchorPoint(PointXYZ anchor){
		this.anchorPoint = anchor;
	}
	
	public void setAnchorPoint(PointXY anchor){
		this.anchorPoint = new PointXYZ(anchor, this.z);
	}
	
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
		
	}

	@Override
	public boolean canMate(Connector c){
	    throw new RuntimeException("TODO - this doesnt work");
	    /*
		int x = Math.min(this.sprite.getLotag(), c.sprite.getLotag());
		int y = Math.max(this.sprite.getLotag(), c.sprite.getLotag());

		// TODO ...
		if(x == PrefabUtils.SpriteLoTags.HORIZONTAL_CONNECTOR_EAST && y == PrefabUtils.SpriteLoTags.HORIZONTAL_CONNECTOR_WEST){
			return true;
		}
		if(x == PrefabUtils.SpriteLoTags.VERTICAL_CONNECTOR_SOUTH && y == PrefabUtils.SpriteLoTags.VERTICAL_CONNECTOR_NORTH){
			return true;
		}
		return false;
		*/
	}
	
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
		sb.append(" wall nextSector: ").append(wall.nextSector).append("\n");
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
			return new PointXYZ(
					c2.x - this.x, 
					c2.ymin - this.ymin, 
					c2.z - this.z);
					//c2.sector.getFloorZ() - this.sector.getFloorZ());
			//return new PointXYZ(-1024*5, c2.ymin - this.ymin, c2.sector.getFloorZ() - this.sector.getFloorZ());	
		}
	}
	

	public int getJoinWallLotag(){
		return wall.getLotag();
	}
	


}