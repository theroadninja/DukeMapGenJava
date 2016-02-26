package trn.duke.experiments;

import java.io.IOException;

import trn.Main;
import trn.Map;
import trn.Sector;
import trn.Wall;


/**
 * Takes a map that has one sector and adds a second sector.
 * 
 * @author Dave
 *
 */
public class E3AddRoom {
	
	public static void main(String[] args) throws IOException{
		
		Map m = Main.loadMap("RT3.MAP");
		go(m);
		Main.writeResult(m);
		
	}

	public static void go(Map map){
	
		short wallTex = 191;
		
		//Textures:
		//wall to be replaced is 503 (glass)
		//other walls are 191
		//floor is 183
		//ceiling is 184
		
		map.getSector(0).print();
		
		System.out.println("----------");
		
		for(int i = 0; i < 4; ++i){
			System.out.println(map.getWall(i).toString());
		}
		
		//note: a single large grid width appears to be 1024.
		//our room is 2x2 large grid squares, and we are adding another 2x2 large grid square
		
		
		
		Wall w4 = new Wall(31744, 31744, wallTex); //same position as wall 0
		w4.setXRepeat(16);
		w4.setYRepeat(8);
		//map.addWall(w4);
		
		Wall w5 = new Wall(31744, 29696, wallTex);
		w5.setXRepeat(16);
		w5.setYRepeat(8);
		//map.addWall(w5);
		
		
		Wall w6 = new Wall(33792, 29696, wallTex);
		w6.setXRepeat(16);
		w6.setYRepeat(8);
		//map.addWall(w6);
		
		
		Wall w7 = new Wall(33792, 31744, wallTex);
		w7.setOtherSide(0, 0);
		w7.setXRepeat(16);
		w7.setYRepeat(8);
		//map.addWall(w7);
		
		map.addLoop(w4, w5, w6, w7);
		
		
		map.getWall(0).setOtherSide(7, 1);
		
		
		Sector s0 = map.getSector(0);
		
		Sector sector = new Sector(4, 4);
		sector.setCeilingZ(s0.getCeilingZ());
		sector.setFloorZ(s0.getFloorZ());
		sector.setCeilingTexture(s0.getCeilingTexture());
		sector.setFloorTexture(s0.getFloorTexture());
		
		map.addSector(sector);
		
		
		
		System.out.println("----------------");
		
		for(int i = 4; i < 8; ++i){
			System.out.println(map.getWall(i).toString());
		}
		
		
		
		
		
		
	}
}
