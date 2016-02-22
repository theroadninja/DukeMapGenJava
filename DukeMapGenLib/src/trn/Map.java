package trn;

import java.io.IOException;
import java.io.InputStream;

public class Map {
	
	long mapVersion;
	
	PlayerStart playerStart;
	
	long sectorWithStartPoint;
	
	int sectorCount;
	
	Sector[] sectors;
	
	int wallCount;
	
	Wall walls[];
	
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
		return sectors[i];
	}
	
	public int getWallCount(){
		return this.wallCount;
	}
	
	public Wall getWall(int i){
		return this.walls[i];
	}
	
	public int getSpriteCount(){
		return this.spriteCount;
	}
	
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
	
	public static Map readMap(InputStream bs) throws IOException {
		
		Map map = new Map();
		
		//long mapVersion = ByteUtil.readUint32LE(mapFile, 0);
		map.mapVersion = ByteUtil.readUint32LE(bs);

		map.playerStart = PlayerStart.fromBytes(bs);
				
				
		map.sectorWithStartPoint = ByteUtil.readUint16LE(bs); //NOTE:  that wiki page is wrong here!
				
				
		map.sectorCount = ByteUtil.readUint16LE(bs);

		map.sectors = new Sector[map.sectorCount];
				for(int i = 0; i < map.sectorCount; ++i){
					map.sectors[i] = Sector.readSector(bs);
					map.sectors[i].print();
				}
				
		map.wallCount = ByteUtil.readUint16LE(bs);
				
				
		map.walls = new Wall[map.wallCount];
		for(int i = 0; i < map.wallCount; ++i){
			map.walls[i] = Wall.readWall(bs);
		}
				
		map.spriteCount = ByteUtil.readUint16LE(bs);
		
				
		map.sprites = new Sprite[map.spriteCount];
		for(int i = 0; i < map.spriteCount; ++i){
			map.sprites[i] = Sprite.readSprite(bs);
					
			System.out.println("sprite texture: " + map.sprites[i].picnum); //oneroom.map should be 22 and 24
		}
				
		System.out.println("read method returns: " + bs.read()); //-1 [probably] means EOF, meaning we did it right.
				
        return map;
	}

}
