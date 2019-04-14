package trn.prefab;

import trn.DukeConstants;
import trn.ISpriteFilter;
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

		public static int TWO_WALL_CONNECTOR = ConnectorType.MULTI_REDWALL;

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
	


	// public static void joinWalls(Map map, RedwallConnector c1, RedwallConnector c2){
	// 	c1.linkConnectors(map, c2);
	// }
	
	
	


}
