package trn.duke.experiments.gridblock;

import trn.MapUtil;
import trn.maze.Heading;

/**
 * Primitive early version of redwall connectors.  I don't think the male/female distinction brought any value,
 * since redwall connections don't have a direction.
 *
 */
public class Connector {
	
	public static boolean MALE = true;
	public static boolean FEMALE = false;

	public static Heading genderToHeading(int connectorType, boolean gender){
		if(connectorType == NORTH_SOUTH){
			return gender ? Heading.NORTH : Heading.SOUTH;
		}else if(connectorType == EAST_WEST){
			return gender ? Heading.EAST : Heading.WEST;
		}else{
			throw new IllegalArgumentException();
		}
	}
	
	
	/**
	 * connector that glues the north edge of a grid-based block to the south edge of another
	 * north is male, south is female
	 */
	public static final int NORTH_SOUTH = 1;
	
	/**
	 * connector that glues the east edge of a grid-based block to the west edge of another
	 * east is male, west is female
	 */
	public static final int EAST_WEST = 2;
	
	
	private final int connectorType;
	
	/**
	 * the block this connector belongs to.
	 */
	protected final Block parentBlock;
	
	/**
	 * a binary connection is made from one male connector and one female connector
	 *
	 * ... gender+connectorType is a stupid way of saying which side of the block its on
	 */
	private final boolean gender;

	private final Heading heading;

	private int sectorIndex = -1;
	private Integer floorZ;

	public Connector(int connectorType, Block parentBlock, boolean gender){
		if(parentBlock == null) throw new IllegalArgumentException();
		this.connectorType = connectorType;
		this.parentBlock = parentBlock;
		this.gender = gender;
		this.heading = genderToHeading(connectorType, gender);
	}

	public final void setSectorIndex(int index) {
		this.sectorIndex = index;
	}
	public final void setFloorZ(Integer floorZ){
		this.floorZ = floorZ;
	}

	public final Integer getFloorZ(){
		return this.floorZ;
	}

	public final int getConnectorType(){
		return this.connectorType;
	}
	
	public final boolean getGender(){
		return this.gender;
	}

	public void draw(trn.Map map, Block otherBlock){
		if(this.parentBlock == otherBlock || otherBlock == null) throw new IllegalArgumentException();
		
		Connector other = null;
		if(this.gender == Connector.MALE){
			other = otherBlock.getConnector(genderToHeading(connectorType, FEMALE));
		}else{
			other = otherBlock.getConnector(genderToHeading(connectorType, MALE));
		}
		this.draw(map, other);
	}

	protected final void draw(trn.Map map, Connector other){
		MapUtil.autoLinkWalls(map, this.getCreatedSectorIndex(), other.getCreatedSectorIndex());
	}

	public final int getCreatedSectorIndex(){
		if(sectorIndex == -1) throw new RuntimeException("sector index not set on connector");
		return sectorIndex;
	}
	public static Connector eastEdge(Block block){
		return new Connector(Connector.EAST_WEST, block, Connector.MALE);
	}

	public static Connector westEdge(Block block){
		return new Connector(Connector.EAST_WEST, block, Connector.FEMALE);
	}

	public static Connector northEdge(Block block){
		return new Connector(Connector.NORTH_SOUTH, block, Connector.MALE);
	}

	public static Connector southEdge(Block block){
		return new Connector(Connector.NORTH_SOUTH, block, Connector.FEMALE);
	}


}
