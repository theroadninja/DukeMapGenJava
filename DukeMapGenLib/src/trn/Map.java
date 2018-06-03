package trn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;

public class Map {
	
	long mapVersion;
	
	PlayerStart playerStart;
	
	int sectorWithStartPoint;
	
	int sectorCount;
	
	List<Sector> sectors = new ArrayList<Sector>();
	
	//XXX: in theory we could get rid of this and rely on wall.size()
	int wallCount;
	
	List<Wall> walls = new ArrayList<Wall>();
	
	int spriteCount;
	
	List<Sprite> sprites = new ArrayList<Sprite>();
	
	private Map(){
		
	}
	
	public static Map createNew(){
		Map map = new Map();
		map.mapVersion = 7; //duke.  not sure if it includes atomic or not.
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
		return this.walls.get(i);
	}
	
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
	
	
	// TODO: !!!! i'm an idiot; the sector could have many wall loops...
	public List<Integer> getSecondWallLoop(final Sector sector){
		
		int loop1size = getFirstWallLoop(sector).size();
		if(loop1size > sector.getWallCount()){
			throw new RuntimeException("something went very wrong");
		}else if(loop1size == sector.getWallCount()){
			return null; // it has no second loop;
		}
		int loop2start = (loop1size + sector.getFirstWall());
		
		return this.getWallLoop(loop2start);
	}
	
	public Iterator<Collection<Integer>> wallLoopIterator(int sectorId){
		return new WallLoopIterator(this, sectorId);
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
			
			index = walls.get(index).getPoint2();
			
			if(index == wallIndex){
				break; //back to where we started
			}
		}
		
		return list;
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
	 * @param walls
	 * @return
	 */
	public int addLoop(Wall ... wallsToAdd){
		if(wallsToAdd == null || wallsToAdd.length < 3) throw new IllegalArgumentException();
		
		int firstWall = -1;
		Wall lastWall = null;
		
		for(Wall w : wallsToAdd){
			
			//add the wall
			int index = addWall(w);
			
			w.setPoint2(index + 1);
			
			//if its the first wall, update the index
			if(firstWall == -1){
				firstWall = index;
			}
			
			lastWall = w;	
		}
		
		lastWall.setPoint2(firstWall);
		
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
		
		getWall(wallIndex).setOtherSide(wallIndex2, sectorIndex2);
		getWall(wallIndex2).setOtherSide(wallIndex, sectorIndex);
		
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
	

	
	public List<Sprite> findSprites(ISpriteFilter... filters){
		List<Sprite> results = new ArrayList<Sprite>(sprites.size());
		for(Sprite s : sprites){
			if(! SpriteFilter.matchAll(s, filters)){
				continue;
			}
			//for(ISpriteFilter st : filters){
			//	if(! st.matches(s)){
			//		continue;
			//	}
			//}
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
