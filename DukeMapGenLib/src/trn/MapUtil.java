package trn;

import java.util.List;

/**
 * See also GridUtils
 * 
 * @author Dave
 *
 */
public class MapUtil {

	
	/**
	 * This is for child sectors.  It will attempt to link (as in, create 2-sided red walls...)
	 * every wall in sectorA with every wall in sectorB.  Since whichever one is the outer sector
	 * has more than one wall loop, the walls in the wall loop must be specified.
	 * 
	 * 
	 * @param sectorA 
	 * @param wallA
	 * @param sectorB
	 * @param wallB
	 */
	public static void linkAllWalls(Map map, int sectorA, int wallAindex, int sectorB, int wallBindex){
		
		List<Integer> wallsA = map.getWallLoop(wallAindex);
		List<Integer> wallsB = map.getWallLoop(wallBindex);
		
		if(wallsA.size() != wallsB.size()) throw new RuntimeException();
		
		//gross n^2 algorithm, but slightly safer than a more efficient one
		
		for(int i = 0; i < wallsA.size(); ++i){
			int a = wallsA.get(i);
			Wall wallA = map.getWall(a);
			Wall wallA2 = map.getWall(wallA.getPoint2());
		
			boolean foundMatch = false;
			for(int j = 0; j < wallsB.size(); ++j){
				
				int b = wallsB.get(j);
				Wall wallB = map.getWall(b);
				Wall wallB2 = map.getWall(wallB.getPoint2());
				
				if(wallA.sameXY(wallB2) && wallA2.sameXY(wallB)){
					foundMatch = true;
					
					
					//map.linkRedWalls(sectorA, a, sectorB, wallB.getPoint2());
					
					map.linkRedWalls(sectorA, a, sectorB, b);
					
					
					//wallA.setOtherSide(wallB.getPoint2(), sectorB);
					//wallB2.setOtherSide(a, sectorA);
					
					break;
				}
				
			}
			
			if(! foundMatch) throw new RuntimeException(String.format("failed to find match for wall %d  wallCount=%d", i, wallsA.size()));
			
			
		}
		
		
	}
}
