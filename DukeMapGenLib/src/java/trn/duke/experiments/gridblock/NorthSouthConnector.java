package trn.duke.experiments.gridblock;

import trn.Map;
import trn.maze.Heading;

/**
 * north is male, south is female
 * 
 * @author Dave
 *
 */
public class NorthSouthConnector extends OrdinalConnector {

	public NorthSouthConnector(Block parentBlock, boolean gender) {
		super(Connector.NORTH_SOUTH, parentBlock, gender);
	}

	@Override
	public void draw(Map map, Connector female) {
		
		//male connector is north edge of south block
		//female connector is south edge of north block
		
		NorthSouthConnector southEdgeOfNorthBlock = (NorthSouthConnector)female;
		NorthSouthConnector northEdgeOfSouthBlock = this;
		
		int ni = northEdgeOfSouthBlock.getCreatedSectorIndex();
		if(ni < 0) throw new RuntimeException("invalid north index: " + ni);
		int si = southEdgeOfNorthBlock.getCreatedSectorIndex();
		if(si < 0) throw new RuntimeException("invalid south index:" + si);
		if(si == ni) throw new RuntimeException("connectors are for same sector! si=ni=" + si);
		
		GridUtils.linkSectors(map, northEdgeOfSouthBlock.getCreatedSectorIndex(), southEdgeOfNorthBlock.getCreatedSectorIndex());
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
