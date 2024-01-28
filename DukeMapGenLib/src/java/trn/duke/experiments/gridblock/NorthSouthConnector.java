package trn.duke.experiments.gridblock;

import trn.Map;
import trn.MapUtil;
import trn.maze.Heading;

/**
 * north is male, south is female
 * 
 * @author Dave
 *
 */
public class NorthSouthConnector extends Connector {

	public NorthSouthConnector(Block parentBlock, boolean gender) {
		super(
				Connector.NORTH_SOUTH,
				parentBlock,
				gender,
				gender ? Heading.NORTH : Heading.SOUTH
		);
	}


	@Override
	protected Heading genderToBlockEdgeHeading(boolean gender){
		//male connector is north edge of south block
		//female connector is south edge of north block
		return (gender) ? Heading.NORTH : Heading.SOUTH;
	}

	public static NorthSouthConnector northEdge(Block block){
		return new NorthSouthConnector(block, Connector.MALE);
	}
	
	public static NorthSouthConnector southEdge(Block block){
		return new NorthSouthConnector(block, Connector.FEMALE);
	}
	
	
}
