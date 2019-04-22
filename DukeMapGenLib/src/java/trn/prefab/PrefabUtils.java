package trn.prefab;

import trn.DukeConstants;
import trn.ISpriteFilter;
import trn.SpriteFilter;

public class PrefabUtils {

	public static class MarkerSpriteLoTags {
		
		/**
		 * lotag of construction sprite whose hitag serves as an id for the group
		 *
		 * Note: if a sector group does NOT have a group id, then it also cannot have a sprite with texture 0 and a
		 * lotag of 1 (only doing this to remind user they forgot to set the texture).
		 */
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

		/**
		 * Identifies this sector group as a child of another sector group.  A redwall child group cannot have its
		 * own ID (no marker sprites with lotag 1).
         *
		 * The child group can only be a child to one parent, and cannot exist on its own.  It will be absorbed into
		 * the parent.
		 *
		 * TODO:  support child sectors that connect to other child sectors with the same parent.  Example:  parent is
		 * 	sector A:  children are B and C.  The connectorIDs are arranged such that B connects to A, and C connects to
		 * 	B.
		 *
		 * lotag:  4
		 * hitag:  ID of parent sector group  (if parent doesnt exist yet, dont add this marker yet)
         * sector placed in:   same sector as the redwall connector to use
		 * 		the redwall connector must have a connectorID that matches a connector in the parent group
		 * 		the parent group can only have one connector with that connectorID (TODO - update)
		 * 		the child connector id must be > 0
		 */
		public static int REDWALL_CHILD = 4;


		
		/** lotag that marks a construction sprite as connector */
		public static int HORIZONTAL_CONNECTOR_EAST = 16;
		
		public static int HORIZONTAL_CONNECTOR_WEST = 17;
		
		/** horizontal connector that vertically connects rooms; the wall with lotag 1 is on the south edge of the sector */
		public static int VERTICAL_CONNECTOR_SOUTH = 18;
		
		/** horizontal connector that vertically connects rooms; the wall with lotag 1 is on the north edge of the sector */
		public static int VERTICAL_CONNECTOR_NORTH = 19;

		/**
		 *  AUTO CONNECTOR
		 *
		 *  To make an elevator:
		 *  	marker sprite lotag 20
		 *  	sector lotag 15
		 *  	SE sprite lotag 17
		 *
         *
		 * Can become:
		 * 	- simple connectors
		 * 	- multi wall connectors
		 * 	- teleporters / water
		 * 	- elevators
		 *
		 */
		public static int SIMPLE_CONNECTOR = 20;

		// unused because you can still use 20
		public static int TWO_WALL_CONNECTOR = ConnectorType.MULTI_REDWALL; // 21

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
	

	public static int MARKER_SPRITE_TEX = DukeConstants.TEXTURES.CONSTRUCTION_SPRITE;
	
	public static ISpriteFilter MARKER_SPRITE = new SpriteFilter(SpriteFilter.TEXTURE, MARKER_SPRITE_TEX);

}
