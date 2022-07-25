package trn.prefab;

import trn.ISpriteFilter;
import trn.Sprite;
import trn.SpriteFilter;

import java.util.Arrays;
import java.util.List;

// TODO - for the hint sprites, see: SectorGroupHints

// TODO - idea:  marker that autopopulates rats to sector (hitag is number of rats to add)

// TODO - reorder these markers before relasing (maybe space them out with reserved numbers in between)
//        (can build a scanner to find them in all my test maps)

public class PrefabUtils {

	public static class MarkerHiTags {

		/** for ANCHOR markers:  use the lowest floor instead of the z of this sprite */
		public static int USE_SECTOR_FLOOR = 1;
	}

	/**
	 * The marker sprite tex is 355
	 */
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



		/**
		 * Marks a sector with existing text sprites meant to spell out words.
		 *
		 * Marker sprite:
		 * 	lotag: 5
		 * 	hitag: 0 OR some ID
		 *
		 * Text Sprites:
		 *   lotag:  non zero, value indicates order (smaller numbers to the left)
		 *
		 */
		public static int AUTO_TEXT = 5;


		/**
		 * The sector group should stay right where it is.  If it has an ID ( GROUP_ID set ) then it will be scanned
		 * and can be pasted again, but the original copy will stay where it is.
		 *
		 */
		public static int STAY = 6;

		// TODO - reserve 7 to possibly use for teleporter connectors
		
		// /** lotag that marks a construction sprite as connector */
		// public static int HORIZONTAL_CONNECTOR_EAST = 16;
		//
		// public static int HORIZONTAL_CONNECTOR_WEST = 17;
		//
		// /** horizontal connector that vertically connects rooms; the wall with lotag 1 is on the south edge of the sector */
		// public static int VERTICAL_CONNECTOR_SOUTH = 18;
		//
		// /** horizontal connector that vertically connects rooms; the wall with lotag 1 is on the north edge of the sector */
		// public static int VERTICAL_CONNECTOR_NORTH = 19;


		/**
		 * Sprite marks a good location for an enemy.
		 */
		public static int ENEMY = 8;

		/**
		 * Location for a powerup, including keys.
		 */
		public static int ITEM = 9;

		/**
		 * Used for a sector group that isnt a real sector group but it only used as input for a particular generator
		 * algorithm.
		 */
		public static int GENERATOR_INPUT = 10;

		/**
		 * This is for a companion sector group that is connected only via water/teleporters/elevators,
		 * and not by redwalls.  This companion sector must be pasted and linked to its parent, however
		 * it can be pasted anyway.
		 *
		 * See REDWALL_CHILD for a child group that connects via a redwall.
         *
		 * All teleporer and elevator connectors between groups must match via connector ids.
		 *
		 * TODO - water automatic?
         *
		 * Lotag: 11
		 * Hitag:  matches sector group id of parent sector group
		 */
		public static int TELEPORT_CHILD = 11;


		/**
		 * Causes the entire sector group to be translazed along the z-axis as it is being read from the source file.
		 * This is for lazy people who want to use elevators but forgot to pgdown the lower sector group.
		 *
		 * Only one of these may exist in a single source file.
		 *
		 * Sprite hitag:  set to amount of z to translate by - TODO better explanation - positive for down?
		 */
		public static int TRANSLATE_Z = 12;

		/**
		 * Generic algorithm "Hint" sprite.  The meaning of the sprite is specific to the algorithm being used to
		 * generate the map.
		 */
		public static int ALGO_HINT = 13;

		/**
		 * For some grid-based algorithms, to lock a room to a certain value on an axis.
		 *
		 * Hitag:     Locks:
		 * 0          x=0
		 * 1          x=1
		 * 2          x=2
		 * ...
		 * 16         y=0
		 * 17         y=1
		 * 18         y=2
		 * ...
		 * 32         z=0
		 * 33         z=1
		 * 34         z=2
		 * ...
		 * 48         w=0
		 * 49         w=1
		 */
		public static int ALGO_AXIS_LOCK = 14;

		/**
		 * Elevator Connector
		 *
		 * To make an elevator with this:
		 * 		marker sprite lotag 17
		 * 		sector lotag 15
		 */
		public static int ELEVATOR_CONNECTOR = 17;

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

		/** a connector that spans multiple sectors */
		public static int MULTI_SECTOR = 22;

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

		public static List<Integer> ALL = Arrays.asList(new Integer[]{
				GROUP_ID,
				PLAYER_START,
				ANCHOR,
				REDWALL_CHILD,
				AUTO_TEXT,
				STAY,
				ENEMY,
				ITEM,
				GENERATOR_INPUT,
				TELEPORT_CHILD,
				TRANSLATE_Z,
				ALGO_HINT,
				ALGO_AXIS_LOCK,
				ELEVATOR_CONNECTOR,
				SIMPLE_CONNECTOR,
				MULTI_SECTOR,
				TELEPORT_CONNECTOR
				});
	}

	public static int MARKER_SPRITE_TEX = 355;  // the construction sprite

	public static boolean isMarker(Sprite s) {
		return s.getTexture() == MARKER_SPRITE_TEX && s.getPal() == 0 && s.getLotag() > 0;
	}

	public static boolean isMarker(Sprite s, int hitag, int lotag){
	    return isMarker(s) && s.getHiTag() == hitag && s.getLotag() == lotag;
	}

	public static void checkValid(Sprite s) throws SpriteLogicException {
		if(isMarker(s) && !MarkerSpriteLoTags.ALL.contains(s.getLotag())){
			throw new SpriteLogicException("invalid marker sprite", s.getLocation().asXY());
		}
	}

	static ISpriteFilter MARKER_SPRITE = new SpriteFilter(SpriteFilter.TEXTURE, MARKER_SPRITE_TEX);

	public static final int hitagToId(Sprite s){
		return (s != null && s.getHiTag() > 0) ? s.getHiTag() : -1;
	}
}
