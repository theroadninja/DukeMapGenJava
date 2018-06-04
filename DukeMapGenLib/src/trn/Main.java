package trn;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Collections;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;

import trn.duke.experiments.E1RandomSprites;
import trn.prefab.Connector;
import trn.prefab.PastedSectorGroup;
import trn.prefab.PrefabPalette;
import trn.prefab.PrefabUtils;

public class Main {

	
	//found this, looks like a good resource for the build map format:
	// http://www.shikadi.net/moddingwiki/MAP_Format_%28Build%29
	
	/** TODO:  remove when I have some real unit tests */
	public static final String HELLO = "hello world";
	
	static String DOSPATH = "C:/Users/Dave/Dropbox/workspace/dosdrive/duke3d/";
	
	public static void main(String[] args) throws Exception {
		

		
		//Map fromMap = loadMap(DOSPATH + "cptest2.map");
		
		// cptest4.map - space joins
		
		// cptest3.map
		//  group 10 is the plus sign hallway intersection
		//		connector on left:  connector id 123 and wall lotag 2
		//		connector on right: connector id 456 and wall lotag 1
		//
		Map fromMap = loadMap(DOSPATH + "cptest3.map");
		
		
		

		
		Map outMap = Map.createNew();
		
		
		
		PrefabPalette palette = new PrefabPalette();
		palette.loadAllGroups(fromMap);
		
		
		
		PastedSectorGroup psg1 = palette.pasteSectorGroup(10, outMap, new PointXYZ(-1024*30, -1024*50, 0));
		
		// connector on left side of group
		Connector conn2 = psg1.getConnector(123);

		Connector conn1_beforepaste = palette.getConnector(10, 456);
		
		PointXYZ delta = conn1_beforepaste.getTransformTo(conn2);

		PastedSectorGroup psg2 = palette.pasteSectorGroup(10, outMap, delta);

		conn1_beforepaste.translateIds(psg2.copystate.idmap); // sector and wall ids
		
		Connector conn1_afterpaste = psg2.getConnector(456);
		
		//PrefabUtils.joinWalls(outMap, conn1_beforepaste, conn2);
		PrefabUtils.joinWalls(outMap, conn1_afterpaste, conn2);
		
		
		// add a third group!
		PastedSectorGroup psg3 = null;
		{
			Connector leftEdge = psg2.getConnector(123);
			
			//Connector rightEdge = palette.getConnector(10, 456);
			//PointXYZ cdelta = rightEdge.getTransformTo(leftEdge);
			psg3 = palette.pasteAndLink(12, 456, outMap, leftEdge);
			
			//PastedSectorGroup psg3 = palette.pasteSectorGroup(10, outMap, cdelta);
			
		}
		
		// now try to add the player start group - 11
		{
			Connector leftEdge = psg3.getConnector(123);
			
			PastedSectorGroup psg4 = palette.pasteAndLink(11, 456, outMap, leftEdge);
			
			
		}
		
		
		ISpriteFilter psfilter = SpriteFilter.playerstart();
		List<Sprite> sprites = outMap.findSprites(psfilter);
		
		System.out.println("filter matches = " + psfilter.matches(sprites.get(0)));
		System.out.println("sprite is: " + sprites.get(0));
		
		if(sprites.size() != 1){
			throw new RuntimeException("wft? sprite count is " + sprites.size());
		}
		Sprite pstart = outMap.findSprites(psfilter).iterator().next();
		
		
		outMap.setPlayerStart(new PlayerStart(pstart.getLocation(),0));
		
		
		
		
		outMap.deleteSprites(SpriteFilter.texture(PrefabUtils.MARKER_SPRITE_TEX));

		deployTest(outMap);
		
	}
	
	
	public static Map loadMap(String filename) throws IOException{
		File path = new File(filename);
		if(path.isAbsolute()){
			// C:/Users/Dave/Dropbox/workspace/dosdrive/duke3d/
			return loadMap(path);
		}else{
			return loadMap(new File(System.getProperty("user.dir") + File.separator + "testdata" + File.separator, filename));
		}
	}
	
	/*public static Map loadMap(String folder, String filename) throws IOException{
		
		//String fname = "TWOROOMS.MAP";
		
		//String mapFile = System.getProperty("user.dir") + File.separator + "testdata" + File.separator + filename;
		String mapFile = folder + filename;
		System.out.println("loading mapfile: " + mapFile);
		
		FileInputStream bs = new FileInputStream(new File(mapFile));
		
		Map map = Map.readMap(bs);
		
		return map;
	}*/
	
	public static Map loadMap(File mapfile) throws IOException {
		FileInputStream bs = new FileInputStream(mapfile);
		Map map = Map.readMap(bs);
		return map;
	}
	
	public static void writeResult(Map map) throws IOException{
		String resultsFile = System.getProperty("user.dir") + File.separator + "dukeoutput" + File.separator + "output.map";
		writeResult(map, resultsFile);
	}
	public static void writeResult(Map map, String resultsFile) throws IOException{
		
		FileOutputStream output = new FileOutputStream(new File(resultsFile)); 
		map.toBytes(output);
		output.close();
	}
	
	/**
	 * writes the map and copies to hardcoded duke3d path in dosbox drive.
	 * @param map
	 * @throws IOException
	 */
	public static void deployTest(Map map) throws IOException{
		String filename = "output.map";
		String resultsFile = System.getProperty("user.dir") + File.separator + "dukeoutput" + File.separator + filename;
		Main.writeResult(map, resultsFile);
		
		//TODO:  this should go in some conf/ini/json file that is not checked in
		
		String copyDest = "C:/Users/Dave/Dropbox/workspace/dosdrive/duke3d/" + filename;
		FileUtils.copyFile(new File(resultsFile), new File(copyDest));
		System.out.println("map generated: " + filename);
		
		//TODO:  can we put build times in the map somewhere?
		
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
			System.out.println(map.getWall(i).toString(i));
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
