package trn.duke.experiments;

import java.util.HashMap;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import trn.Sector;
import trn.maze.DfsMazeGen;

/**
 * Simplified data structure used to create a map based on square rooms in a grid.
 * 
 * 
 * @author Dave
 *
 */
public class LegacyGrid {

	public static class SimpleTileset {
		public final int wallTexture;
		public final int floorTexture;
		public final int ceilingTexture;
		
		public SimpleTileset(int wall, int floor, int ceil){
			this.wallTexture = wall;
			this.floorTexture = floor;
			this.ceilingTexture = ceil;
		}

		@Override
		public String toString(){
			return String.format("{ wall: %d, floor: %d, ceiling: %d", wallTexture, floorTexture, ceilingTexture);
		}
	}

	public static class BlockInfo {
		SimpleTileset tileset;
		public Integer floorZ = null;
		
		public BlockInfo(){}
		public BlockInfo(SimpleTileset s){
			this.tileset = s;
		}
		public SimpleTileset getTileset(){
			return this.tileset;
		}
	}

}
