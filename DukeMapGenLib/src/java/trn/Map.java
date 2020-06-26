package trn;

import trn.duke.MapErrorException;
import trn.prefab.MathIsHardException;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.*;

public class Map implements WallContainer {

	/** The DOS build editor will crash if a map has more than 1024 sectors */
	public static final int MAX_SECTOR_GROUPS = 1024;

	public static final int MAX_X = 65536;
	public static final int MIN_X = -65536;
	public static final int MAX_Y = 65536;
	public static final int MIN_Y = -65536;
	public static final PointXY TOP_LEFT = new PointXY(MIN_X, MIN_Y);

	final long mapVersion;
	
	PlayerStart playerStart;
	
	int sectorWithStartPoint;
	
	int sectorCount;

	// TODO - make not public
	public List<Sector> sectors = new ArrayList<Sector>();
	
	//XXX: in theory we could get rid of this and rely on wall.size()
	int wallCount;
	
	ArrayList<Wall> walls = new ArrayList<Wall>();
	
	int spriteCount;
	
	List<Sprite> sprites = new ArrayList<Sprite>();


	private Map(long mapVersion){
		this.mapVersion = mapVersion;
	}
	private Map(){
		this(7);
	}

	public Map copy(){
	    Map map = new Map(this.mapVersion);
	    map.playerStart = this.playerStart;
	    map.sectorWithStartPoint = this.sectorWithStartPoint;
	    map.sectorCount = this.sectorCount;
	    map.wallCount = this.wallCount;
	    map.spriteCount = this.spriteCount;

	    map.sectors = new ArrayList<>();
	    for(Sector s : this.sectors){
	    	map.sectors.add(s.copy());
		}
	    map.walls = new ArrayList<>();
	    for(Wall w : this.walls){
	    	map.walls.add(w.copy());
		}
	    map.sprites = new ArrayList<>();
	    for(Sprite s : this.sprites){
	    	map.sprites.add(s.copy());
		}
	    return map;
	}
	
	public static Map createNew(){
		Map map = new Map(7); //7 is duke.  not sure if it includes atomic or not.
		return map;
	}

	public long getMapVersion(){
		return mapVersion;
	}
	
	public PlayerStart getPlayerStart(){
		return this.playerStart;
	}
	
	public void setPlayerStart(PlayerStart ps){
		this.playerStart = ps;
	}
	public boolean hasPlayerStart(){
		return this.playerStart != null;
	}
	
	/**
	 * @return the sector that contains the start point
	 */
	public long getStartSector(){
		return this.sectorWithStartPoint;
	}
	
	public int getSectorCount(){
		return this.sectorCount;
	}
	
	public Sector getSector(int i){
		return sectors.get(i);
	}
	public int addSector(Sector s){
		this.sectors.add(s);
		this.sectorCount++;
		return this.sectorCount -1;
	}

	public int getWallCount(){
		return this.wallCount;
	}
	
	public Wall getWall(int i){
		if(i < 0){
			throw new IllegalArgumentException("invalid wall id: " + i);
		}
		return this.walls.get(i);
	}

	public WallView getWallView(int wallId){
		Wall w = getWall(wallId);
		Wall w2 = getWall(w.getPoint2Id());
		LineSegmentXY line = new LineSegmentXY(w.getLocation(), w2.getLocation());
		return new WallView(w, wallId, line);
	}

	public List<WallView> getWallViews(Collection<Integer> wallIds){
		List<WallView> results = new ArrayList<>(wallIds.size());
		for(int wallId : wallIds){
			results.add(getWallView(wallId));
		}
		return results;
	}

	// public List<Wall> getWalls(List<Integer> wallIds){
	// 	List<Wall> results = new ArrayList<>();
	// 	for(int i : wallIds){
	// 		results.add(getWall(i));
	// 	}
	// 	return results;
	// }
	
	/**
	 * @return indexes of all walls in a sector ...actually no, just all walls in the loop that has the first wall
	 */
	public List<Integer> getSectorWallIndexes(int sectorIndex){
		
		Sector sector = getSector(sectorIndex);
		

		//this is invalid because sectors can have more than one wall loop:
		//return getWallLoop(sector.getFirstWall());
		
		int firstWall = sector.getFirstWall();
		
		List<Integer> list = new ArrayList<Integer>(sector.getWallCount());
		for(int i = firstWall; i < firstWall + sector.getWallCount(); ++i){
			list.add(i);
		}
		
		return list;
		
	}
	
	
	/**
	 * 
	 * @param sector
	 * @returns all walls for the sector, which might be in two loops or more
	 */
	public List<Integer> getAllSectorWallIds(final Sector sector){
		List<Integer> walls = new LinkedList<Integer>();
		for(int i = sector.getFirstWall(); i < sector.getFirstWall() + sector.getWallCount(); ++i){
			walls.add(i);
		}
		return walls;
	}
	public List<Integer> getAllSectorWallIds(int sectorId){
		return this.getAllSectorWallIds(this.getSector(sectorId));
	}

	public List<Integer> getFirstWallLoop(final Sector sector){
		return this.getWallLoop(sector.getFirstWall());
	}

	public Iterator<Collection<Integer>> wallLoopIterator(int sectorId){
		return new WallLoopIterator(this, sectorId);
	}

