package trn.duke.experiments.gridblock;

import org.apache.commons.lang3.tuple.Pair;

import trn.Sector;
import trn.duke.experiments.SectorPrefab;
import trn.Wall;
import trn.duke.experiments.WallPrefab;
import trn.maze.Heading;

/**
 * a simple block only gets one wall tex, one floor, and one ceiling
 * @author Dave
 *
 */
public class SimpleBlock extends AbstractBlock implements Block {
	
	public static final int WALL_LENGTH = 2048; //2 x largest grid size
	
	//connectors, indexed by heading
	protected final Connector[] connectors = new Connector[]{null, null, null, null};
	
	//edge walls (red walls that touch the connectors) indexed by heading
	// private final int[] walls = new int[]{-1,-1,-1,-1};
	
	private int wallTex = 0;
	private int ceilTex = 0;
	private int floorTex = 0;
	
	private Integer floorZ = null;
	
	private WallPrefab wallPrefab = null;
	
	private SectorPrefab sectorPrefab = null;
	
	/**
	 * index of the sector that was created by this block.
	 * Note: in the future multiple sectors will be created by this block ...
	 */
	private int createdSectorIndex = -1;
	

	public SimpleBlock(Pair<Integer, Integer> gridCoordinate){
		super(gridCoordinate);
		
		this.setConnector(Heading.NORTH, new NorthSouthConnector(this, Connector.MALE));
		this.setConnector(Heading.SOUTH, new NorthSouthConnector(this, Connector.FEMALE));
		
		this.setConnector(Heading.EAST, new EastWestConnector(this, Connector.MALE));
		this.setConnector(Heading.WEST, new EastWestConnector(this, Connector.FEMALE));
	}
	
	public void setWallPrefab(WallPrefab w){
		this.wallPrefab = w;
	}
	
	public void setSectorPrefab(SectorPrefab s){
		this.sectorPrefab = s;
	}
	
	public Integer getFloorZ(){
		return this.floorZ;
	}
	

	public int getCreatedSectorIndex(){
		return this.createdSectorIndex;
	}
	
	public void setFloorTex(int i){
		this.floorTex = i;
	}
	public void setCeilTex(int i){
		this.ceilTex = i;
	}
	public void setWallTex(int i){
		this.wallTex = i;
	}
	
	public void setFloorZ(Integer z){
		this.floorZ = z;
		for(Connector c : connectors){
			c.setFloorZ(floorZ);
		}
		
	}
	
	public int draw(trn.Map map){
		
		int west = gridCoordinate.getLeft() * WALL_LENGTH;
		int east = (gridCoordinate.getLeft() + 1) * WALL_LENGTH;
		int north = gridCoordinate.getRight() * WALL_LENGTH;
		int south = (gridCoordinate.getRight() + 1) * WALL_LENGTH;
		
		Wall nw = new Wall(west, north, wallTex, 16, 8); //first wall; also matches the grid coordinate
		Wall ne = new Wall(east, north, wallTex, 16, 8);
		Wall se = new Wall(east, south, wallTex, 16, 8);
		Wall sw = new Wall(west, south, wallTex, 16, 8);
		
		if(this.wallPrefab != null){
			wallPrefab.writeTo(nw);
			wallPrefab.writeTo(ne);
			wallPrefab.writeTo(se);
			wallPrefab.writeTo(sw);
		}

		int sectorIndex =  map.createSectorFromLoop(nw, ne, se, sw);
		Sector s = map.getSector(sectorIndex);
		s.setFloorTexture(this.floorTex);
		s.setCeilingTexture(this.ceilTex);
		
		if(this.floorZ != null){
			s.setFloorZ(this.floorZ);
		}
		
		if(this.sectorPrefab != null){
			this.sectorPrefab.writeTo(s);
		}
		

		createdSectorIndex = sectorIndex;
		
		for(Connector c : connectors){
			c.setSectorIndex(createdSectorIndex);
		}
		
		return sectorIndex;
	}

	@Override
	public Connector getConnector(Heading heading) {
		return connectors[heading.arrayIndex];
	}
	
	void setConnector(Heading heading, Connector c){
		if(heading == null) throw new IllegalArgumentException();
		connectors[heading.arrayIndex] = c;
	}

}
