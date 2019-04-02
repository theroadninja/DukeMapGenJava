package trn.duke.experiments;

import java.io.File;

import trn.Main;
import trn.Map;
import trn.MapLoader;

/**
 * just prints the map
 * 
 * @author Dave
 *
 */
public class E0PrintMap {

	public static void main(String[] args) throws Exception {
		
		Map m = MapLoader.loadMap("RT0.MAP");
		//Map m = Main.loadMap(System.getProperty("user.dir") + File.separator + "dukeoutput" + File.separator, "output.map");
		
		//Map m = Main.loadMap("gridsize.MAP");
		
		m.printAll();
		
	}
}
