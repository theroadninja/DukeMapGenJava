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


/**
 * Main entry point for map compiler.
 *
 * For a reference to my sprite logic @see trn.prefab.PrefabUtils
 */
public class Main {

	//found this, looks like a good resource for the build map format:
	// http://www.shikadi.net/moddingwiki/MAP_Format_%28Build%29
	
	public static String DOSPATH = "C:/Users/Dave/Dropbox/workspace/dosdrive/duke3d/";
	
	public static void main(String[] args) throws Exception {

		File f = new File(DOSPATH);
		if(!(f.exists() && f.isDirectory())){
			throw new Exception(DOSPATH + " does not exist");
		}

		//Map outMap = ChildSectorTest.run(MapLoader.loadMap(DOSPATH + ChildSectorTest.FILENAME()));
		//run(FirstPrefabExperiment$.MODULE$);

		// run(GridExperiment$.MODULE$);
		run(new SquareTileMain(SquareTileMain.TestFile1(), GridBuilderInput.apply(8, 8)));

		//Map outMap = Hypercube1.run(MapLoader.loadMap(DOSPATH + "hyper1.map"));
		//Map outMap = Hypercube2.run(MapLoader.loadMap(DOSPATH + "hyper2.map"));

		//run(SoundListMap$.MODULE$);
		//run(PipeDream$.MODULE$);
		//run(PoolExperiment$.MODULE$);
		//run(Sushi$.MODULE$);
		//= Sushi.run(new MapLoader(DOSPATH));

		// writeAndOpenMapPng(outMap);
		//deployTest(outMap);

		//run(Hypercube3$.MODULE$);
		//run(Hypercube4$.MODULE$);
		//run(JigsawPlacerMain$.MODULE$);

		// run(PersonalStorage$.MODULE$);
	}


	private static void run(PrefabExperiment exp) throws IOException {
		Map outMap = exp.run(new MapLoader(DOSPATH));
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
