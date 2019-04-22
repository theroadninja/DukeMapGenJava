package trn.duke.experiments;

import java.io.IOException;

import org.apache.commons.lang3.tuple.ImmutablePair;
import org.apache.commons.lang3.tuple.Pair;

import trn.Main;
import trn.Map;
import trn.PlayerStart;
import trn.SpritePrefab;
import trn.duke.TextureList;
import trn.duke.experiments.gridblock.BlockCursor;
import trn.duke.experiments.gridblock.Grid;
import trn.duke.experiments.stonetunnels.BlockRotation;
import trn.duke.experiments.stonetunnels.ExitBlock;
import trn.duke.experiments.stonetunnels.ItemBlock;
import trn.duke.experiments.stonetunnels.NarrowPassageBlock;
import trn.duke.experiments.stonetunnels.PassageBlock;
import trn.duke.experiments.stonetunnels.StartBlock;
import trn.maze.Heading;

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
		BlockCursor cursor = new BlockCursor(0,1);
		StartBlock start = new StartBlock(cursor.get()); //0,1
		Grid grid = new Grid();
		grid.add(start);
		grid.add(new NarrowPassageBlock(cursor.moveNorth(), BlockRotation.VERTICAL));
		
		//this blows up, but not very cleanly (should the grid check?)
		//grid.add(new PassageBlock(new ImmutablePair<Integer, Integer>(-1, 0)));
		grid.add(new PassageBlock(cursor.moveNorth())); // 0, -1
		grid.add(new PassageBlock(cursor.moveEast())); // 1, -1
		
		//grid.add(new ItemBlock(new ImmutablePair<Integer, Integer>(2, -1), new SpritePrefab(TextureList.Items.CARD)));
		grid.add(new ItemBlock(new ImmutablePair<Integer, Integer>(2, -1), new SpritePrefab(TextureList.Items.ARMOR)));
		grid.add(new PassageBlock(new ImmutablePair<Integer, Integer>(3, -1)));
		grid.add(new PassageBlock(new ImmutablePair<Integer, Integer>(4, -1)));
		grid.add(new PassageBlock(new ImmutablePair<Integer, Integer>(5, -1)));

		grid.add(new PassageBlock(new ImmutablePair<Integer, Integer>(5, 0)));
		
		//grid.add(new PassageBlock(new ImmutablePair<Integer, Integer>(5, 1)));
		grid.add(new ItemBlock(new ImmutablePair<Integer, Integer>(5,1), new SpritePrefab(TextureList.Enemies.LIZTROOP)));
		
		grid.add(new PassageBlock(new ImmutablePair<Integer, Integer>(5, 2)));
		grid.add(new ExitBlock(new ImmutablePair<Integer, Integer>(5, 3)));
		
		//TODO: might be night to have a cursor...
		
		Map map = createMap(grid, start.getPlayerStart());
		
		Main.deployTest(map);
	}
	
	
	public static trn.Map createMap(Grid grid, PlayerStart ps){
		
		Map map = Map.createNew();
		
		map.setPlayerStart(ps);
		
		//create the sectors
		for(Pair<Integer, Integer> p : grid.getNodes()){
			//System.out.println(p);
			grid.getBlock(p).draw(map);
			//int sectorIndex = E7BetterBlocks.createSector(map, p, grid.getBlock(p));
		}
		
		//link the sectors
		for(Pair<Integer, Integer> p : grid.getNodes()){
			Pair<Integer, Integer> east = Heading.EAST.move(p);
			Pair<Integer, Integer> south = Heading.SOUTH.move(p);
			if(grid.contains(east)){
				grid.getBlock(p).getConnector(Heading.EAST).draw(map,  grid.getBlock(east));
			}
			if(grid.contains(south)){
				grid.getBlock(p).getConnector(Heading.SOUTH).draw(map, grid.getBlock(south));
			}
		}
		
		return map;
	}
	
	
	
	
	
	
}
