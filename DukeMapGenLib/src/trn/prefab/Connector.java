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

public class Connector {

	
	
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

	
	public static ConnectorFilter lotagFilter(final int lotag){
		return new ConnectorFilter(){
			@Override
			public boolean matches(Connector c) {
				return c.wall.getLotag() == lotag;
			}
		};
	}
	
	
	

	int connectorId = -1;
	public Sprite sprite;
	int sectorId = -1;
	int wallId = -1;
	
	//Sector sector;
	public Wall wall;  // TODO - make private
	//PointXY p1;
	//PointXY p2;
	
	
	// used for horizontal connector
	int x;
	int ymin;
	int ymax;
	
	
	PointXYZ anchorPoint = null;
	
	
	int z;
	
	
	
	public Connector(){
		//TODO - get rid of this one
	}
	
	public Connector(Sprite markerSprite, Wall wall){
		if(markerSprite == null){
			throw new IllegalArgumentException("markerSprite is null");
		}
		if(markerSprite.getTexture() != PrefabUtils.MARKER_SPRITE_TEX){
			throw new IllegalArgumentException();
		}
		this.connectorId = markerSprite.getHiTag() > 0 ? markerSprite.getHiTag() : -1;
		this.sprite = markerSprite;
		this.sectorId = markerSprite.getSectorId();
		this.wall = wall;
	}
	
	public Connector(Sprite markerSprite, Wall wall, Sector sector){
		this(markerSprite, wall);
		this.z = sector.getFloorZ();
	}
	
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
	
	public PointXYZ getTransformTo(Connector c2){
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
	
	public void translateIds(final IdMap idmap){
		//TODO:  also do sprite ...
		this.sectorId = idmap.sector(this.sectorId);
		this.wallId = idmap.wall(this.wallId);
		
	}
	
	public int getJoinWallLotag(){
		return wall.getLotag();
	}
	
	public static Iterable<Connector> findConnectors(Iterable<Connector> connectors, ConnectorFilter ... cf){
		List<Connector> results = new ArrayList<Connector>();
		for(Connector c: connectors){
			if(cf == null ||  ConnectorFilter.allMatch(c, cf)){
				results.add(c);
			}
		}
		return results;
	}
	
	public static List<Connector> findConnectors(Map map){
		return findConnectors(map, null);
	}
	
	public static List<Connector> findConnectors(Map map, ConnectorFilter ... cf){
		//PrefabUtils.findConnector(outMap, PrefabUtils.JoinType.VERTICAL_JOIN, 1);
		//Map map = numberedSectorGroups.get(sectorGroupId);
		
		List<Connector> results = new ArrayList<Connector>();
		
		for(Sprite s: map.findSprites(
				PrefabUtils.MARKER_SPRITE)){
			
			Sector sector = map.getSector(s.getSectorId());
			if(s.getLotag() == PrefabUtils.SpriteLoTags.HORIZONTAL_CONNECTOR){
				
				
				//if(onlyInTheseSectors != null && ! onlyInTheseSectors.contains(s.getSectorId()));
				
				// look through each wall in the sector
				
				List<Integer> walls = map.getAllSectorWallIds(sector);
				int wallsPerSprite = 0; 
				for(int i: walls){
					Wall w = map.getWall(i); 
					
					
					
					
					// horizontal connector
					if(w.getLotag() == PrefabUtils.WallLoTags.LEFT_WALL
							|| w.getLotag() == PrefabUtils.WallLoTags.RIGHT_WALL){
						
						
						Connector connector = new Connector();
						if(s.getHiTag() > 0){
							connector.connectorId = s.getHiTag(); // TODO - convered by new constructor
						}
						connector.sprite = s; // TODO - covered by new constructor
						connector.sectorId = s.getSectorId(); // TODO - covered by new constructor
						connector.wallId = i;
						connector.wall = w; // TODO - covered by new constructor
						connector.z = sector.getFloorZ();
						
						
						PointXY p1 = new PointXY(w);
						PointXY p2 = new PointXY(map.getWall(w.getPoint2()));
						
						if(p1.x != p2.x){
							throw new IllegalArgumentException();
						}
						PointXY anchor = new PointXY(p1.x, Math.min(p1.y, p2.y));
						connector.setAnchorPoint(anchor);
						
							
					
						//TODO: remove this
						//connector.setVerticalLinePoints(new PointXY(w), new PointXY(map.getWall(w.getPoint2())));
						
						if(cf == null ||  ConnectorFilter.allMatch(connector, cf)){
							results.add(connector);
						}
						
						
						wallsPerSprite += 1;
						if(wallsPerSprite > 1){
							throw new SpriteLogicException("connectors can only have one wall per join sprite");
						}
						
					}
				}

			}else{
				
				Connector connector = ConnectorFactory.create(map, s);				
				if(connector != null && (cf == null ||  ConnectorFilter.allMatch(connector, cf))){
					results.add(connector);
				}
			}
			
			
			

			//
		} // for sprite
		
		return results;
	}
	
	public static List<Connector> matchConnectors(Iterable<Connector> connectors, ConnectorFilter ... cf){
		List<Connector> results = new ArrayList<Connector>();
		for(Connector c: connectors){
			if(cf == null || ConnectorFilter.allMatch(c, cf)){
				results.add(c);
			}
		}
		return results;
	}
}