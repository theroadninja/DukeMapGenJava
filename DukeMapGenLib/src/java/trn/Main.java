package trn;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import org.apache.commons.io.FileUtils;
import trn.duke.MapImageWriter;
import trn.prefab.experiments.*;
import javax.imageio.ImageIO;

public class Main {

	//found this, looks like a good resource for the build map format:
	// http://www.shikadi.net/moddingwiki/MAP_Format_%28Build%29
	
	public static String DOSPATH = "C:/Users/Dave/Dropbox/workspace/dosdrive/duke3d/";
	
	public static void main(String[] args) throws Exception {

		File f = new File(DOSPATH);
		if(!(f.exists() && f.isDirectory())){
			throw new Exception(DOSPATH + " does not exist");
		}

		// Reference Styles
		// 1. copy test 3
		// 2. hyper 1
		// 3. hyper 2
		// 4. sound test
		//Map outMap = ChildSectorTest.run(MapLoader.loadMap(DOSPATH + ChildSectorTest.FILENAME()));

		Map outMap = FirstPrefabExperiment.run(MapLoader.loadMap(DOSPATH + "cptest3.map"));
		//Map outMap = GridExperiment.run(MapLoader.loadMap(DOSPATH + "cptest3.map"));

		// Map outMap = Hypercube1.run(MapLoader.loadMap(DOSPATH + "hyper1.map"));
		//Map outMap = Hypercube2.run(MapLoader.loadMap(DOSPATH + "hyper2.map"));
        //Map outMap = SoundListMap.run(MapLoader.loadMap(DOSPATH + SoundListMap.FILENAME()));
		//Map outMap = ReferenceTestExperiment.run(new MapLoader(DOSPATH));
		//Map outMap = PipeDream.run(new MapLoader(DOSPATH));
		//Map outMap = Sushi.run(new MapLoader(DOSPATH));

		//Map outMap = runDeleteSectorTest(5, new MapLoader(DOSPATH));

		// writeAndOpenMapPng(outMap);
		deployTest(outMap);

	}

	static Map runDeleteSectorTest(int spriteLotag, MapLoader mapLoader) throws IOException {
	    // TODO - this is copied from JavaTestUtils ...
		String fname = "DS.MAP";
		String filepath = System.getProperty("user.dir") + File.separator + "DukeMapGenLib" + File.separator + "testdata" + File.separator + fname;
		Map map = MapLoader.loadMap(filepath);
		//Map map = mapLoader.load("DS2.MAP");


		int sectorId = -1;
		for(int i = 0; i < map.spriteCount; ++i){
			if(map.getSprite(i).lotag == spriteLotag){
				sectorId = map.getSprite(i).sectnum;
			}
		}

		map.deleteSector(sectorId);
		return map;
	}

	public static void writeAndOpenMapPng(Map map) throws IOException {
		File picfile = writeMapPng("outptu.png", map);
		System.out.println("writing " + picfile.toString());
		Desktop.getDesktop().open(picfile);  // note: Runtime.exec() does not work
	}

	public static File writeMapPng(String filename, Map map) throws IOException {
		BufferedImage image = MapImageWriter.toImage(map);
		File outputfile = new File(filename);
		ImageIO.write(image, "png", outputfile);
		return outputfile;
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
	    if(map == null) throw new IllegalArgumentException("map is null");

		String filename = "output.map";
		String resultsFile = System.getProperty("user.dir") + File.separator + "dukeoutput" + File.separator + filename;
		Main.writeResult(map, resultsFile);
		
		//TODO:  this should go in some conf/ini/json file that is not checked in
		
		String copyDest = "C:/Users/Dave/Dropbox/workspace/dosdrive/duke3d/" + filename;
		FileUtils.copyFile(new File(resultsFile), new File(copyDest));
		System.out.println("map generated: " + filename);
		
		//TODO:  can we put build times in the map somewhere?
	}

}
