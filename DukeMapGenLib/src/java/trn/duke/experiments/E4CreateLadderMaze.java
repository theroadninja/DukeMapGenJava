package trn.duke.experiments;

import java.util.Random;

import trn.Main;
import trn.Map;
import trn.PlayerStart;
import trn.Sector;
import trn.Wall;
import trn.duke.Util;

/**
 * Map generator that starts by creating a square room, and then
 * creates more rooms either directly north (-y), or directly east (x+) 
 * @author Dave
 *
 */
public class E4CreateLadderMaze {

	public static void main(String[] args) throws Exception {
		
		final int MAZE_WALL_TEX = 772;
		final int WLENGTH = 2048; //1024 seems just big enough to fit duke comfortably

		E4CreateLadderMaze e = new E4CreateLadderMaze(WLENGTH, MAZE_WALL_TEX);
		//Main.writeResult(e.createMaze());
		Main.deployTest(e.createMaze());
	}

	int wallLength;
	int mazeWallTex;
	
	public E4CreateLadderMaze(int wallLength, int mazeWallTex){
		this.wallLength = wallLength;
		this.mazeWallTex = mazeWallTex;
	}
	
	int createFirstSector(Map map){
		int firstWall = -1;
		
		Wall sw = new Wall(0, 0);
		Wall nw = new Wall(0, -wallLength);
		Wall ne = new Wall(wallLength, -wallLength);
		Wall se = new Wall(wallLength, 0);
		
		sw.setTexture(mazeWallTex, 16, 8);
		nw.setTexture(mazeWallTex, 16, 8);
		ne.setTexture(mazeWallTex, 16, 8);
		se.setTexture(mazeWallTex, 16, 8);
		
		
		firstWall = map.addLoop(sw, nw, ne, se);
		
		Sector startSector = new Sector(firstWall, 4);
		
		startSector.setCeilingZ(Sector.DEFAULT_CEILING_Z);
		startSector.setFloorZ(Sector.DEFAULT_FLOOR_Z);
		
		return map.addSector(startSector);
	}

	int createRoomToEast(Map map, int lastSectorIndex){
		
		Integer[] eastWall = getEastPoints(map, map.getSector(lastSectorIndex).getFirstWall());
		Wall e0 = map.getWall(eastWall[0]);
		Wall e1 = map.getWall(eastWall[1]);
		
		if(e0.getX() != e1.getX()) throw new RuntimeException();
		
		int xwest = e0.getX();
		int ymin = Math.min(e0.getY(), e1.getY());
		int ymax = Math.max(e0.getY(), e1.getY());
		
		int xeast = xwest + wallLength;
		
		Wall sw = new Wall(xwest, ymax, mazeWallTex, 16, 8); //this is the first wall, and is adjacent to last sector
		Wall nw = new Wall(xwest, ymin, mazeWallTex, 16, 8);
		Wall ne = new Wall(xeast, ymin, mazeWallTex, 16, 8);
		Wall se = new Wall(xeast, ymax, mazeWallTex, 16, 8);
		
		int firstWall = map.addLoop(sw, nw, ne, se);
		
		Sector s = new Sector(firstWall, 4);
		int newSectorIndex = map.addSector(s);
		
		map.linkRedWalls(lastSectorIndex, eastWall[0],
				newSectorIndex, firstWall);
		
		return newSectorIndex;
	}
	
	
	int createRoomToNorth(Map map, int lastSectorIndex){
		
		Integer[] northWall = getNorthPoints(map, map.getSector(lastSectorIndex).getFirstWall());
		
		Wall n0 = map.getWall(northWall[0]);
		Wall n1 = map.getWall(northWall[1]);
		
		if(n0.getY() != n1.getY()) throw new RuntimeException();
		
		int ysouth = n0.getY();
		int xmin = Math.min(n0.getX(), n1.getX());
		int xmax = Math.max(n0.getX(), n1.getX());
		
		int ynorth = ysouth - wallLength;
		
		Wall sw = new Wall(xmin, ysouth, mazeWallTex, 16, 8); //this is the first wall
		Wall nw = new Wall(xmin, ynorth, mazeWallTex, 16, 8);
		Wall ne = new Wall(xmax, ynorth, mazeWallTex, 16, 8);
		Wall se = new Wall(xmax, ysouth, mazeWallTex, 16, 8); //this is the one that lines up with the last sectors north wall
		
		
		
		int firstWall = map.addLoop(sw, nw, ne, se);
		
		Sector s = new Sector(firstWall, 4);
		int newSectorIndex = map.addSector(s);
		
		//
		//link the new walls se wall
		//
		map.linkRedWalls(lastSectorIndex, northWall[0], 
				newSectorIndex, firstWall + 3);
		
		//n0.setOtherSide(firstWall + 3, newSectorIndex); //previous sector to this one
		//se.setOtherSide(northWall[0], lastSectorIndex); //this sector to previous one
		
		
		return newSectorIndex;
		
	}
	