	/**
	 * Returns the wall Ids
	 * @param sectorId
	 * @return
	 */
	public List<Collection<Integer>> getAllWallLoops(int sectorId){
		Iterator<Collection<Integer>> it = wallLoopIterator(sectorId);
		List<Collection<Integer>> result = new ArrayList<>();
		while(it.hasNext()){
			result.add(it.next());
		}
		return result;
	}

	// see also MapImplicits.allWallViews
	public List<Collection<WallView>> getAllWallLoopsAsViews(int sectorId){
		Iterator<Collection<Integer>> it = wallLoopIterator(sectorId);
		List<Collection<WallView>> result = new ArrayList<>();
		while(it.hasNext()){
			result.add(getWallViews(it.next()));
		}
		return result;
	}

	
	
	/**
	 * 
	 * @param wallIndex any wall index in the loop
	 * @return
	 */
	public List<Integer> getWallLoop(int wallIndex){
		List<Integer> list = new ArrayList<Integer>();
		
		int safety = 10000;
		int index = wallIndex;
		while(safety-- > 0){
			list.add(index);
			
			index = walls.get(index).getPoint2Id();
			
			if(index == wallIndex){
				break; //back to where we started
			}
		}
		
		return list;
	}

	//public int getPreviousWall(int wallIndex) throws MapErrorException {
	//	if(wallIndex < 0) throw new IllegalArgumentException();
	//	List<Integer> loop = getWallLoop(wallIndex);
	//	for(int i = 0; i < loop.size(); ++i){
	//		Wall pw = getWall(i);
	//		if(wallIndex == pw.point2){
	//			return wallIndex;
	//		}
	//	}
	//	throw new MapErrorException("unable to find previous wall for wall " + wallIndex);
	//}
	//
	/**
	 * WARNING: this does not automatically update any indexes (it does update wall count)
	 * 
	 * @param w wall to add
	 * @return the index of the new wall
	 */
	public int addWall(Wall w){
		walls.add(w);
		this.wallCount++;
		if(this.wallCount != walls.size()) throw new RuntimeException("wall count mismatch");
		return wallCount - 1;
	}
	
	/**
	 * Adds the walls and links them together by setting their 'nextwall' pointers to the 
	 * indices they get when they are added to the wall array.
     *
	 * I think this is only for adding walls for the purpose of creating a sector.
	 * 
	 * Details:  in the build format walls are identified by their index in the global list of
	 * walls for the entire map, so we don't really have an identifier for them until we add them to
	 * that list.  This method makes it easy to add all the walls for a sector at once.
	 * 
	 * @return
	 */
	public int addLoop(Wall ... wallsToAdd){
		if(wallsToAdd == null || wallsToAdd.length < 3) throw new IllegalArgumentException();
		
		int firstWall = -1;
		Wall lastWall = null;
		
		for(Wall w : wallsToAdd){
			
			//add the wall
			int index = addWall(w);
			
			w.setPoint2Id(index + 1);
			
			//if its the first wall, update the index
			if(firstWall == -1){
				firstWall = index;
			}
			
			lastWall = w;	
		}
		
		lastWall.setPoint2Id(firstWall);
		
		return firstWall;
	}


	public List<Integer> addLoopToSector(int sectorId, List<Wall> wallsToAdd){
		return addLoopToSector(sectorId, wallsToAdd.toArray(new Wall[]{}));
	}

