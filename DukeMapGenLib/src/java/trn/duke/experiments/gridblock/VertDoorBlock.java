package trn.duke.experiments.gridblock;

import org.apache.commons.lang3.tuple.Pair;

import trn.DukeConstants;
import trn.Map;
import trn.Sector;
import trn.Sprite;
import trn.Wall;
import trn.duke.TextureList;

/**
 * North-south room that just has a door in the middle
 * 
 * @author Dave
 *
 */
public class VertDoorBlock extends VertBlock implements Block {

	/** its the red one with black trim, looks like a garage door */
	public static final int DAVES_FAV_DOOR = 1173;
	
	public static final int NICE_GRAY_BRICK = 461;
	
	
	
	public static final int DOOR_WIDTH = 64;
	
	
	private Integer floorZ = null;
	
	public VertDoorBlock(Pair<Integer, Integer> gridCoordinate) {
		super(gridCoordinate);
	}
	
	public Integer getFloorZ(){
		return this.floorZ;
	}
	
	public void setFloorZ(Integer z){
		this.floorZ = z;
		super.setFloorZAllConnectors(z);
		
	}

	@Override
	public int draw(Map map) {
		
		int west = getWestEdge();
		int east = getEastEdge();
		int north = getNorthEdge();
		int south = getSouthEdge();
		
		int centery = (south + north) / 2;
		
		//positive y goes down/south
		int doorsouth = centery + DOOR_WIDTH / 2;
		int doornorth = centery - DOOR_WIDTH / 2;
		
		int[] sectorIndexes = new int[3];
		
		final int wallTex = NICE_GRAY_BRICK;
		
		//south
		{
			sectorIndexes[0] = map.createSectorFromLoop(
					new Wall(west, south, wallTex, 16, 8),
					new Wall(west, doorsouth, DAVES_FAV_DOOR, 16, 8),
					new Wall(east, doorsouth, wallTex, 16, 8),
					new Wall(east, south, wallTex, 16, 8)); 
			
			Sector s = map.getSector(sectorIndexes[0]);
		
			s.setFloorTexture(0);
			s.setCeilingTexture(0);
			s.setFloorZ(this.floorZ);
		}
		
		//door
		{
			/*
			 * TODO:

			 * add a wall switch to activate?
			 * 
			 * 
			 */
			
			final int doorInsideTex = 1186;
			
			sectorIndexes[1] = map.createSectorFromLoop(
					new Wall(west, doorsouth, doorInsideTex, 16/8, 8).addCstat(Wall.CSTAT_FLAGS.BIT_2_ALIGN_TEX_ON_BOTTOM), 
					new Wall(west, doornorth, 0, 16, 8),
					new Wall(east, doornorth, doorInsideTex, 16/8, 8).addCstat(Wall.CSTAT_FLAGS.BIT_2_ALIGN_TEX_ON_BOTTOM),
					new Wall(east, doorsouth, 0, 16, 8)); 
			
			Sector s = map.getSector(sectorIndexes[1]);
		
			s.setFloorTexture(0);
			s.setCeilingTexture(0);
			s.setFloorZ(this.floorZ);
			s.setCeilingZ(this.floorZ); //this is what makes it a door
			s.setLotag(DukeConstants.LOTAGS.DOOR); //also this
		}
		
		
		int centerx = (east + west) / 2;
		
		Sprite doorSound = new Sprite(centerx - 128, centery, floorZ, (short)sectorIndexes[1]);
		
		doorSound.setTexture(TextureList.MUSIC_AND_SFX);
		doorSound.setLotag(DukeConstants.SOUNDS.DOOR_OPERATE1); //starting sound
		map.addSprite(doorSound);
		
		
		Sprite doorSpeed = new Sprite(centerx + 128, centery, floorZ, (short)sectorIndexes[1]);
		doorSpeed.setTexture(TextureList.GPSSPEED);
		doorSpeed.setLotag(512); //32 is pretty slow. 512 is pretty fast.  possibly faster than default; not sure.
		map.addSprite(doorSpeed);
		
		
		
		
		
		//north
		{
			sectorIndexes[2] = map.createSectorFromLoop(
					new Wall(west, doornorth, wallTex, 16, 8),
					new Wall(west, north, wallTex, 16, 8),
					new Wall(east, north, wallTex, 16, 8),
					new Wall(east, doornorth, DAVES_FAV_DOOR, 16, 8)); 
			
			Sector s = map.getSector(sectorIndexes[2]);
		
			s.setFloorTexture(0);
			s.setCeilingTexture(0);
			s.setFloorZ(this.floorZ);
		}
		
		GridUtils.linkSectorsNoWrap(map, sectorIndexes);
		
		this.southConnector.setSectorIndex(sectorIndexes[0]);
		this.northConnector.setSectorIndex(sectorIndexes[sectorIndexes.length-1]);

		return sectorIndexes[0];
	}

}