	public Map createMaze(){
		Random random = new Random();
		Map map = Map.createNew();
		map.setPlayerStart(new PlayerStart(512, -512, 0, PlayerStart.NORTH));
		
		//
		//  create start room
		//

		int lastSectorIndex = createFirstSector(map); // map.addSector(startSector, WLENGTH);
		int roomCount = 10;
		for(int i = 0; i < roomCount; ++i){
			
			//
			// add a room at N or E
			//
			boolean north = random.nextBoolean();
			
			System.out.println("direction: " + north);
			if(north){
				//add it north of the last room
				
				lastSectorIndex = createRoomToNorth(map, lastSectorIndex);
				
				
			}else{
				//add it east of the last room
				
				lastSectorIndex = createRoomToEast(map, lastSectorIndex);
				
			}
		}

		return map;
	}
	
	
	
	/**
	 * this is only for square rooms
	 * 
	 * 
	 * @param map map containing the walls
	 * @param startWallIndex any wall on the loop
	 * @return array of 2 walls that define the points of the easternmost wall.  wall 0 is the actuall wall
	 */
	static Integer[] getEastPoints(Map map, int startWallIndex){

		Integer[] walls = new Integer[]{null, null};
		
		int safety = 10000;
		int index = startWallIndex;
		while(safety-- > 0){
			
			Wall w = map.getWall(index);
			
			/* abandoned attempt at abstraction
			if(null == walls[0] || comparator.compare(index, walls[0]) < 0){
				walls[1] = walls[0];
				walls[0] = index;
			}else if(null == walls[1] || comparator.compare(index, walls[0]) < 0){
				walls[1] = index;
			}*/
			
			if(null == walls[0] || w.getX() > map.getWall(walls[0]).getX()){
				walls[1] = walls[0];
				walls[0] = index;
			}else if(null == walls[1] || w.getX() > map.getWall(walls[1]).getX()){
				walls[1] = index;
			}
			
			index = w.getPoint2Id();
			if(index == startWallIndex){
				//we are back to where we started
				break;
			}
			
		}
		
		Util.orderWalls(map, walls);
		return walls;
		
	}
	
	/**
	 * this is only for square rooms
	 * 
	 * 
	 * @param map map containing the walls
	 * @param startWallIndex any wall on the loop
	 * @return array of 2 walls that define the points of the northernmost wall.  wall 0 is the actuall wall
	 */
	static Integer[] getNorthPoints(Map map, int startWallIndex){
		//Wall[] walls = new Wall[]{null, null};
		Integer[] walls = new Integer[]{null, null};
		
		int safety = 10000;
		int index = startWallIndex;
		while(safety-- > 0){
			
			Wall w = map.getWall(index);
			
			/* abandoned attempt at abstraction
			if(null == walls[0] || comparator.compare(index, walls[0]) < 0){
				walls[1] = walls[0];
				walls[0] = index;
			}else if(null == walls[1] || comparator.compare(index, walls[0]) < 0){
				walls[1] = index;
			}*/
			
			if(null == walls[0] || w.getY() < map.getWall(walls[0]).getY()){
				walls[1] = walls[0];
				walls[0] = index;
			}else if(null == walls[1] || w.getY() < map.getWall(walls[1]).getY()){
				walls[1] = index;
			}
			
			index = w.getPoint2Id();
			if(index == startWallIndex){
				//we are back to where we started
				break;
			}
			
		}
		
		Util.orderWalls(map, walls);
		return walls;
	}
	

	
	
}
