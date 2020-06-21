package trn;

import java.util.*;

/**
 * Maps old->new ids for copy operations.
 * 
 * @author Dave
 *
 */
public class IdMap {
	java.util.Map<Short, Short> wallIdMap = new HashMap<Short, Short>();

	/** Map of, for each sector, (Sector Id in old map => Sector Id in new map) */
	java.util.Map<Short, Short> sectorIdMap = new HashMap<Short, Short>();

	java.util.Map<Integer, Integer> tagMap = new HashMap<>();

	public void putSector(int oldId, int newId){
		sectorIdMap.put((short)oldId, (short)newId);
	}
	public void putWall(int oldId, int newId){
		wallIdMap.put((short)oldId, (short)newId);
	}
	
	public short sector(int oldId){
		return sectorIdMap.get((short)oldId);
	}
	public short wall(int oldId){
		return wallIdMap.get((short)oldId);
	}

	public List<Integer> wallIds(List<Integer> wallIds){
		List<Integer> results = new ArrayList<>(wallIds.size());
		for(Integer wallId: wallIds){
			results.add((int)this.wall(wallId));
		}
		return results;
	}

	/**
	 * Maps the given source sector ids to their destination sector ids
	 * @param sourceSectorIds - ids of sectors in the source map
	 * @returns the mapped sector ids (the ids of the sectors in the destination map)
	 */
	public List<Integer> sectorIds(List<Integer> sourceSectorIds){
		List<Integer> results = new ArrayList<>(sourceSectorIds.size());
		for(Integer sectorId: sourceSectorIds){
			results.add((int)this.sector(sectorId));
		}
		return results;
	}

	
}
