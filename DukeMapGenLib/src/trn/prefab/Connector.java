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
	int x;
	int ymin;
	int ymax;
	int z;
	
	public void setPoints(PointXY p1, PointXY p2){
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
		sb.append("  x: ").append(x).append("\n");
		sb.append(" ymin: ").append(ymin).append("\n");
		sb.append(" ymax: ").append(ymax).append("\n");
		sb.append(" sectorId: ").append(sectorId).append("\n");
		sb.append(" wallId: ").append(wallId).append("\n");
		sb.append(" wall nextSector: ").append(wall.nextSector).append("\n");
		return sb.toString();
	}
	
	public PointXYZ getTransformTo(Connector c2){
		return new PointXYZ(
				c2.x - this.x, 
				c2.ymin - this.ymin, 
				c2.z - this.z);
				//c2.sector.getFloorZ() - this.sector.getFloorZ());
		//return new PointXYZ(-1024*5, c2.ymin - this.ymin, c2.sector.getFloorZ() - this.sector.getFloorZ());
	}
	
	public void translateIds(final IdMap idmap){
		//TODO:  also do sprite ...
		this.sectorId = idmap.sector(this.sectorId);
		this.wallId = idmap.wall(this.wallId);
		
	}
	
	public int getJoinWallLotag(){
		return wall.getLotag();
	}
	
	public static List<Connector> findConnectors(Map map){
		return findConnectors(map, null);
	}
	
	public static List<Connector> findConnectors(Map map, ConnectorFilter ... cf){
		//PrefabUtils.findConnector(outMap, PrefabUtils.JoinType.VERTICAL_JOIN, 1);
		//Map map = numberedSectorGroups.get(sectorGroupId);
		
		List<Connector> results = new ArrayList<Connector>();
		
		for(Sprite s: map.findSprites(
				PrefabUtils.MARKER_SPRITE, 
				PrefabUtils.CONNECTOR_SPRITE)){
			
			
			//if(onlyInTheseSectors != null && ! onlyInTheseSectors.contains(s.getSectorId()));
			
			// look through each wall in the sector
			Sector sector = map.getSector(s.getSectorId());
			List<Integer> walls = map.getAllSectorWallIds(sector);
			for(int i: walls){
				Wall w = map.getWall(i); 
				
				int wallsPerSprite = 0; 
				if(w.getLotag() == PrefabUtils.WallLoTags.LEFT_WALL
						|| w.getLotag() == PrefabUtils.WallLoTags.RIGHT_WALL){
					
					
					Connector connector = new Connector();
					if(s.getHiTag() > 0){
						connector.connectorId = s.getHiTag();
					}
					connector.sprite = s;
					connector.sectorId = s.getSectorId();
					connector.wallId = i;
					connector.wall = w;
					connector.z = sector.getFloorZ();
					connector.setPoints(new PointXY(w), new PointXY(map.getWall(w.getPoint2())));
					
					if(cf == null ||  ConnectorFilter.allMatch(connector, cf)){
						results.add(connector);
					}
					
					
					wallsPerSprite += 1;
					if(wallsPerSprite > 1){
						throw new SpriteLogicException("connectors can only have one wall per join sprite");
					}
					
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