	/**
	 * Adds (inserts) wall loop to an existing sector.  If you are calling this, you are basically adding a hole to
	 * the sector (e.g., a column)
	 *
	 * The walls must already be in the correct order, which since they are a hole, means counter-clockwise from the
	 * Build editor POV.
	 *
	 * @param sectorId
	 * @param wallsToAdd
	 * @return the new wall ids
	 */
	public List<Integer> addLoopToSector(int sectorId, Wall ... wallsToAdd){

	    // make sure points are CCW
		List<PointXY> points = new ArrayList<PointXY>();
		for(Wall wall : wallsToAdd){
			points.add(wall.getLocation());
		}
		if(isClockwise(points.toArray(new PointXY[]{}))) throw new IllegalArgumentException("points are in the wrong order");

		// TODO - test for intersections with the parent sector (and maybe every other wall on the map)

		// make sure lines dont intersect
        for(int i = 0; i + 3 < points.size(); ++i){
            // we skip testing i+1 and i+2 because those lines cannot intersect, and overlapping is a valid use case
			// (for doors)
        	LineSegmentXY lineA = new LineSegmentXY(points.get(i), points.get(i+1));
			LineSegmentXY lineB = new LineSegmentXY(points.get(i+2), points.get(i+3));
			if(lineA.intersects(lineB)) throw new IllegalArgumentException("walls in loop intersect each other");
		}

        // defensive copy (not setting sector id here because walls dont know their sector id)
        List<Wall> newWalls = new ArrayList<>(wallsToAdd.length);
        for(Wall wall: wallsToAdd){
        	Wall wall2 = wall.copy();
        	newWalls.add(wall2);
		}

		// insert the walls, setting up the loop
        Sector sector = this.getSector(sectorId);
        int addIndex = sector.getFirstWall() + sector.getWallCount(); // TODO rename to insertionIndex
		this.walls.addAll(addIndex, newWalls); // TODO make a defensive copy of each wall

		// update wall count
		// update sector wall count
		this.wallCount += wallsToAdd.length;
        sector.setWallCount(sector.getWallCount() + wallsToAdd.length);

		// update sector firstWall pointers
		for(int otherSectorId = 0; otherSectorId < this.getSectorCount(); ++otherSectorId){
			Sector otherSector = this.getSector(otherSectorId);
			if(otherSector.getFirstWall() >= addIndex){
			    otherSector.setFirstWall(otherSector.getFirstWall() + wallsToAdd.length);
			}
		}

		// update point2 pointers on walls (none of them should cross the insertion boundary)
		// update nextwall (redwall) pointers
		for(int wallId = 0; wallId < this.wallCount; ++wallId){
			Wall w = this.getWall(wallId);
			if(wallId < addIndex && w.getPoint2Id() >= addIndex) throw new MathIsHardException("should never happen");

			if(w.getPoint2Id() >= addIndex){
				w.setPoint2Id(w.getPoint2Id() + wallsToAdd.length);
			}
			if(w.isRedWall() && w.getOtherWall() >= addIndex){
				w.setOtherWall(w.getOtherWall() + wallsToAdd.length);
			}
		}

		// set the point2 indexes for the new walls, AFTER updating the others
		List<Integer> newIds = new ArrayList<Integer>(wallsToAdd.length);
		for(int i = 0; i < newWalls.size(); ++i){
			newIds.add(addIndex + i);
			Wall wall = this.getWall(addIndex + i);
			wall.setPoint2Id(addIndex + i + 1);
		}
		this.getWall(addIndex + newWalls.size() - 1).setPoint2Id(addIndex);

		if(this.wallCount != walls.size()) throw new RuntimeException("wall count mismatch");
		return newIds;
	}
	
	/**
	 * adds the walls and the new sector object.
	 *
	 * TODO - the walls always need to be either CW or CCW....?
	 * 
	 * @param wallsToAdd
	 * @return sectorId
	 */
	public int createSectorFromLoop(Wall ... wallsToAdd){
		return addSector(new Sector(addLoop(wallsToAdd), wallsToAdd.length));
	}
	
	public int createSectorFromMultipleLoops(Wall[] ... wallLoops){
		
		int wallCount = MapUtil.countWalls(wallLoops);
		if(wallCount < 3) throw new RuntimeException(); //sanity check
		
		int firstWall = -1;
		for(Wall[] wallLoop : wallLoops){
			int i = addLoop(wallLoop);
			if(firstWall == -1){
				firstWall = i;
			}
		}
		if(firstWall < 0) throw new RuntimeException(); //sanity check
		
		return addSector(new Sector(firstWall, wallCount));
		
		
	}

	/**
	 * Adds multiple walls loops and creates a sector out of them, using the given sector as a template.
	 *
	 * Wall "point 2" pointers to the next wall in the loop will be automatically set.
	 * The sectors firstWall and wall count will be automatically set.
	 *
	 * @param sector
	 * @param wallLoops
	 * @return
	 */
	public int createSectorFromMultipleLoops(Sector sector, List<List<Wall>> wallLoops){
		if(wallLoops.size() < 1) throw new IllegalArgumentException("wallLoops cannot be empty");
		String msg = "first wall offset cannot be negative.  use 0 if you dont have a first wall picked out";

        int firstWall = -1;
		int wallCount = 0;
		for(List<Wall> wallLoop: wallLoops){
		    wallCount += wallLoop.size();
			Wall[] array = new Wall[wallLoop.size()];
			for(int i = 0; i < wallLoop.size(); ++i){
				array[i] = wallLoop.get(i).copy(); // defensive copy
			}
			int f = addLoop(array);
			if(firstWall == -1){
				firstWall = f;
			}
		}

		Sector newSector = sector.copy();
		newSector.setFirstWall(firstWall);
		newSector.setWallCount(wallCount);
		return addSector(newSector);
	}
	
	/**
	 * connects two sectors by creating the necessary link between the sectors' walls,
	 * creating two-sided walls (which appear red in build); 
	 */
	public void linkRedWalls(int sectorIndex, int wallIndex, int sectorIndex2, int wallIndex2){
		linkRedWallsStrict(sectorIndex, wallIndex, sectorIndex2, wallIndex2);
	}

