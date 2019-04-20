package trn.duke;

/**
 * 
 * 
 * 
 * http://infosuite.duke4.net/index.php?page=references_special_textures
 * 
 * @author Dave
 *
 */
public class TextureList {

	public static final int SECTOR_EFFECTOR = 1;
	public static final int SE = 1; // shorter version

	/**
	 * Activates a sector lotag or SE sprite, like a switch, but after a time delay.
	 *
	 * BUILDHLP says it can only be activated by a touchplate sprite.
     *
	 * Lotag - same lotag as touchplate sprite
	 * Hitag - set for time delay
	 */
	public static final int MASTERSWITCH = 8;


	public static final int ACTIVATOR = 2;
	public static final int TOUCHPLATE = 3;
	public static final int ACTIVATOR_LOCKED = 4; //a big L
	
	
	/**
	 * a big M.
	 * 
	 * http://wiki.eduke32.com/wiki/Category:Editing_Music_and_Sound_Effects
	 * 
	 * http://forums.duke4.net/topic/2454-exploring-duke-nukem-3ds-sounds/
     *
	 * Activation Sounds
	 * 		lotag = number of the starting sound to play
	 * 		hitag = number of the ending sound to play
	 *
	 * 	Ambient Sound
	 * 		lotag = number of the sound to play
	 *
	 * 	Echo Effect
	 * 		lotag = 1000 + amount of echo (0 to 255, 0 is the least amount)
	 *
	 *  NOTE: you can also set the hitag of a switch to play a sound effect when pressed
	 *
	 *  Some Sounds:
	 *  74 - a door
	 * 
	 */
	public static final int MUSIC_AND_SFX = 5;  //a big M
	public static final int LOCATORS = 6; //a big L with a little plus sign
	public static final int CYCLER = 7; // a big C
	public static final int MASTER_SWITCH = 8; //a big D
	public static final int RESPAWN = 9; //a big R

	/**
	 * Controls the speed of effects.
	 *
	 * No hitag.
	 * Lotag sets the speed.  Smaller number means slower.
     *
	 * However 0 is pretty fast.
	 */
	public static final int GPSSPEED = 10; //this is the one thats not a big letter
	
	//11 and/or 12 might be the cam
	
	public static class Items {
		
		public static final int HANDGUN = 21;
		public static final int CHAINGUN = 22;
		public static final int RPG = 23;
		public static final int FREEZE_RAY = 24;
		public static final int SHRINK_RAY = 25;
		public static final int PIPE_BOMB_SINGLE = 26;
		public static final int TRIP_BOMB = 27;
		public static final int SHOTGUN = 28;
		public static final int DEVASTATOR = 29; //a.k.a. "cycloid" ?
		
		public static final int FREEZE_AMMO = 37;
		public static final int HANDGUN_AMMO = 40;
		public static final int CHAINGUN_AMMO = 41;
		public static final int DEVSTATOR_AMMO = 42;
		public static final int RPG_AMMO = 44;
		public static final int SHRINK_RAY_AMMO = 46;
		public static final int PIPE_BOMB_BOX = 47;
		public static final int SHOTGUN_AMMO = 49;
		
		public static final int HEALTH_SMALL = 51; //a.k.a. cola?
		public static final int HEALTH_MEDIUM = 52;
		public static final int HEALTH_MEDKIT = 53; //the portable item
		public static final int ARMOR = 54;
		public static final int STEROIDS = 55;
		public static final int SCUBA = 56;
		public static final int JETPACK = 57;
		
		public static final int SPACE_SUIT = 58; //NOTE:  not a real item
		
		public static final int NIGHT_VISION = 59;
		
		public static final int CARD = 60;
		
		public static final int BOOTS = 61;
		
		
		public static final int HEALTH_ATOMIC = 100;
		
		public static final int HOLODUKE = 1348;
		
	}
	
	public static class SKIES {
		
		public static final int MOON_SKY = 80; //moon background, earth in distance
		
		public static final int BIG_ORBIT = 84; //huge earth
		
		public static final int LA = 89;
		
		
		
	}
	
	//NOTE: there are some lights here (120) that I'm skipping

	
	public static final class Switches {
		public static final int ACCESS_SWITCH = 130; //for keycards
		
		public static final int SLOT_DOOR = 132; //left or right switch
		
		public static final int LIGHT_SWITCH = 134; //an up or down switch
		
		public static final int SPACE_DOOR_SWITCH = 136; //rotates around center
		
		public static final int SPACE_LIGHT_SWITCH = 138; //swings around a corner
		
		/** looks like a big breaker switch */
		public static final int FRANKENSTINE_SWITCH = 140;
		
		/** end of level switch.
		 * needs a lotag of 65535 to work
		 *  */
		public static final int NUKE_BUTTON = 142; //TODO:  document the pallete to make it a special level
		
		/** switch with 4 positions, like in the death row level */
		public static final int MULTI_SWITCH = 146;
		
		/** the classic red button (circle), often linked together */
		public static final int DIP_SWITCH = 162;
		
		/** square button */
		public static final int DIP_SWITCH_2 = 164;
		
		/** bar that rotates */
		public static final int TECH_SWITCH = 166;
		
