package trn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

import org.junit.Assert;
import org.junit.Test;
import trn.prefab.PrefabUtils;

public class MapTests {

	private PointXY p(int x, int y){
		return new PointXY(x, y);
	}

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


	private long sumOfCrossProduct(Collection<WallView> wallLoop){
		List<WallView> list = new ArrayList<>(wallLoop);
		return MapUtil.sumOfCrossProduct(list);
	}

	@Test
	public void testGetAllWallLoops() throws Exception {
		Map map = JavaTestUtils.readTestMap(JavaTestUtils.JUNIT1);

		HashMap<Integer, Integer> testSectors = new HashMap<>();
		for(Sprite s: map.sprites){
			if(s.getTexture() == 49 || s.getTexture() == 37){
				// shotgun texture
			}else{
				Assert.assertEquals(s.getTexture(), PrefabUtils.MARKER_SPRITE_TEX);
				Assert.assertEquals(s.getLotag(), PrefabUtils.MarkerSpriteLoTags.GROUP_ID);
				testSectors.put((int)s.getHiTag(), (int)s.getSectorId());
			}
		}

		// sector 1, one wall loop
        int sectorId = testSectors.get(1);
		Assert.assertEquals(1, map.getAllWallLoops(sectorId).size());
		Assert.assertEquals(1, map.getAllWallLoopsAsViews(sectorId).size());
		Assert.assertTrue(0 < sumOfCrossProduct(map.getAllWallLoopsAsViews(sectorId).get(0)));
		Assert.assertTrue(MapUtil.isOuterWallLoop(map.getAllWallLoopsAsViews(sectorId).get(0)));

		// sector 2:  the outer loop as 5 walls; the inner has 4
		sectorId = testSectors.get(2);
		Assert.assertEquals(2, map.getAllWallLoops(sectorId).size());
		List<Collection<WallView>> allLoops = map.getAllWallLoopsAsViews(sectorId);
		for(Collection<WallView> wallLoop : allLoops){
		    if(wallLoop.size() == 4){
		        Assert.assertTrue(0 > MapUtil.sumOfCrossProduct(wallLoop));
		        Assert.assertFalse(MapUtil.isOuterWallLoop(wallLoop));
			}else if(wallLoop.size() == 5){
				Assert.assertTrue(0 < MapUtil.sumOfCrossProduct(wallLoop));
				Assert.assertTrue(MapUtil.isOuterWallLoop(wallLoop));
			}else{
		    	Assert.fail();
			}
		}

		sectorId = testSectors.get(3);
		Assert.assertEquals(4, map.getAllWallLoops(sectorId).size());

		// This only has 4, because the bottom wall of the peninsula belongs to the
		// other sector entirely.
		sectorId = testSectors.get(4);
		Assert.assertEquals(4, map.getAllWallLoops(sectorId).size());
		Assert.assertEquals(4, map.getAllWallLoopsAsViews(sectorId).size());


	}

	@Test
	public void testDeterminant() {
		Assert.assertEquals(0, Map.determinant(new PointXY(0, 0), new PointXY(7, 7)));
		Assert.assertEquals(-7, Map.determinant(new PointXY(0, 1), new PointXY(7, 7)));
		Assert.assertEquals(-14, Map.determinant(new PointXY(0, 2), new PointXY(7, 7)));
		Assert.assertEquals(-21, Map.determinant(new PointXY(0, 3), new PointXY(7, 7)));
		Assert.assertEquals(21, Map.determinant(new PointXY(0, 3), new PointXY(-7, 7)));
		Assert.assertEquals(-24, Map.determinant(new PointXY(0, 3), new PointXY(8, 7)));
		Assert.assertEquals(-32, Map.determinant(new PointXY(0, 4), new PointXY(8, 6)));
		Assert.assertEquals(-26, Map.determinant(new PointXY(1, 4), new PointXY(8, 6)));
		Assert.assertEquals(-38, Map.determinant(new PointXY(1, 4), new PointXY(8, -6)));
		Assert.assertEquals(-20, Map.determinant(new PointXY(2, 4), new PointXY(8, 6)));
		Assert.assertEquals(-14, Map.determinant(new PointXY(3, 4), new PointXY(8, 6)));
		Assert.assertEquals(18, Map.determinant(new PointXY(3, 4), new PointXY(0, 6)));
	}