	public void linkRedWallsStrict(int sectorIndex, int wallIndex, int sectorIndex2, int wallIndex2){
		Wall w1 = getWall(wallIndex);
		// TODO - instead of throwing, just disable...
		if(w1.getStat().blockPlayer()){
			throw new RuntimeException("wall has blocking enabled near " + w1.getLocation());
			//TODO - warn
		}
		Wall w1End = getWall(w1.getPoint2Id());
		Wall w2 = getWall(wallIndex2);
		// the below condition can easily happen as soon as any red wall in the sector group has blocking,
		// even the solid walls seem to inherit it somehow.
		if(w2.getStat().blockPlayer()) throw new RuntimeException("wall has blocking enabled " + w2.getLocation());
		Wall w2End = getWall(w2.getPoint2Id());
		if(w1.isRedWall()) throw new IllegalArgumentException("wall " + wallIndex + " is already a red wall");
		if(w2.isRedWall()) throw new IllegalArgumentException("wall " + wallIndex2 + " is already a red wall");

		if(!(w1.getLocation().equals(w2End.getLocation()) && w2.getLocation().equals(w1End.getLocation()))){
			String message = String.format("%s->%s vs %s<-%s", w1.getLocation().toString(), w1End.getLocation().toString(), w2End.getLocation().toString(), w2.getLocation().toString());
			throw new IllegalArgumentException(message);
		}
		w1.setOtherSide(wallIndex2, sectorIndex2);
		w2.setOtherSide(wallIndex, sectorIndex);
    }

	
	public int addSprite(Sprite s){
		this.sprites.add(s);
		this.spriteCount++;
		if(sprites.size() != spriteCount) throw new RuntimeException();
		return spriteCount - 1;
	}
	
	public int getSpriteCount(){
		return this.spriteCount;
	}

	public int spriteCount(){ return getSpriteCount(); }
	
	public Sprite getSprite(int i){
		return sprites.get(i);
	}
	
	public void deleteSprite(int spriteId){
		sprites.remove(spriteId);
		this.spriteCount = sprites.size();
	}
	

	public List<Integer> findSpriteIds(ISpriteFilter... filters){
		List<Integer> results = new ArrayList<Integer>(sprites.size());
		for(int i = 0; i < sprites.size(); ++i){
			if(! SpriteFilter.matchAll(sprites.get(i), filters)){
				continue;
			}
			results.add(i);
		}
		return results;
	}

	/**
	 * Removes a sector AND everything in it.
	 *
	 * TODO: does this work?
	 */
	public void deleteSector(int sectorId){

		if(sectorId < 0 || sectorId > this.sectors.size()){
			throw new IllegalArgumentException("invalid sector id: " + sectorId);
		}

		Sector sector = getSector(sectorId);

		// 1. unlink all walls
		List<Integer> wallsToDelete = getAllSectorWallIds(sector);
		for(Wall w: walls){
			if(wallsToDelete.contains((int)w.nextWall)){
				w.setOtherSide(-1, -1);
			}
		}

		// 2. remove this sectors sprites
		deleteSprites((Sprite s) -> s.getSectorId() == sectorId);

		// if(1==1) return;

		int startIndex = sector.getFirstWall();
		int endIndexExclusive = startIndex + sector.getWallCount();
		int deletedCount = sector.getWallCount();

		// 2. delete this sector's walls
		walls.subList(startIndex, endIndexExclusive).clear();


		// 4. remove the sector
		this.sectors.remove(sectorId);

		// 5. shift sprite sectorIds
        for(Sprite s: this.sprites){
        	if(s.sectnum >= sectorId){
        		s.sectnum -= 1;
			}
		}

		// 6. shift firstWall Pointers of other sectors
		for(Sector s: sectors){
			if(s.getFirstWall() >= startIndex && s.getFirstWall() < endIndexExclusive){
			    // NOTE: this gets false positive if we do it before we delete the sector
				throw new RuntimeException("something went wrong");
			}else if(s.getFirstWall() >= endIndexExclusive){
				s.setFirstWall(s.getFirstWall() - deletedCount);
			}
		}

		// 7. shift the redwall indexes and point2 indexes, and the next sector tag
        for(Wall w: walls){
        	if(w.nextWall >= startIndex){
        		if(w.nextWall < endIndexExclusive){
					// these walls have been deleted; nothing should be pointing to them
					throw new RuntimeException("something went wrong");
				}else{
        		    w.nextWall -= deletedCount;
				}
			}
        	if(w.nextSector == sectorId) {
				throw new RuntimeException("something went wrong");
			}else if(w.nextSector > sectorId){
        		w.nextSector -= 1;
			}
        	if(w.point2 >= startIndex){
        		if(w.point2 < endIndexExclusive){
					// these walls have been deleted; nothing should be pointing to them
					throw new RuntimeException("something went wrong");
				}else{
        			w.point2 -= deletedCount;
				}
			}
		}

		// 8. update counts
		this.sectorCount -= 1;
		if(this.wallCount != this.walls.size() + wallsToDelete.size()){
			throw new RuntimeException("something went wrong");
		}
		this.wallCount = this.walls.size();
	}


