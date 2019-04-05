package trn.prefab;

import trn.DukeConstants;
import trn.ISpriteFilter;
import trn.Map;
import trn.SpriteFilter;

public class PrefabUtils {
	/*
	public static class JoinType {
		// hitag of constructions sprite - marking it as simple vertical join 
		public static int VERTICAL_JOIN = 1;
	}
	*/
	
	public static class MarkerSpriteLoTags {
		
		/** lotag of construction sprite whose hitag serves as an id for the group */
		public static int GROUP_ID = 1;
		
		// is the hitag then a priority to beat out other player starts?
		public static int PLAYER_START = 2;


		/**
		 * An anchor whose position you can read to help place a sector group on the grid.
		 * For example, if you want to place the group such that the middle of the room is in
		 * a certain spot, put an anchor in the middle of the room and use its coordinates for
		 * translation.
		 */
		public static int ANCHOR = 3;
		
		/** lotag that marks a construction sprite as connector */
		public static int HORIZONTAL_CONNECTOR_EAST = 16;
		
		public static int HORIZONTAL_CONNECTOR_WEST = 17;
		
		/** horizontal connector that vertically connects rooms; the wall with lotag 1 is on the south edge of the sector */
		public static int VERTICAL_CONNECTOR_SOUTH = 18;
		
		/** horizontal connector that vertically connects rooms; the wall with lotag 1 is on the north edge of the sector */
		public static int VERTICAL_CONNECTOR_NORTH = 19;

		/** marks the sector as a east,west,south or north connector */
		public static int SIMPLE_CONNECTOR = 20;

		/**
		 * A connector sprite that becomes a normal or water teleporter.
		 * (but not a silent teleporter).
		 * When done this way, you don't need an SE sprite because this sprite
		 * becomes an SE sprite.
		 *
		 * You can also make a teleporter connector by putting a simple connector
		 * in a sector group with a teleporter.
		 */
		public static int TELEPORT_CONNECTOR = 27;

	}
	
	public static class WallLoTags {
		
		/** connect wall on the left side of the group on the right */
		// NOTE:  pretty sure I'm not using this
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
	//public static ISpriteFilter CONNECTOR_SPRITE = SpriteFilter.loTag(MarkerSpriteLoTags.HORIZONTAL_CONNECTOR);
	
	
	
	
	
	//public static int SPRITE_LO_CONNECTOR = 2;
	

	

	
	public void go(){
		
	}
	
	
	/*
	public static SimpleConnector findConnector(Map map, int wallLotag){
		
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
					SimpleConnector connector = new SimpleConnector();
					connector.sprite = s;
					connector.sectorId = s.getSectorId();
					//connector.sector = sector;
					connector.wall = w;
					connector.wallId = i;
					connector.z = sector.getFloorZ();
					
					//connector.p1 = new PointXY(w);
					//connector.p2 = new PointXY(map.getWall(w.getPoint2Id()));
					connector.setVerticalLinePoints(new PointXY(w), new PointXY(map.getWall(w.getPoint2Id())));
					
					return connector;
				}
			}
		}
		
		throw new RuntimeException("cant find connector");
	}*/
	
	
	public static void joinWalls(Map map, RedwallConnector c1, RedwallConnector c2){
		//Wall w1 = map.getWall(c1.wallId)
		//Wall w2 = map.getWall(c2.wallId);
		
		map.linkRedWalls(c1.getSectorId(), c1.getWallId(), c2.getSectorId(), c2.getWallId());
		
		
	}
	
	
	


}
