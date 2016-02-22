package trn;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.IOUtils;

import trn.duke.experiments.E1RandomSprites;

public class Main {

	
	//found this, looks like a good resource for the build map format:
	// http://www.shikadi.net/moddingwiki/MAP_Format_%28Build%29
	
	/** TODO:  remove when I have some real unit tests */
	public static final String HELLO = "hello world";
	
	
	public static void main(String[] args) throws Exception {
		
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
	
	public static void oldmain(String[] args) throws Exception {
		
		
		//some ideas:
		//
		//
		// change a random sprite?
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
