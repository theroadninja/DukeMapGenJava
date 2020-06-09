package trn;

import java.util.*;

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
	 */
	public static void linkAllWalls(Map map, int sectorA, int wallAindex, int sectorB, int wallBindex){
		
		List<Integer> wallsA = map.getWallLoop(wallAindex);
		List<Integer> wallsB = map.getWallLoop(wallBindex);
		
		if(wallsA.size() != wallsB.size()) throw new RuntimeException();
		
		//gross n^2 algorithm, but slightly safer than a more efficient one
		
		for(int i = 0; i < wallsA.size(); ++i){
			int a = wallsA.get(i);
			Wall wallA = map.getWall(a);
			Wall wallA2 = map.getWall(wallA.getPoint2Id());
		
			boolean foundMatch = false;
			for(int j = 0; j < wallsB.size(); ++j){
				
				int b = wallsB.get(j);
				Wall wallB = map.getWall(b);
				Wall wallB2 = map.getWall(wallB.getPoint2Id());
				
				if(wallA.sameXY(wallB2) && wallA2.sameXY(wallB)){
					foundMatch = true;
					
					
					//map.linkRedWalls(sectorA, a, sectorB, wallB.getPoint2Id());
					
					map.linkRedWalls(sectorA, a, sectorB, b);
					
					
					//wallA.setOtherSide(wallB.getPoint2Id(), sectorB);
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
	 * Copy a sector, and every sector connected by a redwall.
	 * 
	 *
	 * @param sourceMap
	 * @param destMap
	 */
	public static CopyState copySectorGroup(final Map sourceMap, Map destMap, int sourceSectorId, PointXYZ transform){
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
		// boolean ignoreOtherSectors = false;
		// if(ignoreOtherSectors){
		// 	for(Wall w: destMap.walls){
		// 		w.nextSector = -1;
		// 		w.nextWall = -1;
		// 	}
		// }
		updateIds(destMap, cpstate);

		// I think i forgot to update the sprite count ...
		if(destMap.sprites.size() != destMap.getSpriteCount()) throw new RuntimeException("logic error");
		
		return cpstate;
		
	}
	
	
	// this is public because someone might need the source sector ids
	public static class CopyState {
		public IdMap idmap = new IdMap();
		
		List<Integer> wallsToUpdate = new LinkedList<Integer>();
		List<Integer> sectorsToUpdate = new LinkedList<Integer>();
		
		public Set<Short> sourceSectorIds(){
			return idmap.sectorIdMap.keySet();
		}
		
		public Set<Short> destSectorIds(){
			Set<Short> ids = new TreeSet<Short>();
			ids.addAll(idmap.sectorIdMap.values());
			return ids;
		}
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
		//System.out.println("sector first wall is: " + sector.getFirstWall());
		
		
		int newSectorId = destMap.addSector(sector.copy().translate(transform));
		cpstate.idmap.putSector(sourceSectorId, newSectorId);
		cpstate.sectorsToUpdate.add(newSectorId);
		
		Iterator<Collection<Integer>> loopIterator = sourceMap.wallLoopIterator(sourceSectorId);
		while(loopIterator.hasNext()){
			Collection<Integer> wallLoopIds = loopIterator.next();
			neighboors.addAll(copyWallLoop(sourceMap, destMap, cpstate, cpstate.wallsToUpdate, wallLoopIds, transform));
		}

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
			Iterable<Integer> wallLoopIds,
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
			//System.out.println("copying sprite!v picnum=" + sp.picnum);
			destMap.addSprite(sp.copy(destSectorId).translate(transform));
		}
	}


	/**
	 *
	 * // TODO - not good enough.  I think this could return the wrong wall if multiple walls in the sector
	 *           insersect the sprite's ray
	 *
	 * Determines if a sprite is "pointing at" a wall.
	 *
	 * The sprites position and angle is used to define a ray (half-line), and the walls position + the position of
	 * the next wall in the loop, is used to define a line segment.  This method returns true if the ray from the
	 * sprite intersects with a wall segment.
	 *
	 * @param s
	 * @param wallId
	 * @param m
	 * @returns true if the sprite is pointed at the wall.
	 */
	public static boolean isSpritePointedAtWall(Sprite s, int wallId, Map m) {
		Wall w1 = m.getWall(wallId);
		Wall w2 = m.getWall(w1.getPoint2Id());
		return isSpritePointedAtWall(s, w1, w2);
	}

	// this is just split out for unit testing
	// TODO - compare to Sprite.intersectsSegment (maybe get rid of this one)
	// NOTE: see also ConnectorScanner.rayIntersect()
	static boolean isSpritePointedAtWall(Sprite s, Wall w1, Wall w2){
		PointXY sv = AngleUtil.unitVector(s.getAngle());
		// System.out.println("\tunit vector: " + sv);
		PointXY p1 = w1.getLocation();
		// System.out.println("\twall segment: " + p1 + ", " + w2.getLocation() + " delta: " + w2.getLocation().subtractedBy(p1));
		return PointXY.raySegmentIntersect(s.getLocation().asXY(), sv, p1, w2.getLocation().subtractedBy(p1), false);
	}

	/**
	 *
	 * @param wallIds ids of walls in the same sector that are connected
	 * @param map the map containing the walls
	 * @return the wallIds sorted in order (so that Wall(i).getPoint2() == Wall(i+1)
	 */
	public static List<Integer> sortWallSection(List<Integer> wallIds, Map map){
	    // TODO - this is a classic topological, so optimize this enough to not be embarassing
		if(wallIds == null) throw new IllegalArgumentException();
	    java.util.Map<Integer, Integer> walls = new TreeMap<>();
		java.util.Map<Integer, Integer> wallsReversed = new TreeMap<>();
	    for(Integer wallId : wallIds) {
	    	Wall w = map.getWall(wallId);
			if (walls.containsKey(wallId)) {
				throw new IllegalArgumentException("list of wall ids contains duplicate");
			} else {
				walls.put(wallId, w.getPoint2Id());
				wallsReversed.put(w.getPoint2Id(), wallId);
			}
		}
	    // pick a random item, build onto the front, then onto the back
		LinkedList<Integer> results = new LinkedList<>();
	    results.add(walls.keySet().iterator().next());
	    while(wallsReversed.containsKey(results.getFirst())){
	        results.addFirst(wallsReversed.get(results.getFirst()));
		}
	    while(walls.containsKey(results.getLast())){
	    	results.addLast(walls.get(results.getLast()));
		}
	    results.removeLast(); // the last element is not part of the list


		if(results.size() != wallIds.size()) throw new RuntimeException("" + results.size() + " != " + wallIds.size());
		checkLoopSection(results, map); // sanity check
		return results;
	}

	/** throws an exception if the walls are not adjacent and in order */
	private static void checkLoopSection(List<Integer> wallIds, Map map){
		for(int i = 0; i < wallIds.size() - 1; ++i){
			int wallId = wallIds.get(i);
			int nextWallId = wallIds.get(i + 1);
			Wall w = map.getWall(wallId);
			if(w.getNextWallInLoop() != nextWallId) throw new IllegalArgumentException("walls are not in a sequential loop");
		}
	}

	/**
	 *
	 * @returns the sum of the cross product of every wall with the next wall.
	 */
	public static long sumOfCrossProduct(Collection<WallView> wallLoop){
		long sum = 0;
		List<WallView> wallLoop2 = new ArrayList<WallView>(wallLoop);
		for(int i = 0; i < wallLoop.size() - 1; ++i){
		    int j = (i+1) % wallLoop.size();
			sum += wallLoop2.get(i).getVector().crossProduct2d(wallLoop2.get(j).getVector());
		}
		return sum;
	}

	/**
	 * @param wallLoop
	 * @returns true if `wallLoop` is the OUTER wall loop of a sector (that is, the sector exists inside it).
	 * 		it returns false if wallLoop is an inner loop, for example a column, where nothing exists inside.
	 */
	public static boolean isOuterWallLoop(Collection<WallView> wallLoop){
		return sumOfCrossProduct(wallLoop) > 0;
	}
}
