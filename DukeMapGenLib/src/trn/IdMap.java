package trn;

import java.util.HashMap;

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
	
	
}
