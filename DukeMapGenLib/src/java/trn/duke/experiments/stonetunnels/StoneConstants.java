package trn.duke.experiments.stonetunnels;

import trn.Sector;
import trn.duke.experiments.SectorPrefab;
import trn.duke.experiments.WallPrefab;

public class StoneConstants {

	//dark shade for most textures
	public static final short SHADE = 14;
	
	
	//
	// UPPER LEVEL
	//
	
	
	
	public static final int UPPER_WALL_TEX = 781;
	public static final int UPPER_CEILING = 742;
	public static final int UPPER_FLOOR = 782;
	
	/** z coord, not texture */
	public static final int UPPER_FLOORZ = Sector.DEFAULT_FLOOR_Z;
	
	public static final WallPrefab UPPER_WALL = new WallPrefab(StoneConstants.UPPER_WALL_TEX).setXRepeat(16).setYRepeat(8).setShade(StoneConstants.SHADE);
	
	public static final SectorPrefab UPPER_SECTOR = new SectorPrefab(UPPER_FLOOR, UPPER_CEILING).setFloorShade(SHADE).setCeilingShade(SHADE);
	
	//
	// LOWER LEVEL
	//
	
	public static final int LOWER_FLOOR = 801;
}