		/** whole thing glows red or green */
		public static final int DIP_SWITCH_3 = 168;
		
		/** for key cards; the space version */
		public static final int ACCESS_SWITCH_2 = 170;
		
		/** standard, boring home light switch */
		public static final int LIGHT_SWITCH_2 = 712;
		
		/** outdoor switch wired to box */
		public static final int POWER_SWITCH_1 = 860;
		
		/** giant lever that goes between "open" and "locked" */
		public static final int LOCK_SWITCH_1 = 862;
		
		/** surrounded by yellow/black bars, goes between "off" and "on" */
		public static final int POWER_SWITCH_2 = 864;
		
		/** green alien handprint button */
		public static final int HAND_SWITCH = 1111;
		
		/** alien switch that flips left or right */
		public static final int PULL_SWITCH = 1122;
		
		
		/** green sphincter */
		public static final int ALIEN_SWITCH = 1142;
		
		/** painted hand; only has one tile */
		public static final int HANDPRINT_SWITCH = 1155;
		
		
	}
	
	/**
	 * See
	 * http://infosuite.duke4.net/index.php?page=references_special_textures
	 * for more details.
	 * 
	 *
	 */
	public static final class Enemies {
		
		//stayput == stay in same sector?
		
		public static final int EGG = 675;
		public static final int SHARK = 1680;
		public static final int LIZTROOP = 1680;
		public static final int LIZTROOP_RUNNING = 1681;
		public static final int LIZTROOP_STAYPUT = 1682;
		public static final int LIZTROOP_SHOOT = 1715;
		public static final int LIZTROOP_JETPACK = 1725;
		public static final int LIZTROOP_TOILET = 1741;
		public static final int LIZTROOP_JUSTSIT = 1742;
		public static final int LIZTROOP_DUCKING = 1744;
		public static final int CANNON = 1810;
		public static final int OCTABRAIN = 1820;
		public static final int OCTABRAIN_STAYPUT = 1821;
		public static final int DRONE = 1880;
		public static final int COMMANDER = 1920;
		public static final int COMMANDER_STAYPUT = 1921;
		public static final int RECON = 1960;
		public static final int TANK = 1975;
		public static final int PIGCOP = 2000;
		public static final int PIGCOP_STAYPUT = 2001;
		public static final int PIGCOP_DIVE = 2025; //apparently this counts as a stayput
		public static final int LIZMAN = 2120;
		public static final int LIZMAN_STAYPUT = 2121;
		public static final int LIZMAN_SPITTING = 2150;
		public static final int LIZMAN_JUMP = 2165;
		public static final int ROTATEGUN = 2360;
		public static final int GREENSLIME = 2370;
		public static final int BOSS1 = 2630;
		public static final int BOSS1_STAYPUT = 2631;
		public static final int BOSS2 = 2710;
		public static final int BOSS3 = 2760;
		public static final int NEWBEAST = 4610;
		public static final int NEWBEAST_STAYPUT = 4611;
		public static final int NEWBEAST_HANG = 4670;
		public static final int NEWBEAST_HANGDEAD = 4671;
		public static final int NEWBEAST_JUMP = 4690;
		public static final int BOSS4 = 4740;
		public static final int BOSS4_STAYPUT = 4741;
	}

	public static class ForceFields {
		/** this is the visible one */
		public static int W_FORCEFIELD = 663;

		/** the invisible one that shouldn't hurt the player */
		public static int BIGFORCE = 230;

		// TODO - which is the invisible one?
	}
	
	//TODO:  add door tiles, see 150
	
	//what the hell is REFLECTWATERTILE ?  (180)
	
	public static final int FLOOR_SLIME = 200;
	
	/** pretty sure this is the standard water tile */
	public static final int WATER_TILE_2 = 336;
	
	
	
	
	
	public static final int VIEWSCREEN_SPACE = 449; //a.k.a. viewscreen2
	
	public static final int VIEWSCREEN = 502;

	// Note:  198 doesnt seem to work
	public static final int GLASS = 503;
	
	public static final int GLASS_2 = 504;
	
	
	
	public static final int CRACK1 = 546;
	public static final int CRACK2 = 547;
	public static final int CRACK3 = 548;
	public static final int CRACK4 = 549;
	
	
	/** the big grate with holes in it, often used as a sprite to be a floor or wall */
	public static final int MASK_WALL_12 = 609;

	/**
	 * Used with VIEWSCREEN, sprite 502
     * The VIEWSCREEN sprite's hitag is set equal to CAMERA1 Lotag
	 *
	 *  CAMERA1
	 *  Lotag:  set waual to VIEWSCREEN hitag
	 *  Hitag:  set to amount of turning radius (0 for none)
	 *  Shade:  set to angle of facing down?
	 *  Angle:  set to direction to point
	 *
	 */
	public static final int CAMERA1 = 621;
	
	public static final int TELEPORTER = 626; //labeled "brick" in build
	
	
	/** I think this is the floor that electrocutes you */
	public static final int HURTRAIL = 859;
	
	
	
	
	
	public static final int FLOOR_PLASMA = 1082;
	public static final int LAVA = FLOOR_PLASMA;
	
	
}
