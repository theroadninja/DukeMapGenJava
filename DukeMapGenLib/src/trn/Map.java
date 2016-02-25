package trn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.List;

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
	
	Sprite[] sprites;
	
	public long getMapVersion(){
		return mapVersion;
	}
	
	public PlayerStart getPlayerStart(){
		return this.playerStart;
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
	
	public int getSpriteCount(){
		return this.spriteCount;
	}
	
	public int spriteCount(){ return getSpriteCount(); }
	
	public Sprite getSprite(int i){
		return sprites[i];
	}
	
	
	public void print(){
		System.out.println("map version: " + mapVersion);
		
		System.out.println("player start: " + playerStart.toString());
		
		System.out.println("sector with start point: " + sectorWithStartPoint);
		
		System.out.println("sector count: " + sectorCount);
		
		System.out.println("wall count: " + wallCount);
		
		System.out.println("sprite count: " + spriteCount);
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
			sprites[i].toBytes(output);
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
		
				
		map.sprites = new Sprite[map.spriteCount];
		for(int i = 0; i < map.spriteCount; ++i){
			map.sprites[i] = Sprite.readSprite(bs);
					
			System.out.println("sprite texture: " + map.sprites[i].picnum); //oneroom.map should be 22 and 24
		}
		
		//System.out.println("read method returns: " + bs.read()); //-1 [probably] means EOF, meaning we did it right.
		if(-1 != bs.read()){
			throw new IOException("data left over at end of file");
		}
		
		
				
        return map;
	}

}
