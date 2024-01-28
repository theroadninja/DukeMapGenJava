package trn.duke;

import java.util.Arrays;
import java.util.HashMap;
import java.util.List;

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

	/**
	 * a pulse speed of 135 seems to be about once per second (pulse speed is GPSPEED lotag)
	 */
	public static final int CYCLER = 7; // a big C

	/**
	 * Activates a sector lotag or SE sprite, like a switch, but after a time delay.
	 *
	 * BUILDHLP says it can only be activated by a touchplate sprite. (I dont thnink this is true)
	 *
	 * Lotag - same lotag as touchplate sprite
	 * Hitag - set for time delay
	 */
	public static final int MASTERSWITCH = 8;

	/**
	 * Lotag - set to touchplate lotag
	 * Hitag - set to sprite number
	 */
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

	/** tripbombs placed in level; not ammo.  For ammo use tex 27 */
	public static final int PLACED_TRIP_BOMB = 2566;
	
	public static class Items {
		
		public static final int HANDGUN = 21;
		public static final int CHAINGUN = 22;
		public static final int RPG = 23;
		public static final int FREEZE_RAY = 24;
		public static final int SHRINK_RAY = 25;
		public static final int PIPE_BOMB_SINGLE = 26;

		/** WARNING:  this is ONLY the ammo pickup.  To place them in the level use tex 2566 */
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
		public static final int KEY = CARD;
		
		public static final int BOOTS = 61;
		
		
		public static final int HEALTH_ATOMIC = 100;
		
		public static final int HOLODUKE = 1348;
		
	}

	public static class DOORS {
		public static final int DOORTILE5 = 150; // classic E2L1 space door
		public static final int DOORTILE6 = 151; // slides horizontally
		public static final int DOORTILE1 = 152; // top half
		public static final int DOORTILE2 = 153; // bottom half of 152
		public static final int DOORTILE3 = 154; // circles, top half
		public static final int DOORTILE4 = 155; // circles, bottom half
		public static final int DOORTILE7 = 156; // blue sliding door
		public static final int DOORTILE8 = 157; // nuke door
		public static final int DOORTILE9 = 158;
		public static final int DOORTILE10 = 159;
		public static final int DOORTILE22 = 395; // doesnt look like a door
		public static final int DOORTILE18 = 447;
		public static final int DOORTILE19 = 448;
		public static final int DOORTILE20 = 449;
		public static final int DOORTILE14 = 717;
		public static final int DOORTILE16 = 781;
		public static final int DOORTILE15 = 1102;
		public static final int DOORTILE21 = 1144;
		public static final int DOORTILE11 = 1178;
		public static final int DOORTILE12 = 1179;

		// TODO - where is DOORTILE13? DOORTILE17?  is 22 the last?

		// TODO - not done yet

		// Textures that build editor tile browser doesnt recognize as doors:
		// TODO - test if these work (the lotag/hitag thing)
        // 450, 455, 457, 458, 459, 469, 470, 473, 719, 843, 858, 879, 883, 1108, 1117, 1173
		// Note: 719 is the bathroom door

		public static final List<Integer> ALL = Arrays.asList(new Integer[]{
		        DOORTILE1, DOORTILE2, DOORTILE3, DOORTILE4, DOORTILE5, DOORTILE6, DOORTILE7, DOORTILE8, DOORTILE9,
				DOORTILE10, DOORTILE11, DOORTILE12,
				//DOORTILE13
				DOORTILE14, DOORTILE15, DOORTILE15,
				//DOORTILE16, <-- stone tex that doesnt look like a door
				// DOORTILE17
				DOORTILE18, DOORTILE19, DOORTILE20, DOORTILE21, DOORTILE22
		});


	}

	public static class EXPLOSIVE_TRASH {
		// TODO - not sure which ones work as SEENINE sprites

		public static final int OOZFILTER = 1079; // alien version of SEENINE
		public static final int NUKEBARREL = 1227;
		public static final int CANWITHSOMETHING = 1232; // trash can
		public static final int EXPLODINGBARREL = 1238;
		public static final int FIREBARREL = 1240;
		public static final int SEENINE = 1247;
	}

	public static class FEM {
		// Note: this does not include the femmag (posters, etc)
		public static final int NAKED1 = 603;
		public static final int STATUE = 753; // STATUEFLASH is 869
		public static final int PODFEM1 = 1294; // alien reference
		public static final int FEM1 = 1312; // stripper
		public static final int FEM2 = 1317; // pole stripper
		public static final int FEM3 = 1321; // beer seller
		public static final int FEM5 = 1323; // tied to spike
		public static final int FEM4 = 1325; // fire dancer
		public static final int FEM6 = 1334; // on platform
		public static final int FEM7 = 1395; // karaoke singer

		// cheerleader is 3450, but build editor doesnt recognize as a FEM

        public static final List<Integer> ALL = Arrays.asList(new Integer[]{
        		NAKED1, STATUE, PODFEM1, FEM1, FEM2, FEM3, FEM4, FEM5, FEM6, FEM7
		});
	}
	
	public static class SKIES {
		
		public static final int MOON_SKY = 80; //moon background, earth in distance
		
		public static final int BIG_ORBIT = 84; //huge earth
		
		public static final int LA = 89;
	}

	public static final boolean isDeadly(int tex){
		return tex == SKIES.MOON_SKY || tex == SKIES.BIG_ORBIT;
	}


	//NOTE: there are some lights here (120) that I'm skipping

	
	public static final class Switches {
		public static final int ACCESS_SWITCH = 130; //for keycards
		public static final int ACCESS_SWITCH_OFF = 131;

		public static final int SLOT_DOOR = 132; //left or right switch
        public static final int SLOT_DOOR_OFF = 133;

		public static final int LIGHT_SWITCH = 134; //an up or down switch
        public static final int LIGHT_SWITCH_OFF = 135;

		public static final int SPACE_DOOR_SWITCH = 136; //rotates around center
		public static final int SPACE_DOOR_SWITCH_OFF = 137;
		
		public static final int SPACE_LIGHT_SWITCH = 138; //swings around a corner
		public static final int SPACE_LIGHT_SWITCH_OFF = 139;

		/** looks like a big breaker switch */
		public static final int FRANKENSTINE_SWITCH = 140;
		public static final int FRANKENSTINE_SWITCH_OFF = 141;

		/**
		 * End of level switch.
		 * needs a lotag of 65535 to work (according to a site i found, any non-zero lotag will work, and the
		 * lotag is the episode to go to?)
		 *
		 * Setting the palette to greater than zero makes it a secret level button.  Usually palette 14 is used.
		 *
		 * See http://infosuite.duke4.net/index.php?page=references_special_textures
		 *
		 * NUKE BUTTON TROUBLESHOOTING
		 * - if blocking is set on the sprite, it won't work
		 * 	- open a level that works and compare the cstat of the sprite!
		 * - https://infosuite.duke4.net/index.php?page=references_faq
		 */
		public static final int NUKE_BUTTON = 142; //TODO:  document the pallete to make it a special level

		/**
		 * Lotag of nuke button that will end a level.  Some sources say it must be 65535, others say any nonzero #.
		 * This doesnt really belong here b/c its not a picnum, but I want it to be easy to find.
		 */
		public static final int NUKE_BUTTON_LOTAG = 65535;
		
		/** switch with 4 positions, like in the death row level */
		public static final int MULTI_SWITCH = 146;
		public static final int MULTI_SWITCH_B = 147;
		public static final int MULTI_SWITCH_C = 148;
		public static final int MULTI_SWITCH_D = 149;

		/** the classic red button (circle), often linked together */
		public static final int DIP_SWITCH = 162;
		public static final int DIP_SWITCH_OFF = 163;

		/** square button */
		public static final int DIP_SWITCH_2 = 164;
		public static final int DIP_SWITCH_2_OFF = 165;

		/** bar that rotates */
		public static final int TECH_SWITCH = 166;
		public static final int TECH_SWITCH_OFF = 167;

		/** whole thing glows red or green */
		public static final int DIP_SWITCH_3 = 168;
		public static final int DIP_SWITCH_3_OFF = 169;

		/** for key cards; the space version */
		public static final int ACCESS_SWITCH_2 = 170;
		public static final int ACCESS_SWITCH_2_OFF = 171;

		/** standard, boring home light switch */
		public static final int LIGHT_SWITCH_2 = 712;
		public static final int LIGHT_SWITCH_2_OFF = 713;

		/** outdoor switch wired to box */
		public static final int POWER_SWITCH_1 = 860;
		public static final int POWER_SWITCH_1_OFF = 861;

		/** giant lever that goes between "open" and "locked" */
		public static final int LOCK_SWITCH_1 = 862;
		public static final int LOCK_SWITCH_1_OFF = 863;

		/** surrounded by yellow/black bars, goes between "off" and "on" */
		public static final int POWER_SWITCH_2 = 864;
		public static final int POWER_SWITCH_2_OFF = 865;

		/** green alien handprint button */
		public static final int HAND_SWITCH = 1111;
		public static final int HAND_SWITCH_OFF = 1112;

		/** alien switch that flips left or right */
		public static final int PULL_SWITCH = 1122;
		public static final int PULL_SWITCH_OFF = 1123;

		/** green sphincter */
		public static final int ALIEN_SWITCH = 1142;
		public static final int ALIEN_SWITCH_OFF = 1143;

		/** painted hand; only has one tile */
		public static final int HANDPRINT_SWITCH = 1155;
		
		public static final List<Integer> ALL = Arrays.asList(new Integer[]{ ACCESS_SWITCH, ACCESS_SWITCH_OFF, SLOT_DOOR,
			SLOT_DOOR_OFF, LIGHT_SWITCH, LIGHT_SWITCH_OFF, SPACE_DOOR_SWITCH, SPACE_DOOR_SWITCH_OFF, SPACE_LIGHT_SWITCH,
			SPACE_LIGHT_SWITCH_OFF, FRANKENSTINE_SWITCH,  FRANKENSTINE_SWITCH_OFF, NUKE_BUTTON, MULTI_SWITCH,
			MULTI_SWITCH_B, MULTI_SWITCH_C, MULTI_SWITCH_D, DIP_SWITCH, DIP_SWITCH_OFF, DIP_SWITCH_2, DIP_SWITCH_2_OFF,
			TECH_SWITCH, TECH_SWITCH_OFF, DIP_SWITCH_3, DIP_SWITCH_3_OFF, ACCESS_SWITCH_2, ACCESS_SWITCH_2_OFF,
			LIGHT_SWITCH_2, LIGHT_SWITCH_2_OFF, POWER_SWITCH_1, POWER_SWITCH_1_OFF, LOCK_SWITCH_1, LOCK_SWITCH_1_OFF,
			POWER_SWITCH_2, POWER_SWITCH_2_OFF, HAND_SWITCH, HAND_SWITCH_OFF, PULL_SWITCH, PULL_SWITCH_OFF,
			ALIEN_SWITCH, ALIEN_SWITCH_OFF, HANDPRINT_SWITCH
		});
	}

	public static final boolean isLock(int tex){
		return Switches.ACCESS_SWITCH == tex || Switches.ACCESS_SWITCH_2 == tex;
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
		public static final int NEWBEAST = 4610; // this is the protector drone?
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

		public static final List<Integer> ALL = Arrays.asList(new Integer[]{ W_FORCEFIELD, BIGFORCE });
	}

	/** nothing special, just random textures */
	public static class Other {
	    public static int NICE_GRAY_BRICK = 461;

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

	// TODO - double check these, some of them appear to simply be in the order of the standard ascii charset

	/** this is just numbers, thick and stylized */
	public static final TextureFont FONT1 = new TextureFont(640, "1234567890");

	/** looks like a digital clock */
	public static final TextureFont FONT_DIGITALNUM = new TextureFont(2472, "0123456789");

	/** small blue */
	public static final TextureFont FONT_STARTALPHANUM = new TextureFont(2822, "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`abcdefghijklmnopqrstuvyxwz{|}~");

	/** big red */
	public static final TextureFont FONT_BIGRED = new TextureFont(2930, "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZ")
			.addedTo(new TextureFont(3002, ".,!?;:/%"))
			.addedTo(new TextureFont(3022, "'"));

	/** big gray */
	public static final TextureFont FONT_BIGGRAY = new TextureFont(2966, "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789");

	/** tiny yellow */
	public static final TextureFont FONT_TINYYELLOW = new TextureFont(3010, "0123456789:/");

	/** minifont - very small blue (almost ascii but not quite) */
	public static final TextureFont FONT_MINIFONT = new TextureFont(3072, "!\"#$%&'()*+,-./0123456789:;<=>?@ABCDEFGHIJKLMNOPQRSTUVWXYZ[\\]^_`")
			.addedTo(new TextureFont(3162, "{|}~"));


	private static final TextureFontManager textureToFont = new TextureFontManager(
			FONT1, FONT_DIGITALNUM, FONT_STARTALPHANUM, FONT_BIGRED, FONT_BIGGRAY, FONT_TINYYELLOW, FONT_MINIFONT
	);

	public static final TextureFont getFont(int textureId){
		return textureToFont.get(textureId);
	}
	public static final boolean isFontTex(int textureId){
		return textureToFont.isFontTexture(textureId);
	}
}
