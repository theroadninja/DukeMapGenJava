package trn.duke.experiments.gridblock;

import java.util.List;

import org.apache.commons.lang3.tuple.Pair;

import trn.duke.Util;

/**
 * See also MapUtil
 * 
 * 
 * Collection of methods useful if the map is made of square rooms arranged in a grid.
 * 
 * @author Dave
 *
 */
public class GridUtils {

	/**
	 * maybe this could be expanded to a general purpose glue utility
	 * 
	 * @param map
	 * @param sector0
	 * @param sector1
	 */
	public static void linkSectors(trn.Map map, int sector0, int sector1){

		Pair<List<Integer>, List<Integer>> overlappingWalls = Util.filterOverlappingPoints(map, sector0, sector1);
		
		if(overlappingWalls.getLeft().size() != 2) throw new RuntimeException(String.format("sector0=%d sector1=%d", sector0, sector1));
		if(overlappingWalls.getRight().size() != 2) throw new RuntimeException();
		
		Util.orderWalls(map, overlappingWalls.getLeft());
		Util.orderWalls(map, overlappingWalls.getRight());
		
		/*
		System.out.println("-----wall set 1");
		for(int i : overlappingWalls.getLeft()){
			System.out.println("i=" + i);
			System.out.println(map.getWall(i));
		}
		System.out.println("------wall set 2");
		for(int i : overlappingWalls.getRight()){
			System.out.println("i=" + i);
			System.out.println(map.getWall(i));
		}*/
		
		
		int w0 = overlappingWalls.getLeft().get(0);
		int w1 = overlappingWalls.getRight().get(0);
		
		//System.out.println(map.getWall(w0));
		//System.out.println(map.getWall(w1));
		
		//System.out.println(String.format("map.linkRedWalls(%d, %d, %d, %d)", sector0, w0, sector1, w1));
		map.linkRedWalls(sector0, w0, sector1, w1);
	
	}
	
	/**
	 * links every sector in the array to its neighboors.  Does NOT
	 * link the first index to the last.
	 * 
	 * @param map
	 * @param sectorIndexes
	 */
	public static void linkSectorsNoWrap(trn.Map map, int[] sectorIndexes){
		if(sectorIndexes == null || sectorIndexes.length < 2) throw new IllegalArgumentException();
		
		for(int i = 0; i < sectorIndexes.length - 1; ++i){
			linkSectors(map, sectorIndexes[i], sectorIndexes[i+1]);
		}
		
		
	}
}
