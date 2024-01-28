package trn.duke.experiments.gridblock;

import trn.MapUtil;
import trn.maze.Heading;

/**
 * Primitive early version of redwall connectors.  I don't think the male/female distinction brought any value,
 * since redwall connections don't have a direction.
 *
 */
public abstract class Connector {
	
	public static boolean MALE = true;
	public static boolean FEMALE = false;
	
	
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
	 */
	private final boolean gender;

	private final Heading heading;

	private int sectorIndex = -1;
	private Integer floorZ;

	public Connector(int connectorType, Block parentBlock, boolean gender, Heading heading){
		if(parentBlock == null) throw new IllegalArgumentException();
		this.connectorType = connectorType;
		this.parentBlock = parentBlock;
		this.gender = gender;
		this.heading = heading;
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
			// other = otherBlock.getConnector(genderToBlockEdgeHeading(FEMALE));
			other = otherBlock.getConnector(genderToBlockEdgeHeading(FEMALE));
		}else{
			// other = otherBlock.getConnector(genderToBlockEdgeHeading(MALE));
			other = otherBlock.getConnector(genderToBlockEdgeHeading(MALE));
		}
		this.draw(map, other);
	}

	protected abstract Heading genderToBlockEdgeHeading(boolean gender);
	
	protected final void draw(trn.Map map, Connector other){
		MapUtil.autoLinkWalls(map, this.getCreatedSectorIndex(), other.getCreatedSectorIndex());
	}

	public final int getCreatedSectorIndex(){
		if(sectorIndex == -1) throw new RuntimeException("sector index not set on connector");
		return sectorIndex;
	}

}
