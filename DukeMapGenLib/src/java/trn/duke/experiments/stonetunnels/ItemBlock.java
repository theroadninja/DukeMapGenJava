package trn.duke.experiments.stonetunnels;

import java.util.Arrays;

import org.apache.commons.lang3.ArrayUtils;
import org.apache.commons.lang3.tuple.Pair;

import trn.MapUtil;
import trn.PointXY;
import trn.Sector;
import trn.Sprite;
import trn.SpritePrefab;
import trn.Wall;
import trn.WallPrefab;
import trn.duke.experiments.gridblock.OrdinalConnector;
import trn.duke.experiments.gridblock.SimpleBlock;

/**
 * Has an item on a dais for the player to pick up.
 * 
 * @author Dave
 *
 */
public class ItemBlock extends SimpleBlock {

	private final SpritePrefab itemPrefab;
	
	public ItemBlock(Pair<Integer, Integer> gridCoordinate, SpritePrefab item) {
		super(gridCoordinate);
		this.itemPrefab = item;
	}
	
	@Override
	public int draw(trn.Map map){
		
		int south = getSouthEdge();
		int west = getWestEdge();
		int north = getNorthEdge();
		int east = getEastEdge();
		
		/** width between dais and wall */
		final int edgeWidth = 256;
		
		int inner_south = south - edgeWidth;
		int inner_north = north + edgeWidth;
		int inner_west = west + edgeWidth;
		int inner_east = east - edgeWidth;
		
		final int daisZ = StoneConstants.UPPER_FLOORZ - (64 << 4);
		
		
		PointXY[] outer_box = new PointXY[]{
				new PointXY(west, south),
				new PointXY(west, north),
				new PointXY(east, north),
				new PointXY(east, south)
			};
		
		PointXY[] inner_box_cw = new PointXY[]{
				new PointXY(inner_west, inner_south),
				new PointXY(inner_west, inner_north),
				new PointXY(inner_east, inner_north),
				new PointXY(inner_east, inner_south),
		};
		
		PointXY[] inner_box_ccw = Arrays.copyOf(inner_box_cw, inner_box_cw.length);
		ArrayUtils.reverse(inner_box_ccw);
		
		
		
		
		
		
		WallPrefab daisWall = new WallPrefab(StoneConstants.UPPER_WALL);
		daisWall.setTexture(StoneConstants.UPPER_CEILING);
		
		//outer sector
		int outerSectorIndex = map.createSectorFromMultipleLoops(
				Wall.createLoop(inner_box_ccw, daisWall), //this needs to be first b/c of linkAllWalls
				Wall.createLoop(outer_box, StoneConstants.UPPER_WALL)
				
				);
		StoneConstants.UPPER_SECTOR.writeTo(map.getSector(outerSectorIndex));
		
		int innerSectorIndex = map.createSectorFromLoop(
				Wall.createLoop(inner_box_cw, StoneConstants.UPPER_WALL));
		Sector innerSector = map.getSector(innerSectorIndex);
		StoneConstants.UPPER_SECTOR.writeTo(innerSector);
		innerSector.setFloorTexture(StoneConstants.UPPER_CEILING);
		innerSector.setFloorZ(daisZ);
		
		MapUtil.linkAllWalls(map, outerSectorIndex, map.getSector(outerSectorIndex).getFirstWall(), 
				innerSectorIndex, innerSector.getFirstWall());
		
		
		if(this.itemPrefab != null){
			Sprite item = new Sprite(getCenter(), daisZ, innerSectorIndex);
			itemPrefab.writeTo(item);
			map.addSprite(item);
		}
		
		
		
		
		
		
		
		for(OrdinalConnector c : connectors){
			c.setSectorIndex(outerSectorIndex);
		}
		
		
		return -1;
	}

}
