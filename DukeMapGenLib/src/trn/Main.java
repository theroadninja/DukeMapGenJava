package trn;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import trn.duke.experiments.E1RandomSprites;
import trn.duke.experiments.E2XRepeat;
import trn.duke.experiments.E3AddRoom;

public class Main {

	
	//found this, looks like a good resource for the build map format:
	// http://www.shikadi.net/moddingwiki/MAP_Format_%28Build%29
	
	/** TODO:  remove when I have some real unit tests */
	public static final String HELLO = "hello world";
	
	
	public static void main(String[] args) throws Exception {
		
		System.exit(0);
		
		
		
		
		
		/*
		Map m = loadMap("RT0.MAP");
		E2XRepeat.go(m);
		writeResult(m);
		System.exit(0);
		*/
		
		
		//RT0.MAP is for hardcoding experimentation
		
		
		printWalls(loadMap("RT0.MAP"));
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		

		

		
	}
	
	public static Map loadMap(String filename) throws IOException{
		
		//String fname = "TWOROOMS.MAP";
		
		String mapFile = System.getProperty("user.dir") + File.separator + "testdata" + File.separator + filename;
		System.out.println("loading mapfile: " + mapFile);
		
		FileInputStream bs = new FileInputStream(new File(mapFile));
		
		Map map = Map.readMap(bs);
		
		return map;
	}
	
	
	public static void writeResult(Map map) throws IOException{
		String resultsFile = System.getProperty("user.dir") + File.separator + "dukeoutput" + File.separator + "output.map";
		FileOutputStream output = new FileOutputStream(new File(resultsFile)); 
		map.toBytes(output);
		output.close();
	}
	
	public static void runExperiment1() throws Exception {
		FileInputStream bs = new FileInputStream(E1RandomSprites.intputFile());
		
		Map map = Map.readMap(bs);
		
		
		E1RandomSprites.randomSprites(map);
		
		//ByteArrayOutputStream bs2 = new ByteArrayOutputStream();
		
		//TODO:  dukeoutput is ignored by git; need to create folder if its not there
		String resultsFile = System.getProperty("user.dir") + File.separator + "dukeoutput" + File.separator + "output.map";
		
		
		FileOutputStream output = new FileOutputStream(new File(resultsFile)); 
		map.toBytes(output);
		output.close();
	}
	
	public static void printWalls(Map map) throws Exception {
		/*
		String fname =  "ONEROOM.MAP";
		//String fname = "TWOROOMS.MAP";
		
		String mapFile = System.getProperty("user.dir") + File.separator + "testdata" + File.separator + fname;
		
		FileInputStream bs = new FileInputStream(new File(mapFile));
		
		Map map = Map.readMap(bs);
		*/
		
		System.out.println();
		System.out.println();
		System.out.println();
		
		for(int i = 0; i < map.getWallCount(); ++i){
			System.out.println(map.getWall(i).toString());
		}
	}
	
	public static void oldmain(String[] args) throws Exception {
		
		
		//some ideas:
		//
		//
		// X    change a random sprite?
		//
		// change set of textures?
		//
		// i guess focus on sprites first...
		//
		// random texture too?  or maybe a set of textures?
		//
		// translate an entire room? (could be super tricky...read walls...)
		//
		// copy a room?
		//
		// generate a simple room?  (need much better understanding of all fields...
		//
		// generate a boring maze
		
		
		
		
		
		
		String fname =  "ONEROOM.MAP";
		//String fname = "TWOROOMS.MAP";
		
		String filepath = System.getProperty("user.dir") + File.separator + "testdata" + File.separator + fname;
		
		
		File f = new File(filepath);
		if(f.exists() && f.isFile()){
		
			byte[] mapFile = IOUtils.toByteArray(new FileInputStream(f));
			parseOnTheFly(mapFile);
			
		}else{
			System.err.println(String.format("%s is not a valid file", filepath));
			System.exit(1);
		}
		
	}
	

	public static void parseOnTheFly(byte[] mapFile) throws IOException{
		
		ByteArrayInputStream bs = new ByteArrayInputStream(mapFile);
		
		Map map = Map.readMap(bs);
		

		
		
	}
}
