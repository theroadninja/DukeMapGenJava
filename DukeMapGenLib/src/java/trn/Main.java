package trn;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;

import org.apache.commons.io.FileUtils;

import trn.duke.MapImageWriter;
import trn.duke.experiments.E1RandomSprites;
import trn.prefab.experiments.Hypercube1;
import trn.prefab.experiments.Hypercube2;

import javax.imageio.ImageIO;

public class Main {

	
	//found this, looks like a good resource for the build map format:
	// http://www.shikadi.net/moddingwiki/MAP_Format_%28Build%29
	
	public static String DOSPATH = "C:/Users/Dave/Dropbox/workspace/dosdrive/duke3d/";
	
	public static void main(String[] args) throws Exception {
		

		
		//Map fromMap = loadMap(DOSPATH + "cptest2.map");
		
		// cptest4.map - space joins
		
		// cptest3.map
		//  group 10 is the plus sign hallway intersection
		//		connector on left:  connector id 123 and wall lotag 2
		//		connector on right: connector id 456 and wall lotag 1
		//

		File f = new File(DOSPATH);
		if(!(f.exists() && f.isDirectory())){
			throw new Exception(DOSPATH + " does not exist");
		}


		// Map fromMap = loadMap(DOSPATH + "cptest3.map");
		// //Map outMap = PrefabExperiment.copytest3(fromMap);
		// //Map outMap = PrefabExperiment.copytest4(fromMap);
		// Map outMap = GridExperiment.run(fromMap);



		//Map outMap = Hypercube1.run(loadMap(DOSPATH + "hyper1.map"));
		Map outMap = Hypercube2.run(MapLoader.loadMap(DOSPATH + "hyper2.map"));

		// writeAndOpenMapPng(outMap);
		deployTest(outMap);

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
	

	/*
	public static void oldmain(String[] args) throws Exception {
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
	*/
}
