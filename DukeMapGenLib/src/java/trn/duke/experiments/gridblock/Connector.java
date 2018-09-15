package trn.duke.experiments.gridblock;

import trn.maze.Heading;

/**
 * Identifies which type of connector a block has.
 * 
 * @author Dave
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
	
	
	public Connector(int connectorType, Block parentBlock, boolean gender){
		if(parentBlock == null) throw new IllegalArgumentException();
		this.connectorType = connectorType;
		this.parentBlock = parentBlock;
		this.gender = gender;
	}
	
	public final int getConnectorType(){
		return this.connectorType;
	}
	
	public final boolean getGender(){
		return this.gender;
	}

	public void draw(trn.Map map, Block otherBlock){
		if(this.parentBlock == otherBlock || otherBlock == null) throw new IllegalArgumentException();
		
		Connector male = null;
		Connector female = null;
		
		if(this.gender == Connector.MALE){
			male = this;
			female = otherBlock.getConnector(genderToBlockEdgeHeading(FEMALE));
		}else{
			male = otherBlock.getConnector(genderToBlockEdgeHeading(MALE));
			female = this;
		}
		
		//safety check
		if(male.gender != MALE || female.gender != FEMALE) throw new RuntimeException();
		
		male.draw(map, female);
		
	}
	
	/**
	 * TODO: there should be a better way to do this...maybe set the wall instead of the sector?
	 * 
	 * @param index
	 */
	public abstract void setSectorIndex(int index);
	
	protected abstract Heading genderToBlockEdgeHeading(boolean gender);
	
	/**
	 * this should only be called by SimpleConnector.
	 * 
	 * Always calls the male connector and passes the female as the argument.
	 * 
	 * @param map
	 * @param femaleConnector 
	 */
	protected abstract void draw(trn.Map map, Connector femaleConnector);
	
}
