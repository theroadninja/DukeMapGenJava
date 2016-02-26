package trn.duke.experiments;

import trn.Main;
import trn.Map;

/**
 * just prints the map
 * 
 * @author Dave
 *
 */
public class E0PrintMap {

	public static void main(String[] args) throws Exception {
		
		Map m = Main.loadMap("RT0.MAP");
		
		m.printAll();
		
	}
}
