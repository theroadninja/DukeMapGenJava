package trn;

/**
 * See also:  http://infosuite.duke4.net/index.php?page=references_dimensions
 * 
 * 
 * 
 * @author Dave
 *
 */
public class DukeConstants {

	//POSITIVE X GOES RIGHT/EAST
	//POSITIVE Y GOES DOWN/SOUTH
	//POSITIVE Z GOES DOWN/LOWER (more negative means more higher)
	
	
	/** facing "up" when looking at the map in build */
	public static final int ANGLE_NORTH = 1536;  //i think 2048 == 360 degrees, with 0 == east and 90 deg is south
	
	public static final int ANGLE_SOUTH = 512; // 90 * (2048 / 360)
	
	
	public static final int DEFAULT_ANGLE = ANGLE_NORTH;
	
	
	/** from http://infosuite.duke4.net/index.php?page=references_dimensions */
	public static final int MAX_DUKE_JUMP_HEIGHT = 20;
	
	
	
	public static final class LOTAGS {
		
		/** simple door that comes down from ceiling */
		public static final int DOOR = 20;
		
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
}
