package trn.duke.experiments;

import java.io.IOException;

import org.apache.commons.lang3.tuple.ImmutablePair;

import trn.Main;
import trn.Map;
import trn.duke.experiments.stonetunnels.StartBlock;

/**
 * Still block based like E7, but now trying to improve maze generation towards
 * playability, i.e. no longer simple DFS.
 * 
 * 
 * @author Dave
 *
 */
public class E8StoneTunnels {

	public static void main(String[] args) throws IOException{
		
		//Grid grid = new Grid();
		
		Map map = Map.createNew();
		
		//StartBlockSimple start = new StartBlockSimple(new ImmutablePair<Integer, Integer>(0,0));
		StartBlock start = new StartBlock(new ImmutablePair<Integer, Integer>(0,0));
		
		map.setPlayerStart(start.getPlayerStart());
		start.draw(map);
		
		Main.writeResult(map);
		
		
	}
}
