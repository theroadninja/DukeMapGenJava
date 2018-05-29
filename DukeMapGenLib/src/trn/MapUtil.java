package trn;

import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

/**
 * See also GridUtils
 * 
 * @author Dave
 *
 */
public class MapUtil {
	
	public static int average(Iterable<Integer> ints){
		int total = 0;
		int count = 0;
		for(int i: ints){
			total += i;
			count += 1;
		}
		if(count == 0){
			throw new IllegalArgumentException();
		}else{
			return total / count;
		}
	}
	
	
	/**
	 * Takes an array of wall loops (array of arrays) and returns the
	 * total number of walls.
	 * 
	 * @param wallLoops
	 * @return
	 */
	public static int countWalls(Wall[] ... wallLoops){
		if(wallLoops == null) throw new IllegalArgumentException();
		
		int sum = 0;
		for(Wall[] wallLoop : wallLoops){
			if(wallLoop == null) throw new IllegalArgumentException();
			sum += wallLoop.length;
		}
		
		return sum;
	}

	
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
	
	public static <T> T pop(Collection<T> whatever){
		Iterator<T> it = whatever.iterator();
		T result = it.next();
		it.remove();
		return result;
	}
	
	
	/**
	 * Copy a sector, and everything it touches to another map.
	 * 
	 * In a normal map with no elevators, this would copy pretty much everything.
	 * 
	 * @param sourceMap
	 * @param destMap
	 */
	public static CopyState copySectorGroup(final Map sourceMap, Map destMap, int sourceSectorId, PointXYZ transform){
		
		//maps from ids in the old map, to ids in the new map
		//java.util.Map<Short, Short> idmap = new HashMap<Short, Short>();
		
		
		CopyState cpstate = new CopyState();
		
		Set<Integer> sectorsToCopy = new TreeSet<Integer>();
		Set<Integer> alreadyCopied = new TreeSet<Integer>();
		
		sectorsToCopy.add(sourceSectorId);
		while(sectorsToCopy.size() > 0){
			Integer nextId = pop(sectorsToCopy);
			alreadyCopied.add(nextId);
			
			Set<Integer> moreSectors = copySector(sourceMap, destMap, cpstate, nextId, transform);
			for(int i : moreSectors){
				if(! alreadyCopied.contains(i)){
					sectorsToCopy.add(i);
				}
			}
		}
		

		boolean ignoreOtherSectors = false;
		if(ignoreOtherSectors){
			for(Wall w: destMap.walls){
				w.nextSector = -1;
				w.nextWall = -1;
						
			}
		}
		updateIds(destMap, cpstate);
		
		return cpstate;
		
	}
	
	
	static class CopyState {
		IdMap idmap = new IdMap();
		
		List<Integer> wallsToUpdate = new LinkedList<Integer>();
		List<Integer> sectorsToUpdate = new LinkedList<Integer>();
	}
	
	/**
	 * 
	 * @returns more source sector ids, which are neighboors
	 */
	static Set<Integer> copySector(final Map sourceMap,
			Map destMap,
			CopyState cpstate,
			int sourceSectorId,
			PointXYZ transform
			){
		
		Set<Integer> neighboors = new TreeSet<Integer>();
		
		Sector sector = sourceMap.getSector(sourceSectorId);
		System.out.println("sector first wall is: " + sector.getFirstWall());
		
		
		int newSectorId = destMap.addSector(sector.copy().translate(transform));
		cpstate.idmap.putSector(sourceSectorId, newSectorId);
		cpstate.sectorsToUpdate.add(newSectorId);
		
		
		List<Integer> wallLoopIds = sourceMap.getFirstWallLoop(sector);
		neighboors.addAll(copyWallLoop(sourceMap, destMap, cpstate, cpstate.wallsToUpdate, wallLoopIds, transform));
		
		List<Integer> secondWallLoopIds = sourceMap.getSecondWallLoop(sector);
		neighboors.addAll(copyWallLoop(sourceMap, destMap, cpstate, cpstate.wallsToUpdate, secondWallLoopIds, transform));
		
		copySpritesInSector(sourceMap, destMap, sourceSectorId, (short)newSectorId, transform);

		return neighboors;
	}
	
	static void updateIds(Map destMap, CopyState cpstate){
		for(int sid: cpstate.sectorsToUpdate){
			destMap.getSector(sid).translateIds(cpstate.idmap);
		}
		for(int wid: cpstate.wallsToUpdate){
			destMap.getWall(wid).translateIds(cpstate.idmap, false);
		}
	}
	
	/**
	 * 
	 * @returns set of source ids of other sectors
	 */
	static Set<Integer> copyWallLoop(final Map sourceMap, 
			Map destMap, 
			CopyState cpstate,
			List<Integer> wallsToUpdate,
			List<Integer> wallLoopIds,
			PointXYZ transform){
		
		Set<Integer> otherSourceSectors = new TreeSet<Integer>();
		
		if(wallLoopIds == null){
			return otherSourceSectors;
		}
		
		for(int wi: wallLoopIds){
			Wall w = sourceMap.getWall(wi);
			if(w.nextSector != -1){
				otherSourceSectors.add((int)w.nextSector);
			}
			int newId = destMap.addWall(w.copy().translate(transform));
			cpstate.idmap.putWall(wi, newId);
			wallsToUpdate.add(newId);
		}
		return otherSourceSectors;
	}
	
	static void copySpritesInSector(final Map sourceMap, Map destMap, int sourceSectorId, short destSectorId,
			PointXYZ transform){
		
		List<Sprite> sprites = sourceMap.findSprites(null, null, sourceSectorId);
		for(Sprite sp: sprites){
			System.out.println("copying sprite!v picnum=" + sp.picnum);
			destMap.addSprite(sp.copy(destSectorId).translate(transform));
		}
	}
}