	// TODO note about join(A, B) != join(B, A)
	public void joinSectors(int sectorIdA, int sectorIdB){

		// TODO!!!  this needs to verify that the sectors actually share a redwall
        // TODO - maybe also do clockwise checks on the loops (only 1 should be clockwise from Build POV)

		if(this.sectorCount != this.sectors.size()){
			throw new IllegalStateException("sector count mismatch");
		}
		if(sectorIdA < 0 || sectorIdA >= this.sectorCount || sectorIdB < 0 || sectorIdB >= this.sectorCount || sectorIdA == sectorIdB){
			throw new IllegalArgumentException(String.format("invalid sector ids: %d, %d", sectorIdA, sectorIdB));
		}

		// 1. figure out the new wall loops
		List<WallView> walls = new ArrayList<>();
		for(int i: this.getAllSectorWallIds(getSector(sectorIdA))){
			walls.add(this.getWallView(i));
		}
		for(int i: this.getAllSectorWallIds(getSector(sectorIdB))){
			walls.add(this.getWallView(i));
		}
		Set<Integer> oldIds = new HashSet<>();
		for(int i = 0; i < walls.size(); ++i){
			oldIds.add(walls.get(i).getWallId());
		}


		List<List<WallView>> wallsForJoin = Map.followAllWallsForJoin(sectorIdA, sectorIdB, walls);
		int firstWallId = getSector(sectorIdA).getFirstWall();
		if(getWall(firstWallId).isRedWall() && getWall(firstWallId).getOtherSector() == sectorIdB){
			// this wall will be destroyed
			firstWallId = wallsForJoin.get(0).get(0).getWallId();
		}
		List<List<WallView>> preCopyLoops = Map.firstWallFirst(
				Map.followAllWallsForJoin(sectorIdA, sectorIdB, walls),
				firstWallId
		);

		// 2. create the new sector, wall id map
		List<WallView> preCopyLoopsFlattened = new ArrayList<>();
		List<List<Wall>> preCopyLoops2 = new ArrayList<List<Wall>>(preCopyLoops.size());
		for(List<WallView> preCopyLoop: preCopyLoops){
			preCopyLoops2.add(WallView.toWalls(preCopyLoop));
			preCopyLoopsFlattened.addAll(preCopyLoop);
		}
		int newSectorId = this.createSectorFromMultipleLoops(getSector(sectorIdA), preCopyLoops2);

		// TODO - this might not be needed
		java.util.Map<Integer, Integer> wallIdMap = new HashMap<Integer, Integer>();

		// map of old wall id -> new wall id
		List<Integer> newWallIds = this.getAllSectorWallIds(newSectorId);
		if(newWallIds.size() != preCopyLoopsFlattened.size()){
			throw new MathIsHardException("wall counts dont match");
		}
		for(int i = 0; i < newWallIds.size(); ++i){
			wallIdMap.put(preCopyLoopsFlattened.get(i).getWallId(), newWallIds.get(i));
		}

		// 3. update inbound nextwall links for other wall and other sector
		for(int newWallId: newWallIds){
			Wall newWall = getWall(newWallId);
			if(newWall.isRedWall()){
				Wall otherWall = getWall(newWall.getOtherWall());
				otherWall.setOtherSide(newWallId, newSectorId);
			}
		}
		// zero out red walls for old walls
		for(Integer oldWallId: oldIds){
			Wall oldWall = this.getWall(oldWallId);
			oldWall.setOtherSide(-1, -1);
		}
		// now that we've set all the back links, there should be no redwalls pointing to the old sectors
		for(int i = 0; i < this.wallCount; ++i){
			Wall w = this.getWall(i);
			if(w.isRedWall()){
				if(w.getOtherSector() == sectorIdA || w.getOtherSector() == sectorIdB){
					String msg = String.format("Sector join: wall %d still pointing to the old sector", i);
					throw new MathIsHardException(msg);
				}
				if(oldIds.contains(w.getOtherWall())){
					throw new MathIsHardException("Sector join: walls still pointing to the old walls");
				}
			}
		}

		// 4. update sprites
		for(int spriteId = 0; spriteId < this.getSpriteCount(); ++spriteId){
			Sprite sprite = this.getSprite(spriteId);
			if(sprite.getSectorId() == sectorIdA || sprite.getSectorId() == sectorIdB){
				sprite.setSectorId(newSectorId);
			}
		}

		// 5. delete the old sectors
        if(sectorIdA > sectorIdB){
			this.deleteSector(sectorIdA);
			this.deleteSector(sectorIdB);
		}else{
			this.deleteSector(sectorIdB);
			this.deleteSector(sectorIdA);
		}

		try {
			this.assertIntegrity();
		}catch(Exception ex){
			throw new RuntimeException(ex);
		}

	}


	private void assertValidSector(int sectorId) throws Exception {
		if(sectorId < 0 || sectorId > this.sectors.size()){
			throw new Exception("invalid sector id=" + sectorId);
		}

	}
	/**
	 * @throws if there is something wrong with the map's structure
	 */
	public void assertIntegrity() throws Exception {
		if (this.sectorCount != this.sectors.size()) {
			throw new Exception("sectorCount is off");
		}
		if(this.wallCount != this.walls.size()){
			throw new Exception("wallCount is off");
		}

	    // make sure all sprites point to an existing sector
		for(Sprite s: this.sprites){
			if(s.sectnum < 0 || s.sectnum >= this.sectors.size()){
				throw new Exception("sprite has invalid sector id: " + s.sectnum);
			}
		}

		// make sure walls are all accounted for, and only belong to one sector
		Set<Integer> closedList = new HashSet<>();
        for(int sectorId = 0; sectorId < this.sectors.size(); ++sectorId){
        	Sector sector = getSector(sectorId);
        	Set<Integer> sectorWallIds = new HashSet<>(this.getAllSectorWallIds(sector));
        	for(Integer wallId : sectorWallIds){
        		if(closedList.contains(wallId)){
        			throw new Exception("wall appears twice id=" + wallId);
				}else{
        			closedList.add(wallId);
				}
			}

        	// also check the loops // TODO - check the other loops
			for(Integer wallId: this.getWallLoop(sector.getFirstWall())){
				if(!sectorWallIds.contains(wallId)){
					throw new RuntimeException("wall loop broken");
				}
			}
		}

        if(closedList.size() != this.walls.size()){
        	throw new Exception("walls are missing or unused");
		}
		Set<Integer> seenPoint2 = new TreeSet<>();

		// make sure every redwall is pointing to a valid sector and a matching redwall?
        // and make sure point2 is valid
        for(int wallId = 0; wallId < this.walls.size(); ++wallId){
        	Wall w = this.getWall(wallId);
        	if(w.isRedWall()){
        		assertValidSector(w.nextSector);
        		Wall otherWall = getWall(w.nextWall);
        		assertValidSector(otherWall.nextSector);
        		if(wallId != otherWall.nextWall){
        			throw new Exception("redwalls dont match");
				}
			}
			if(seenPoint2.contains(w.getPoint2Id())){
				throw new Exception("wall id " + w.getPoint2Id() + " appears more than once");
			}else{
				seenPoint2.add(w.getPoint2Id());
			}
        	if(w.getPoint2Id() < 0 || w.getPoint2Id() >= this.wallCount){
        		throw new Exception("wall is invalid point2: " + w.getPoint2Id());
			}
		}

	}

