package trn.prefab;

import java.util.List;

import trn.DukeConstants;
import trn.ISpriteFilter;
import trn.Map;
import trn.PointXY;
import trn.Sector;
import trn.Sprite;
import trn.SpriteFilter;
import trn.Wall;

public class PrefabUtils {
	/*
	public static class JoinType {
		// hitag of constructions sprite - marking it as simple vertical join 
		public static int VERTICAL_JOIN = 1;
	}
	*/
	
	public static class SpriteLoTags {
		
		/** lotag of construction sprite whose hitag serves as an id for the group */
		public static int GROUP_ID = 1;
		
		// is the hitag then a priority to beat out other player starts?
		public static int PLAYER_START = 2;
		
		/** lotag that marks a construction sprite as connector */
		public static int HORIZONTAL_CONNECTOR = 16;
		
		/** horizontal connector that vertically connects rooms; the wall with lotag 1 is on the south edge of the sector */
		public static int VERTICAL_CONNECTOR_SOUTH = 18;
		
		/** horizontal connector that vertically connects rooms; the wall with lotag 1 is on the north edge of the sector */
		public static int VERTICAL_CONNECTOR_NORTH = 19;
	}
	
	public static class WallLoTags {
		
		/** connect wall on the left side of the group on the right */
		public static int LEFT_WALL = 2;
		
		/** the connector wall on the right side of the left group */
		public static int RIGHT_WALL = 1;
	}
	
	// simple join
	// - construction sprite with lotag 1
	// - walls with lotag 1 and 2
	//   - 2 is on the left side of a group, and 1 is on the right
	
	public static int MARKER_SPRITE_TEX = DukeConstants.TEXTURES.CONSTRUCTION_SPRITE;
	
	public static ISpriteFilter MARKER_SPRITE = new SpriteFilter(SpriteFilter.TEXTURE, MARKER_SPRITE_TEX);
	//public static ISpriteFilter CONNECTOR_SPRITE = SpriteFilter.loTag(SpriteLoTags.HORIZONTAL_CONNECTOR);
	
	
	
	
	
	//public static int SPRITE_LO_CONNECTOR = 2;
	

	

	
	public void go(){
		
	}
	
	
	/*
	public static Connector findConnector(Map map, int wallLotag){
		
		// TODO - there is another findConnector() method on PrefabPalette
		
		
		//List<Sprite> sprites = new LinkedList<Sprite>();
		
		
		//for(Sprite s: map.findSprites(MARKER_SPRITE, connectorType, null)){
		for(Sprite s: map.findSprites(
				MARKER_SPRITE, 
				CONNECTOR_SPRITE)){
			
			Sector sector = map.getSector(s.getSectorId());
			List<Integer> walls = map.getAllSectorWallIds(sector);
			for(int i: walls){
				Wall w = map.getWall(i); 
				if(wallLotag == w.getLotag()){
					Connector connector = new Connector();
					connector.sprite = s;
					connector.sectorId = s.getSectorId();
					//connector.sector = sector;
					connector.wall = w;
					connector.wallId = i;
					connector.z = sector.getFloorZ();
					
					//connector.p1 = new PointXY(w);
					//connector.p2 = new PointXY(map.getWall(w.getPoint2()));
					connector.setVerticalLinePoints(new PointXY(w), new PointXY(map.getWall(w.getPoint2())));
					
					return connector;
				}
			}
		}
		
		throw new RuntimeException("cant find connector");
	}*/
	
	
	public static void joinWalls(Map map, Connector c1, Connector c2){
		//Wall w1 = map.getWall(c1.wallId)
		//Wall w2 = map.getWall(c2.wallId);
		
		map.linkRedWalls(c1.sectorId, c1.wallId, c2.sectorId, c2.wallId);
		
		
	}
	
	
	


}
