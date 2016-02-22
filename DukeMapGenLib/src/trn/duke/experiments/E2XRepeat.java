package trn.duke.experiments;

import trn.Map;
import trn.Wall;

/**
 * Experiment to test theories on how xrepeat field works
 * @author Dave
 *
 */
public class E2XRepeat {

	public static void go(Map map){
		
		/*
		 * Ok, so with texture 224, which is mostly a blue wall with a big gray X on it, setting every wall to have xrepeat 8
		 * make it so the half of the texture showed up on every wall, regardless of size.
		 * --- editart says it is 128x128
		 * 
		 * 
		 * setting xrepeat to 4 made less than half of it show up
		 * 
		 * setting xrepeat to 16 made the whole texture show up on each wall
		 * 
		 * now trying texture 285, which appears to be smaller (thought it might not be).  purple mask bg with x on it.
		 * ---   editart says its 64x128
		 * 
		 * xrepeat=4:  half of it shows up on every wall
		 * xrepeat=8:  whole thing shows up on every wall
		 * xrepeat=16:  two show up on every wall
		 * 
		 * so...xrepeat really is something about how many times the tex repeats, but the value is affected by texture size
		 * 
		 * trying texture 23, the RPG (this tex is more than half in the preview square, but doesnt fill the whole square)
		 * -- according to editart, the rpg tile is 77x24
		 * 
		 * xrepeat=4:  gets maybe half-ish?
		 * xrepeat=8: most of the rpg is shown, but not the whole tex
		 * xrepeat=16:  just under two of them are shown
		 * 
		 * 
		 * RESULTS:
		 * comparing this shit to editart, I think the formula is:
		 * 
		 * |------------------------------------------------------|
		 * | (8 * xrepeat) / (tile width) = # repetitions on wall |
		 * |------------------------------------------------------|
		 * 
		 * 
		 * 
		 */
		
		/*
		 * 
		 * editart notes:
		 * 
		 * need to extract the art files first:
		 * kextract duke3d.grp *.art
		 * 
		 * g = goto tile
		 * v = view all tiles
		 * 
		 */
		
		
		
		
		for(int i = 0; i < map.getWallCount(); ++i){
			Wall w = map.getWall(i);
			w.setXRepeat((short)16);
		}
	}
}
