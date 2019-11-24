package trn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.List;

import org.junit.Assert;
import org.junit.Test;

public class MapTests {

	@Test
	public void testAddLoop(){
		Map map = Map.createNew();
		
		
		Wall w0 = new Wall(0,0);
		Wall w1 = new Wall(1,1);
		Wall w2 = new Wall(2,2);
		
		map.addLoop(w0, w1, w2);
		
		Assert.assertEquals(3, map.getWallCount());
		
		Assert.assertEquals(1, map.getWall(0).getPoint2Id());
		Assert.assertEquals(2, map.getWall(1).getPoint2Id());
		Assert.assertEquals(0, map.getWall(2).getPoint2Id());
		
		//
		// add another set
		//
		
		Wall w3 = new Wall(10,100);
		Wall w4 = new Wall(10,120);
		Wall w5 = new Wall(20,100);
		
		map.addLoop(w3, w4, w5);
		
		Assert.assertEquals(6, map.getWallCount());
		
		Assert.assertEquals(1, map.getWall(0).getPoint2Id());
		Assert.assertEquals(2, map.getWall(1).getPoint2Id());
		Assert.assertEquals(0, map.getWall(2).getPoint2Id());
		
		Assert.assertEquals(4, map.getWall(3).getPoint2Id());
		Assert.assertEquals(5, map.getWall(4).getPoint2Id());
		Assert.assertEquals(3, map.getWall(5).getPoint2Id());
		
	}
	
	@Test
	public void testGetSectorWallIndexes(){
	
		Map map = Map.createNew();
		
		
		Wall w0 = new Wall(0,0);
		Wall w1 = new Wall(1,1);
		Wall w2 = new Wall(2,2);
		
		map.createSectorFromLoop(w0, w1, w2);
		
		List<Integer> list = map.getSectorWallIndexes(0);
		
		Assert.assertEquals(3, list.size());
		
		Assert.assertEquals(0, (int)list.get(0));
		Assert.assertEquals(1, (int)list.get(1));
		Assert.assertEquals(2, (int)list.get(2));
	}

	private static int getSectorWithSprite(Map map, int spriteLotag){
		for(int i = 0; i < map.spriteCount; ++i){
			if(map.getSprite(i).lotag == spriteLotag){
				return map.getSprite(i).sectnum;
			}
		}
		return -1;
	}

	private Map checkSerialization(Map map) throws Exception {
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		map.toBytes(output);
		Map result = Map.readMap(output.toByteArray());
		result.assertIntegrity();
		return result;
	}

	@Test
	public void testDeleteSector() throws Exception {

		for(int lotagToDelete = 1; lotagToDelete <= 5; lotagToDelete++){

			Map map = JavaTestUtils.readTestMap("ds.map");
			map.assertIntegrity();
			int startingSectorCount = map.sectorCount;

			int sectorId = getSectorWithSprite(map, lotagToDelete);
			Assert.assertTrue(sectorId > -1);
			map.deleteSector(sectorId);
			Assert.assertEquals(-1, getSectorWithSprite(map, lotagToDelete));
			map.assertIntegrity();

			Assert.assertEquals(startingSectorCount - 1, checkSerialization(map).sectorCount);
		}
	}
}