	/**
	 * Deletes all sprites matching a filter.
	 * @param filters
	 * @return the number of sprites deleted.
	 */
	public int deleteSprites(ISpriteFilter... filters){
		Iterator<Sprite> it = sprites.iterator();
		int count = 0;
		while(it.hasNext()){
			if(SpriteFilter.matchAll(it.next(), filters)){
				it.remove();
				count += 1;
			}
		}
		this.spriteCount = this.sprites.size();
		return count;
	}

	public List<Sprite> findSprites(ISpriteFilter... filters){
		List<Sprite> results = new ArrayList<Sprite>(sprites.size());
		for(Sprite s : sprites){
			if(! SpriteFilter.matchAll(s, filters)){
				continue;
			}
			results.add(s);
		}
		return results;
	}

	// having trouble with scala vs java varargs -- will figure it out later
	public List<Sprite> findSprites4Scala(List<ISpriteFilter> filters){
		List<Sprite> results = new ArrayList<Sprite>(sprites.size());
		for(Sprite s : sprites){
			if(! SpriteFilter.matchAll(s, filters)){
				continue;
			}
			results.add(s);
		}
		return results;
	}
	
	public List<Sprite> findSprites(Integer picnum, Integer lotag, Integer sectorId){
		// later:  use Wall.nextSector
		
		List<Sprite> results = new ArrayList<Sprite>(sprites.size());
		for(Sprite s : sprites){
			if(picnum != null && picnum != s.picnum){
				continue;
			}else if(lotag != null && lotag != s.lotag){
				continue;
			}else if(sectorId != null && sectorId != s.sectnum){
				continue;
			}
			
			results.add(s);
		}
		return results;
	}
	
	// /**
	//  * TODO:  this doesnt handle inner sectors yet!
	//  *
	//  * Gets all of the adjacent sectors (sectors that share a red wall)
	//  * @param sectorId - sector id (a.k.a. secnum) of the starting sector
	//  * @return set of sector Ids that share a red wall with this sector
	//  */
	// public Set<Integer> getAdjacentSectors(int sectorId){
	// 	Set<Integer> results = new TreeSet<Integer>();
	// 	if(sectorId < 0 || sectorId >= this.sectors.size()){
	// 		throw new IllegalArgumentException();
	// 	}
	//
	// 	Sector sector = this.sectors.get(sectorId);
	// 	List<Integer> walls = this.getWallLoop(sector.getFirstWall());
	// 	for(int i : walls){
	// 		int otherSector = this.getWall(i).nextSector;
	// 		if(otherSector != -1){
	// 			results.add(otherSector);
	// 		}
	// 	}
	// 	return results;
	// }
	
	// /**
	//  * Do a really dumb point average to guess the center of a sector.
	//  * For convex sectors this could be totally wrong.
	//  *
	//  * @param sectorId
	//  * @return
	//  */
	// public PointXYZ guessCenter(int sectorId){
	// 	Sector sector = this.getSector(sectorId);
	// 	List<Integer> walls = this.getFirstWallLoop(sector);
	//
	// 	List<Integer> xvalues = new ArrayList<Integer>(walls.size());
	// 	List<Integer> yvalues = new ArrayList<Integer>(walls.size());
	// 	for(int wi : walls){
	// 		xvalues.add(this.getWall(wi).x);
	// 		yvalues.add(this.getWall(wi).y);
	// 	}
	// 	return new PointXYZ(MapUtil.average(xvalues), MapUtil.average(yvalues), sector.getFloorZ());
	// }
	
	
	public void print(){
		System.out.println("map version: " + mapVersion);
		System.out.println("player start: " + playerStart.toString());
		System.out.println("sector with start point: " + sectorWithStartPoint);
		System.out.println("sector count: " + sectorCount);
		System.out.println("wall count: " + wallCount);
		System.out.println("sprite count: " + spriteCount);
	}
	
