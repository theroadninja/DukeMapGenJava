package trn.prefab;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

import trn.DukeConstants;
import trn.Map;
import trn.Sprite;

/**
 * Generates random maps based on "prefab" pieces in a map file.
 * @author Dave
 *
 */
public class PrefabGenerator {
	
	/** the map with all the prefab sections */
	private Map prefabMap;
	
	public PrefabGenerator(Map input){
		this.prefabMap = input;
	}
	
	public void generate(){
		
		
		
		// simple vertical join, no rotation.
		// left wall is 1, right wall is 2
		
		/**
		 * Sprite texture:  355 (CONSTRUCTION_SPRITE)
		 * Wall lotag:  (left wall = 1, right wall = 2)
		 * 	where left means wall for the group on the left
		 */
		int JOIN_VERTICAL = 1;
		
		List<Sprite> sprites = prefabMap.findSprites(DukeConstants.TEXTURES.CONSTRUCTION_SPRITE, JOIN_VERTICAL, null);
		for(Sprite s : sprites){
			int sectorId = s.getSectorId();
			System.out.println(sectorId);
			System.out.println(prefabMap.getSector(sectorId));
		}
		
		//
		
	}
	
	
	public static void copy(final Map fromMap, Map toMap){
		//  copy groups of sectors from one map to another
	}
	
	public static Set<Integer> findAllAdjacentSectors(final Map map, int sectorId){
		Set<Integer> closedlist = new HashSet<Integer>();
		
		Set<Integer> openlist = new TreeSet<Integer>();
		openlist.add(sectorId);
		while(openlist.size() > 0){
			int nextId = openlist.iterator().next();
			openlist.iterator().remove();
			closedlist.add(nextId);
			
			for(int adj : map.getAdjacentSectors(nextId)){
				if(! closedlist.contains(adj)){
					openlist.add(adj);
				}
			}
		} //while
		
		
		return closedlist;
		
	}

}
