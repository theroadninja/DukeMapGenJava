package trn;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/**
 * Maps old->new ids for copy operations.
 * 
 * @author Dave
 *
 */
public class IdMap {
	java.util.Map<Short, Short> wallIdMap = new HashMap<Short, Short>();
	java.util.Map<Short, Short> sectorIdMap = new HashMap<Short, Short>();
	
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
	
	
}