	public void printAll(){
		print();
		System.out.println(this.playerStart.toString());
		System.out.println("\nSECTORS\n");
		for(int i = 0; i < sectors.size(); ++i){
			System.out.print("sector " + i + ": ");
			sectors.get(i).print();
			System.out.println();
		}
		System.out.println("WALLS");
		for(int i = 0; i < walls.size(); ++i){
			System.out.print("wall " + i + ": ");
			System.out.println(walls.get(i).toString());
			System.out.println();
		}
		System.out.println("SPRITES");
		for(int i = 0; i < sprites.size(); ++i){
			System.out.println(sprites.get(i).toString());
		}
	}

	// for debugging
	public void printTable(){
		System.out.println("map version: " + mapVersion);
		System.out.println("player start: " + playerStart.toString());
		System.out.println("sector with start point: " + sectorWithStartPoint);
		System.out.println("sector count: " + sectorCount);
		System.out.println("wall count: " + wallCount);
		System.out.println("sprite count: " + spriteCount);

		for(int i = 0; i < sectorCount; ++i){
			Sector sector = this.getSector(i);
			System.out.println(i + "  firstWall=" + sector.getFirstWall() + " wallCount=" + sector.getWallCount());
		}
		System.out.println("--------------------");
		for(int i = 0; i < wallCount; ++i){
			Wall wall = this.getWall(i);
			System.out.println("wall " + i + " | point2=" + wall.getPoint2Id());
		}
		System.out.println("--------------------");

	}
	
	
	public void toBytes(OutputStream output) throws IOException {
		if(sprites.size() != this.spriteCount) throw new IllegalStateException("sprite count messed up");
		if(this.playerStart == null) throw new IllegalStateException("no player start");
		
		ByteUtil.writeUint32LE(output, mapVersion);
		this.playerStart.toBytes(output);
		
		ByteUtil.writeUint16LE(output, sectorWithStartPoint);
		ByteUtil.writeUint16LE(output, sectorCount);
		for(int i = 0; i < sectorCount; ++i){
			sectors.get(i).toBytes(output);
		}
		
		ByteUtil.writeUint16LE(output, wallCount);
		for(int i = 0; i < wallCount; ++i){
			walls.get(i).toBytes(output);
		}
		
		ByteUtil.writeUint16LE(output, spriteCount);
		for(int i = 0; i < spriteCount; ++i){
			sprites.get(i).toBytes(output);
		}
		
		
	}

	public static Map readMap(byte[] bytes) throws IOException {
	    return readMap(new ByteArrayInputStream(bytes));
	}

	public static Map readMap(InputStream bs) throws IOException {
		long mapVersion = ByteUtil.readUint32LE(bs);
		Map map = new Map(mapVersion);

		map.playerStart = PlayerStart.readPlayerStart(bs);

		map.sectorWithStartPoint = ByteUtil.readUint16LE(bs); //NOTE:  that wiki page is wrong here!

		map.sectorCount = ByteUtil.readUint16LE(bs);

		map.sectors = new ArrayList<Sector>(map.sectorCount);
				for(int i = 0; i < map.sectorCount; ++i){
					map.sectors.add(Sector.readSector(bs));
					//map.sectors[i] = Sector.readSector(bs);
					//map.sectors[i].print();
				}
				
		map.wallCount = ByteUtil.readUint16LE(bs);
				
				
		map.walls = new ArrayList<Wall>(map.wallCount);
		for(int i = 0; i < map.wallCount; ++i){
			map.walls.add(Wall.readWall(bs));
			//map.walls.set(i, Wall.readWall(bs));
			//map.walls[i] = Wall.readWall(bs);
		}
				
		map.spriteCount = ByteUtil.readUint16LE(bs);
		
				
		//map.sprites = new Sprite[map.spriteCount];
		map.sprites = new ArrayList<Sprite>(map.spriteCount);
		for(int i = 0; i < map.spriteCount; ++i){
			map.sprites.add(Sprite.readSprite(bs));
		}
		
		//System.out.println("read method returns: " + bs.read()); //-1 [probably] means EOF, meaning we did it right.
		if(-1 != bs.read()){
			throw new IOException("data left over at end of file");
		}
        return map;
	}


	/**
	 * Gets the determinant of two PointXY's, treating them as columns.  In other words, it computes:
	 *
	 * |                  |
	 * | [  p0.x  p1.x  ] |
	 * | [  p0.y  p1.y  ] |
	 * |                  |
	 *
	 * @param p0
	 * @param p1
	 * @return
	 */
	static int determinant(PointXY p0, PointXY p1){
		return p0.x * p1.y - p0.y * p1.x;
	}

	/**
	 *
	 * TODO - this belons on a math-related class, not here
	 * @param points
	 * @return true if the points are clockwise FROM THE POV OF THE BUILD EDITOR...which probably actualls means they
	 * are counter clockwise.
	 */
	static boolean isClockwise(PointXY ... points){
	    if(points.length < 3) throw new IllegalArgumentException("need at least three points");

		int sum = 0;
		for(int i = 0; i < points.length; ++i){
			int j = (i + 1) % points.length;
			sum += determinant(points[i], points[j]);
		}
		return sum > 0;
	}

	public static boolean isClockwise(Collection<PointXY> points){
		return isClockwise(points.toArray(new PointXY[]{}));
	}


