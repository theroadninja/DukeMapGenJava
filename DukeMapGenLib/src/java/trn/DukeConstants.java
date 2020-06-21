package trn;

/**
 * See also:  http://infosuite.duke4.net/index.php?page=references_dimensions
 * 
 * TODO - this is probably replaced by BuildConstants and/or GameConfig
 * 
 * @author Dave
 *
 */
public class DukeConstants {

	//POSITIVE X GOES RIGHT/EAST
	//POSITIVE Y GOES DOWN/SOUTH
	//POSITIVE Z GOES DOWN/LOWER (more negative means more higher)
	
	
	public static final class GridSizes {
		public static final int LARGEST = 1024;
		//512
		//256
		//128
		//64
		//32
		public static final int SMALLEST = 32;
	}
	
	// public static final class CARD_COLORS {
	// 	public static final short BLUE = 0;
	// 	public static final short RED = 21;
	// 	public static final short YELLOW = 23;
	//
	// }
	
	
	/** facing "up" when looking at the map in build */
	public static final int ANGLE_NORTH = 1536;  //i think 2048 == 360 degrees, with 0 == east and 90 deg is south
	
	public static final int ANGLE_SOUTH = 512; // 90 * (2048 / 360)
	
	
	public static final int DEFAULT_ANGLE = ANGLE_NORTH;
	
	
	/** from http://infosuite.duke4.net/index.php?page=references_dimensions */
	public static final int MAX_DUKE_JUMP_HEIGHT = 20;
	
	

	@Deprecated // use trn.duke.Lotags.SE instead
	public static final class SE_LOTAGS {
		public static final int TELEPORT = 7;

		//public static final int ELEVATOR = 17;
	}
	@Deprecated // use trn.duke.Lotags.SE instead
	public static final class LOTAGS {
		
		public static final int NUKE_BUTTON_END_LEVEL = 65535;
		
		/** simple door that comes down from ceiling */
		public static final int DOOR = 20;


		public static final int TWO_WAY_TRAIN = 30;
		
	}
	
	
	/**
	 * http://wiki.eduke32.com/wiki/Category:Editing_Music_and_Sound_Effects
	 * 
	 * http://forums.duke4.net/topic/2454-exploring-duke-nukem-3ds-sounds/
	 * 
	 * @author Dave
	 *
	 */
	public static final class SOUNDS {
		
		public static final int DOOR_OPERATE1 = 74;
	}


	@Deprecated // use trn.duke.TextureList instead
	public static final class TEXTURES {
		public static int SECTOR_EFFECTOR = 1;
		
		/** black and yellow striped construction texture */
		public static int CONSTRUCTION_SPRITE = 355;


		public static int NUKE_BUTTON = 142;
	}
}
