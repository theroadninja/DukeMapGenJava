package trn.duke.experiments.gridblock;

import trn.Map;
import trn.maze.Heading;

public abstract class OrdinalConnector extends Connector {

	private int sectorIndex;
	
	private Integer floorZ;
	
	public OrdinalConnector(int connectorType, Block parentBlock, boolean gender) {
		super(connectorType, parentBlock, gender);
	}
	
	public final void setFloorZ(Integer floorZ){
		this.floorZ = floorZ;
	}
	
	public final Integer getFloorZ(){
		return this.floorZ;
	}

	@Override
	public final void setSectorIndex(int index) {
		this.sectorIndex = index;
	}
	
	public final int getCreatedSectorIndex(){
		return sectorIndex;
	}
	


}