	private static List<WallView> followWallsForJoin(
			int sectorIdA,
			int sectorIdB,
			WallView startWall,
			List<WallView> path,
			java.util.Map<PointXY, List<WallView>> p1map // must contain ONLY walls in sector A and B
	){
		if(sectorIdA == sectorIdB){
			throw new IllegalArgumentException("sectorIdA cannot equal sectorIdB");
		}
		if(startWall == null){
			throw new IllegalArgumentException("start wall cannot be null");
		}
		if(path == null || path.isEmpty()){
			throw new IllegalArgumentException("path cannot be empty");
		}

		WallView currentWall = path.get(path.size() - 1);

		WallView nextWall = null;
		for(WallView n: p1map.get(currentWall.p2())){
			if(n.otherSectorId() != sectorIdA && n.otherSectorId() != sectorIdB){
			    if(nextWall == null){
			    	nextWall = n;
				}else{
			    	System.out.println("other sector: " + nextWall.otherSectorId());
			        String walls = String.format(" wall ids: %d, %d", nextWall.getWallId(), n.getWallId());
			    	String msg = "sector join: more than one wall with start point " + currentWall.p1();
			    	throw new MathIsHardException(msg + walls + " - ensure you filtered by sector first");
				}
			}
		}
		if(nextWall == null){
			throw new MathIsHardException("could not find wall to follow wall id " + currentWall.getWallId());
		}

		if(nextWall.equals(startWall)) {
			return path;
		}else if(nextWall.getWallId() == startWall.getWallId()){
			throw new MathIsHardException("this shouldn't happen");
		}else{
			// System.out.println("adding " + nextWall.getWallId() + " start wall is " + startWall.getWallId());
			path.add(nextWall);
			return followWallsForJoin(sectorIdA, sectorIdB, startWall, path, p1map);
		}
	}

	/**
	 * Internal utility method used when joining walls.
     *
	 * Given a start wall, this finds all of the walls in that loop.
	 */
	static List<WallView> followWallsForJoin(
			int sectorIdA,
			int sectorIdB,
			WallView startWall,
			java.util.Map<PointXY, List<WallView>> p1map // must contain ONLY walls in sector A and B
	){

		List<WallView> path = new ArrayList<WallView>();
		path.add(startWall);

		return followWallsForJoin(sectorIdA, sectorIdB, startWall, path, p1map);
	}

	static List<List<WallView>> followAllWallsForJoin(
			int sectorIdA,
			int sectorIdB,
			List<WallView> walls // must pass in ALL walls for the two sectors
	){
		if(sectorIdA == sectorIdB){
			throw new IllegalArgumentException("sectorIdA cannot equal sectorIdB");
		}
	    Set<Integer> distinctCheck = new HashSet<>();
	    for(WallView wv: walls){
	    	if(distinctCheck.contains(wv.getWallId())){
	    		throw new IllegalArgumentException("walls param contains walls with duplicate ids: " + wv.getWallId());
			}else{
	    		distinctCheck.add(wv.getWallId());
			}
		}

		java.util.Map<PointXY, List<WallView>> p1map = new HashMap<>();
		for(WallView w: walls){
			if(!p1map.containsKey(w.p1())){
				p1map.put(w.p1(), new LinkedList<>());
			}
			p1map.get(w.p1()).add(w);
		}

		Set<Integer> closedList = new HashSet<>();
		List<WallView> openList = new ArrayList<>(walls);
		List<List<WallView>> results = new ArrayList<>();
		while(openList.size() > 0){
		    WallView start = openList.remove(0);
		    if((start.otherSectorId() != sectorIdA && start.otherSectorId() != sectorIdB) && !closedList.contains(start.getWallId())){
		        closedList.add(start.getWallId());
				List<WallView> path = followWallsForJoin(sectorIdA, sectorIdB, start, p1map);
				for(WallView wv: path){
					closedList.add(wv.getWallId());
				}
				results.add(path);
			}
		}
		return results;
	}

	/**
     * Internal utility method for wall joining.
	 *
	 * Sorts the input so that the wall with the given id is the first wall in the first list.
	 */
	static List<List<WallView>> firstWallFirst(List<List<WallView>> walls, int firstWallId){
	    if(walls.isEmpty()) throw new IllegalArgumentException("walls cannot be empty");

	    List<List<WallView>> results = new ArrayList<>(walls.size());
	    boolean found = false;
	    for(List<WallView> loop: walls){
	    	Integer firstWallIndex = null;
	    	for(int i = 0; i < loop.size(); ++i){
	    		if(loop.get(i).getWallId() == firstWallId){
	    			firstWallIndex = i;
	    			found = true;
				}
			}
	    	if(firstWallIndex == null){
	    		results.add(loop);
			}else{
	    		List<WallView> reordered = new ArrayList<WallView>(loop.size());
	    		reordered.addAll(loop.subList(firstWallIndex, loop.size())); // ending is exclusive
				reordered.addAll(loop.subList(0, firstWallIndex));
	    		results.add(0, reordered);
			}
		}
	    if(!found){
	    	throw new IllegalArgumentException(String.format("first wall id %d not in input", firstWallId));
		}
	    return results;
	}

}
