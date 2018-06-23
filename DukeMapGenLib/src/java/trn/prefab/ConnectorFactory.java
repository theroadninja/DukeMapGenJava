package trn.prefab;

import java.util.Collection;
import java.util.List;

import trn.Map;
import trn.PointXY;
import trn.PointXYZ;
import trn.Sector;
import trn.Sprite;
import trn.Wall;

public class ConnectorFactory {
		
	public static Connector create(Map map, Sprite markerSprite){
		Sprite s = markerSprite;
		
		Sector sector = map.getSector(s.getSectorId());
		
		
		if(s.getLotag() == PrefabUtils.SpriteLoTags.HORIZONTAL_CONNECTOR_EAST
				|| s.getLotag() == PrefabUtils.SpriteLoTags.HORIZONTAL_CONNECTOR_WEST){
			
			
			
			
			//if(onlyInTheseSectors != null && ! onlyInTheseSectors.contains(s.getSectorId()));
			
			// look through each wall in the sector
			
			
			
			int wallId = getLinkWallId(map, sector);
			Wall w = map.getWall(wallId);
			
			Connector connector = new Connector(s, w, sector);
			//connector.wallId = i;
			connector.wallId = wallId;
			connector.z = sector.getFloorZ();
			
			
			PointXY p1 = new PointXY(w);
			PointXY p2 = new PointXY(map.getWall(w.getPoint2()));
			
			if(p1.x != p2.x){
				throw new IllegalArgumentException();
			}
			PointXY anchor = new PointXY(p1.x, Math.min(p1.y, p2.y));
			connector.setAnchorPoint(anchor);
			return connector;
			//*/
			
			
			/*Connector connector = null;
			List<Integer> walls = map.getAllSectorWallIds(sector);
			int wallsPerSprite = 0; 
			for(int i: walls){
				Wall w = map.getWall(i); 
				
				
				
				
				// horizontal connector
				if(w.getLotag() == PrefabUtils.WallLoTags.LEFT_WALL
						|| w.getLotag() == PrefabUtils.WallLoTags.RIGHT_WALL){
					
					connector = new Connector(s, w, sector);
					connector.wallId = i;
					connector.z = sector.getFloorZ();
					
					
					PointXY p1 = new PointXY(w);
					PointXY p2 = new PointXY(map.getWall(w.getPoint2()));
					
					if(p1.x != p2.x){
						throw new IllegalArgumentException();
					}
					PointXY anchor = new PointXY(p1.x, Math.min(p1.y, p2.y));
					connector.setAnchorPoint(anchor);
					
						
				
					
					
					
					wallsPerSprite += 1;
					if(wallsPerSprite > 1){
						throw new SpriteLogicException("connectors can only have one wall per join sprite");
					}
					
					
					
				}
			}
			
			return connector;
			//*/
			
			
			
			
			
		}else if(s.getLotag() == PrefabUtils.SpriteLoTags.VERTICAL_CONNECTOR_NORTH
				|| s.getLotag() == PrefabUtils.SpriteLoTags.VERTICAL_CONNECTOR_SOUTH){
			
			int wallId = getLinkWallId(map, sector);
			
			Wall w = map.getWall(wallId);
			Connector connector = new Connector(s, w, sector);
			connector.wallId = wallId;
			
			PointXY anchor = getHorizontalAnchorPoint(w, map.getWall(w.getPoint2()));
			
			connector.setAnchorPoint(anchor);
			return connector;

		}else{
			//throw new SpriteLogicException("sprite lotag=" + s.getLotag())
			return null;
		}
	}
	
	private static PointXY getHorizontalAnchorPoint(Wall w1, Wall w2){
		PointXY p1 = new PointXY(w1);
		PointXY p2 = new PointXY(w2);
		if(p1.y != p2.y){
			throw new SpriteLogicException();
		}
		return new PointXY(
				Math.min(p1.x, p2.x),
				p1.y);
	}
	
	private static int getLinkWallId(Map map, Sector sector){
		final int linkLotag = 1;
		
		Iterable<Integer> wallIds = map.getAllSectorWallIds(sector);
		Integer wallId = null;
		
		for(int i: wallIds){
			Wall w = map.getWall(i);
			if(w.getLotag() == linkLotag){
				if(wallId == null){
					wallId = i;
				}else{
					throw new SpriteLogicException();
				}
			}
		}
		if(wallId == null){
			throw new SpriteLogicException();
		}
		return wallId;
	}

}
