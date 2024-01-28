package trn.duke.experiments.gridblock;

import trn.Map;
import trn.MapUtil;
import trn.maze.Heading;

public class EastWestConnector extends Connector {

	public EastWestConnector(Block parentBlock, boolean gender) {
		super(
				Connector.EAST_WEST,
				parentBlock,
				gender,
				gender ? Heading.EAST : Heading.WEST
				);
	}

	@Override
	protected Heading genderToBlockEdgeHeading(boolean gender) {
		// east edge of block is male, west is female
		
		return gender ? Heading.EAST : Heading.WEST; 
	}

	public static EastWestConnector eastEdge(Block block){
		return new EastWestConnector(block, Connector.MALE);
	}
	
	public static EastWestConnector westEdge(Block block){
		return new EastWestConnector(block, Connector.FEMALE);
	}


}
