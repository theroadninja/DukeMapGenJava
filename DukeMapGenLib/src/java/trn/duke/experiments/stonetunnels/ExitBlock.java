package trn.duke.experiments.stonetunnels;

import org.apache.commons.lang3.tuple.Pair;

import trn.*;
import trn.duke.TextureList;
import trn.duke.experiments.gridblock.AbstractBlock;
import trn.duke.experiments.gridblock.Block;
import trn.duke.experiments.gridblock.Connector;
import trn.duke.experiments.gridblock.NorthSouthConnector;
import trn.maze.Heading;

/**
 * 
 * @author Dave
 *
 */
public class ExitBlock extends AbstractBlock implements Block {

	// TODO apparently any nonzero value will work, and buildhlp claims it should be 32767....
	public static final int NUKE_BUTTON_END_LEVEL = 65535;

	private Connector connector;
	
	private final Heading connectorEdge = Heading.NORTH;
	
	private final int floorZ = StoneConstants.UPPER_FLOORZ;

	public ExitBlock(Pair<Integer, Integer> gridCoordinate) {
		super(gridCoordinate);

		this.connector = NorthSouthConnector.northEdge(this);
	}

	@Override
	public Connector getConnector(Heading heading) {
		return heading == connectorEdge ? connector : null;
	}

	@Override
	public int draw(Map map) {
		
		int south = getSouthEdge();
		int west = getWestEdge();
		int north = getNorthEdge();
		int east = getEastEdge();
		
		PointXY[] box = new PointXY[]{
				new PointXY(west, south),
				new PointXY(west, north),
				new PointXY(east, north),
				new PointXY(east, south)
			};
		
		int sectorIndex = map.createSectorFromLoop(Wall.createLoop(box, StoneConstants.UPPER_WALL));
		
		StoneConstants.UPPER_SECTOR.writeTo(map.getSector(sectorIndex));
		
		
		Sprite exitSprite = new Sprite(
				(east + west) / 2,
				south,
				256 << 4, //z
				(short)sectorIndex);
		
		exitSprite.setTexture(TextureList.Switches.NUKE_BUTTON);
		exitSprite.setCstat((short)Sprite.CSTAT_FLAGS.PLACED_ON_WALL);
		exitSprite.setXRepeat(21);
		exitSprite.setYRepeat(26);
		exitSprite.setAngle(AngleUtil.ANGLE_UP);
		exitSprite.setLotag(NUKE_BUTTON_END_LEVEL);
		
		map.addSprite(exitSprite);
		
		
		connector.setSectorIndex(sectorIndex);
		
		
		
		return -1;
	}

}
