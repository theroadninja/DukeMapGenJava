package trn;

import trn.duke.MapErrorException;

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


	long mapVersion;
	
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

	public ImmutableMap readOnly(){
		return new ImmutableMap(this);
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

	public int getPreviousWall(int wallIndex) throws MapErrorException {
		if(wallIndex < 0) throw new IllegalArgumentException();
		List<Integer> loop = getWallLoop(wallIndex);
		for(int i = 0; i < loop.size(); ++i){
			Wall pw = getWall(i);
			if(wallIndex == pw.point2){
				return wallIndex;
			}
		}
		throw new MapErrorException("unable to find previous wall for wall " + wallIndex);
	}
	
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
	
	/**
	 * adds the walls and the new sector object.
	 * 
	 * @param wallsToAdd
	 * @return
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
	 * connects two sectors by creating the necessary link between the sectors' walls,
	 * creating two-sided walls (which appear red in build); 
	 */
	public void linkRedWalls(int sectorIndex, int wallIndex, int sectorIndex2, int wallIndex2){

		linkRedWallsStrict(sectorIndex, wallIndex, sectorIndex2, wallIndex2);

	    // old implementation
		// getWall(wallIndex).setOtherSide(wallIndex2, sectorIndex2);
		// getWall(wallIndex2).setOtherSide(wallIndex, sectorIndex);
	}

	public void linkRedWallsStrict(int sectorIndex, int wallIndex, int sectorIndex2, int wallIndex2){
		Wall w1 = getWall(wallIndex);
		// TODO - instead of throwing, just disable...
		if(w1.getStat().blockPlayer()) throw new RuntimeException("wall has blocking enabled");
		Wall w1End = getWall(w1.getPoint2Id());
		Wall w2 = getWall(wallIndex2);
		if(w2.getStat().blockPlayer()) throw new RuntimeException("wall has blocking enabled");
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

		// 6. shift all walls of other sectors
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

        	// also check the loops
			for(Integer wallId: this.getWallLoop(sector.getFirstWall())){
				if(!sectorWallIds.contains(wallId)){
					throw new RuntimeException("wall loop broken");
				}
			}
		}

        if(closedList.size() != this.walls.size()){
        	throw new Exception("walls are missing or unused");
		}

        // make sure every redwall is pointing to a valid sector and a matching redwall?
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
	
	/**
	 * TODO:  this doesnt handle inner sectors yet!
	 * 
	 * Gets all of the adjacent sectors (sectors that share a red wall)
	 * @param sectorId - sector id (a.k.a. secnum) of the starting sector
	 * @return set of sector Ids that share a red wall with this sector
	 */
	public Set<Integer> getAdjacentSectors(int sectorId){
		Set<Integer> results = new TreeSet<Integer>();
		if(sectorId < 0 || sectorId >= this.sectors.size()){
			throw new IllegalArgumentException();
		}
		
		Sector sector = this.sectors.get(sectorId);
		List<Integer> walls = this.getWallLoop(sector.getFirstWall());
		for(int i : walls){
			int otherSector = this.getWall(i).nextSector;
			if(otherSector != -1){
				results.add(otherSector);
			}
		}
		return results;
	}
	
	/**
	 * Do a really dumb point average to guess the center of a sector.
	 * For convex sectors this could be totally wrong.
	 * 
	 * @param sectorId
	 * @return
	 */
	public PointXYZ guessCenter(int sectorId){
		Sector sector = this.getSector(sectorId);
		List<Integer> walls = this.getFirstWallLoop(sector);
		
		List<Integer> xvalues = new ArrayList<Integer>(walls.size());
		List<Integer> yvalues = new ArrayList<Integer>(walls.size());
		for(int wi : walls){
			xvalues.add(this.getWall(wi).x);
			yvalues.add(this.getWall(wi).y);
		}
		return new PointXYZ(MapUtil.average(xvalues), MapUtil.average(yvalues), sector.getFloorZ());
	}
	
	
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
		
		Map map = new Map();
		
		//long mapVersion = ByteUtil.readUint32LE(mapFile, 0);
		map.mapVersion = ByteUtil.readUint32LE(bs);

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
			//map.sprites[i] = Sprite.readSprite(bs);
					
			//System.out.println("sprite texture: " + map.sprites[i].picnum); //oneroom.map should be 22 and 24
		}
		
		//System.out.println("read method returns: " + bs.read()); //-1 [probably] means EOF, meaning we did it right.
		if(-1 != bs.read()){
			throw new IOException("data left over at end of file");
		}
		
		
				
        return map;
	}

}
