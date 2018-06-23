package trn.duke.experiments.gridblock;

import trn.Map;
import trn.maze.Heading;

public class EastWestConnector extends OrdinalConnector {

	public EastWestConnector(Block parentBlock, boolean gender) {
		super(Connector.EAST_WEST, parentBlock, gender);
	}

	@Override
	protected Heading genderToBlockEdgeHeading(boolean gender) {
		// east edge of block is male, west is female
		
		return gender ? Heading.EAST : Heading.WEST; 
	}

	@Override
	protected void draw(Map map, Connector femaleConnector) {
		
		EastWestConnector eastEdgeOfWestBlock = this;
		EastWestConnector westEdgeOfEastBlock = (EastWestConnector)femaleConnector;
		
		GridUtils.linkSectors(map, westEdgeOfEastBlock.getCreatedSectorIndex(), eastEdgeOfWestBlock.getCreatedSectorIndex());
		
	}

	
	public static EastWestConnector eastEdge(Block block){
		return new EastWestConnector(block, Connector.MALE);
	}
	
	public static EastWestConnector westEdge(Block block){
		return new EastWestConnector(block, Connector.FEMALE);
	}


}
