package trn.prefab;

import trn.ISpriteFilter;
import trn.Sprite;
import trn.SpriteFilter;

import java.util.Arrays;
import java.util.List;

// TODO - for the hint sprites, see: SectorGroupHints

// TODO - idea:  marker that autopopulates rats to sector (hitag is number of rats to add)

// TODO - move most of this to Marker.java
public class PrefabUtils {

	// public static class MarkerHiTags {

	// 	/** for ANCHOR markers:  use the lowest floor instead of the z of this sprite */
	// 	public static int USE_SECTOR_FLOOR = 1;
	// }

	/**
	 * The marker sprite tex is 355
	 */
	public static class MarkerSpriteLoTags {
		// public static int GROUP_ID = Marker.Lotags.GROUP_ID; // 1
		// public static int PLAYER_START = Marker.Lotags.PLAYER_START; // 2
		// public static int ANCHOR = Marker.Lotags.ANCHOR; // 3
		// public static int REDWALL_CHILD = Marker.Lotags.REDWALL_CHILD; // 4
		// public static int AUTO_TEXT = Marker.Lotags.AUTO_TEXT;
		// public static int STAY = 6;
		// TODO - reserve 7 to possibly use for teleporter connectors
		// public static int ENEMY = Marker.Lotags.ENEMY; // 8
		// public static int ITEM = Marker.Lotags.ITEM;
		// public static int GENERATOR_INPUT = 10;
		// public static int TELEPORT_CHILD = Marker.Lotags.TELEPORT_CHILD;
		// public static int TRANSLATE_Z = Marker.Lotags.TRANSLATE_Z;
		// public static int ALGO_HINT = 13;
		// public static int ALGO_AXIS_LOCK = Marker.Lotags.ALGO_AXIS_LOCK;
		// public static int SWITCH_REQUESTED = Marker.Lotags.SWITCH_REQUESTED; // 15
		// public static int ALGO_GENERIC  = Marker.Lotags.ALGO_GENERIC; // 16
		// public static int ELEVATOR_CONNECTOR = 17;
		// public static int SIMPLE_CONNECTOR = 20; // now this is basically all redwall connectors
		// public static int SIMPLE_CONNECTOR = Marker.Lotags.REDWALL_MARKER;

		// NOTE this is NOT about making child sectors.  See Redwall Child for that (lotag 4)
		// also NOTE:  this one is not deprecated!   marker 20 does not automatically extend into other sectors
		// you must place one of these in each sector
		// public static int MULTISECTOR_CHILD = 21;  // accomplishes multi-sector redwall conns by being a child segment

		// public static int MULTI_SECTOR = 22; @Deprecated // use a combination of SIMPLE_CONNECTOR + MULTISECTOR_CHILD

		// public static int RANDOM_ITEM = Marker.Lotags.RANDOM_ITEM; // 23

		// public static int TELEPORT_CONNECTOR = 27;

		// public static int FALL_CONNECTOR = Marker.Lotags.FALL_CONNECTOR;

		// public static int BLANK = Marker.Lotags.BLANK;

		// public static int ALTERNATE_FLOOR_TEX = Marker.Lotags.ALTERNATE_FLOOR_TEX;

	}

	// @Deprecated
	// public static int MARKER_SPRITE_TEX = Marker.MARKER_SPRITE_TEX;

	public static boolean isMarker(Sprite s, int hitag, int lotag){ // TODO not sure if I should keep this
	    return Marker.isMarker(s) && s.getHiTag() == hitag && s.getLotag() == lotag;
	}


	// static ISpriteFilter MARKER_SPRITE = new SpriteFilter(SpriteFilter.TEXTURE, MARKER_SPRITE_TEX);

	/** I don't think I want this behavior to be universal across all markers */
	@Deprecated
	public static final int hitagToId(Sprite s){
		return (s != null && s.getHiTag() > 0) ? s.getHiTag() : -1;
	}
}