	@Test
	public void testIsClockwise() {
		// build coordinates are:
		//
        //         y-
		//         /\
		//          |
		//   x- <--- ---> x+
		//          |
		//         \/
		//         y+
		Assert.assertTrue(Map.isClockwise(p(-5, -5), p(5, -5), p(5, 5), p(-5, 5)));
		Assert.assertFalse(Map.isClockwise(p(-5, -5), p(-5, 5), p(5, 5), p(5, -5)));
	}

	private List<Wall> createInnerBox(PointXY topLeft, int tex){
	    List<Wall> results = new ArrayList<>(4);
		results.add(new Wall(topLeft.add(p(0, 1024)), tex, 8, 8));
		results.add(new Wall(topLeft.add(p(1024, 1024)), tex, 8, 8));
		results.add(new Wall(topLeft.add(p(1024, 0)), tex, 8, 8));
		results.add(new Wall(topLeft, tex, 8, 8));
		return results;
	}

	private PointXY topLeft(Map map, int sectorId){
		int minX = Map.MAX_X;
		int minY = Map.MAX_Y;
		for(int w : map.getAllSectorWallIds(map.getSector(sectorId))){
		    Wall wall = map.getWall(w);
		    minX = Math.min(minX, wall.x);
			minY = Math.min(minY, wall.y);
		}
		return p(minX, minY);
	}

	@Test
	public void testAddLoopToSector() throws Exception {
		Map map = JavaTestUtils.readTestMap(JavaTestUtils.ADD_LOOP);
		//Map map = JavaTestUtils.readMap(HardcodedConfig.getDosboxPath("ADDLOOP.MAP"));

		int roomATex = 395;
		int roomBTex = 461;
		int roomCTex = 3387;
		List<Integer> sectorTextures = new ArrayList<>(3);
		sectorTextures.add(roomATex);
		sectorTextures.add(roomBTex);
		sectorTextures.add(roomCTex);

		int sectorIdA = -1;
		int sectorIdB = -1;
		int sectorIdC = -1;

		Assert.assertEquals(3, map.getSectorCount());
		for(int i = 0; i < map.getSectorCount(); ++i){
			Sector sector = map.getSector(i);
			if(sector.getFloorTexture() == roomATex){
				sectorIdA = i;
			}else if(sector.getFloorTexture() == roomBTex){
				sectorIdB = i;
			}else if(sector.getFloorTexture() == roomCTex){
				sectorIdC = i;
			}else{
				Assert.fail();
			}
		}

		List<Integer> sectorIds = new ArrayList<>(3);
		sectorIds.add(sectorIdA);
		sectorIds.add(sectorIdB);
		sectorIds.add(sectorIdC);

		for(int sector = 0; sector < 3; ++sector){
			int sectorId = sectorIds.get(sector);

			for(int w : map.getAllSectorWallIds(map.getSector(sectorId))){
				Wall wall = map.getWall(w);
				Assert.assertEquals((int)sectorTextures.get(sector), (int)wall.getTexture());
			}

			List<Collection<Integer>> wallLoops = map.getAllWallLoops(sectorId);
			for(Collection<Integer> wallLoop: wallLoops){
				List<PointXY> polygon = new ArrayList<PointXY>();
				for(int wallId: wallLoop){
					polygon.add(map.getWall(wallId).getLocation());
				}
				Assert.assertTrue(Map.isClockwise(polygon.toArray(new PointXY[]{})));
			}
		}

		PointXY columnA = topLeft(map, sectorIdA).add(p(1024, 1024));
		List<Wall> columnWallsA = createInnerBox(columnA, roomATex);

		Assert.assertEquals(12, map.getWallCount());
		map.addLoopToSector(sectorIdA, columnWallsA);
		Assert.assertEquals(16, map.getWallCount());

		PointXY columnB = topLeft(map, sectorIdB).add(p(1024, 1024));
		map.addLoopToSector(sectorIdB, createInnerBox(columnB, roomBTex));

		PointXY columnC = topLeft(map, sectorIdC).add(p(1024, 1024));
		map.addLoopToSector(sectorIdC, createInnerBox(columnC, roomCTex));

		map.assertIntegrity();
		// Main.deployTest(map);
	}
}
