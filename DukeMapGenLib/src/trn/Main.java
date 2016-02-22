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

public class Main {

	
	//found this, looks like a good resource for the build map format:
	// http://www.shikadi.net/moddingwiki/MAP_Format_%28Build%29
	
	/** TODO:  remove when I have some real unit tests */
	public static final String HELLO = "hello world";
	
	
	public static void main(String[] args) throws Exception {
		
		
		Map m = loadMap("RT0.MAP");
		E2XRepeat.go(m);
		writeResult(m);
		System.exit(0);
		
		
		
		//RT0.MAP is for hardcoding experimentation
		
		
		printWalls(loadMap("RT0.MAP"));
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		
		//the following notes were made based on a signed/unsigned bug ...although that seems to have only affect high numbers
		//...so this field is still a bit of a mystery
		
		
		
		//with length half a large grid (i.e. 1 pentultimate grid space) we get xrepeat 4, and maybe half the texture is showing
		// does 8 always mean texture fits perfectly?
		
		
		
		//for a wall with length equal to 1 block of largest grid size, xrepeat = 8.
		//two blocks of largest grid:  xrepeat = 16
		//three blocks of largest grid:  xrepeat = 24
		//dividing xrepeat by grid size leaves us with 8...
		
		//is this really a repeat??  ... or is it PIXEL repeat and not TEXTURE repeat...
		
		//using the keypad to shrink x (i.e. cause it to repeat itself more) results in increase:  24 -> 31
	
		//making the nuke button texture fit wall exactly:  xrepeat 14, yrepeat 5
		
		//making tex 158 (door) texture fit wall exactly: xrepeat 15, 7
		
		//making tex 157 (half-door) texture fit wall exactly: 10, y
		
		//i think the textures are different sizes, and the repeat is related to that
		
		//it can't be pixel repeat though
		
		//same texture, ridiculous stretching:  xrepeat 0
		
		//same texture, increased wall length to 14 (i think):  xrepeat  98
		//	98 / 8 is about 12, but the texture only repeats 2 times
		
		
		//shrink it a lot, and xrepeat goes negative.....!!!!!!!
		
	}
	
	public static Map loadMap(String filename) throws IOException{
		
		//String fname = "TWOROOMS.MAP";
		
		String mapFile = System.getProperty("user.dir") + File.separator + "testdata" + File.separator + filename;
		
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
