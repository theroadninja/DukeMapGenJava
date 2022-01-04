package trn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.*;

import org.junit.Assert;
import org.junit.Test;
import trn.duke.TextureList;
import trn.prefab.PrefabPalette;
import trn.prefab.PrefabUtils;
import trn.prefab.SectorGroup;

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

		Assert.assertEquals(0, map.getSectorIdForWall(0));
		Assert.assertEquals(0, map.getSectorIdForWall(1));
		Assert.assertEquals(0, map.getSectorIdForWall(2));
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
		for(Integer wallId : map.getAllWallLoops(sectorId).get(0)){
			Assert.assertEquals(sectorId, map.getSectorIdForWall(wallId));
		}

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

		    for(WallView wall : wallLoop){
		    	Assert.assertEquals(sectorId, map.getSectorIdForWall(wall.getWallId()));
			}
		}

		sectorId = testSectors.get(3);
		Assert.assertEquals(4, map.getAllWallLoops(sectorId).size());
		for(Collection<WallView> wallLoop : map.getAllWallLoopsAsViews(sectorId)) {
			for (WallView wall : wallLoop) {
				Assert.assertEquals(sectorId, map.getSectorIdForWall(wall.getWallId()));
			}
		}

		// This only has 4, because the bottom wall of the peninsula belongs to the
		// other sector entirely.
		sectorId = testSectors.get(4);
		Assert.assertEquals(4, map.getAllWallLoops(sectorId).size());
		Assert.assertEquals(4, map.getAllWallLoopsAsViews(sectorId).size());
		for(Collection<WallView> wallLoop : map.getAllWallLoopsAsViews(sectorId)) {
			for (WallView wall : wallLoop) {
				Assert.assertEquals(sectorId, map.getSectorIdForWall(wall.getWallId()));
			}
		}


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
		List<Integer> resultsA = map.addLoopToSector(sectorIdA, columnWallsA);
		Assert.assertEquals(16, map.getWallCount());
		Assert.assertEquals(4, resultsA.size());

		List<Wall> wallsA = new ArrayList<>(4);
		List<PointXY> pointsA = new ArrayList<>(4);
		for(int wallId: resultsA){
			wallsA.add(map.getWall(wallId));
			pointsA.add(map.getWall(wallId).getLocation());
		}
		Assert.assertTrue(!Map.isClockwise(pointsA));

		for(int i = 0; i < wallsA.size(); ++i){
			Assert.assertEquals(wallsA.get(i).getLocation(), columnWallsA.get(i).getLocation());
		}




		PointXY columnB = topLeft(map, sectorIdB).add(p(1024, 1024));
		map.addLoopToSector(sectorIdB, createInnerBox(columnB, roomBTex));

		PointXY columnC = topLeft(map, sectorIdC).add(p(1024, 1024));
		map.addLoopToSector(sectorIdC, createInnerBox(columnC, roomCTex));

		map.assertIntegrity();
		// Main.deployTest(map);
	}

	private WallView wall(int wallId, PointXY p1, PointXY p2, int nextWall, int otherSector){
		Wall w = new Wall(p1, TextureList.Other.NICE_GRAY_BRICK, 8, 8);
		w.setOtherSide(nextWall, otherSector);
		WallView wv = new WallView(w, wallId, p1, p2, -1, -1);
		return wv;
	}

	private List<WallView> followWallsForJoin(
			int sectorIdA,
			int sectorIdB,
			WallView startWall,
			List<WallView> walls // NOTE: must only pass walls in the two sectors
	){
		java.util.Map<PointXY, List<WallView>> p1map = new HashMap<>();
		for(WallView w: walls){
			if(!p1map.containsKey(w.p1())){
				p1map.put(w.p1(), new LinkedList<>());
			}
			p1map.get(w.p1()).add(w);
		}
		return Map.followWallsForJoin(sectorIdA, sectorIdB, startWall, p1map);
	}

	@Test
	public void testFollowWallsForJoin1(){
		/*
		 *     A--1---B--5---C
		 *     |      ..     |
		 *     4      28     6
		 *     |      ..     |
		 *     F---3--E--7---D
		 */
		int sectorA = 0;
		int sectorB = 1;

		PointXY a = p(-1024, -1024), b = p(0, -1024), c = p(1024, -1024);
		PointXY f = p(-1024, 0), e = p(0, 0), d = p(1024, 0);

		List<WallView> walls1 = new ArrayList<>();
		walls1.add(wall(1, a, b, -1, -1));
		walls1.add(wall(2, b, e, 8, sectorB));
        walls1.add(wall(3, e, f, -1, -1));
        walls1.add(wall(4, f, a, -1, -1));

        walls1.add(wall(5, b, c, -1, -1));
        walls1.add(wall(6, c, d, -1, -1));
        walls1.add(wall(7, d, e, -1, -1));
        walls1.add(wall(8, e, b, 2, sectorA));

        List<WallView> results = followWallsForJoin(sectorA, sectorB, walls1.get(0), walls1);
        Assert.assertEquals(6, results.size());

        List<Integer> resultsIds = new ArrayList<>(results.size());
        for(WallView r : results){
        	resultsIds.add(r.getWallId());
		}
        Assert.assertTrue(resultsIds.contains(1));
		Assert.assertTrue(resultsIds.contains(5));
		Assert.assertTrue(resultsIds.contains(6));
		Assert.assertTrue(resultsIds.contains(7));
		Assert.assertTrue(resultsIds.contains(3));
		Assert.assertTrue(resultsIds.contains(4));

		List<List<WallView>> moreResults = Map.followAllWallsForJoin(sectorA, sectorB, walls1);
		Assert.assertEquals(1, moreResults.size());
	}

	@Test
	public void testFollowWallsForJoin2(){
		/*
		 *     A--1----B---5----C--------G
		 *     |       ..       ..       |
		 *     4   A   28   B   611  C   |
		 *     |       ..       ..       |
		 *     F---3---E---7----D--------H
		 */
		int sectorA = 0;
		int sectorB = 1;
		int sectorC = 2;

		PointXY a = p(-1024, -1024), b = p(0, -1024), c = p(1024, -1024), g = p(2048, -1024);
		PointXY f = p(-1024, 0), e = p(0, 0), d = p(1024, 0), h = p(2048, 0);

		List<WallView> walls1 = new ArrayList<>();
		walls1.add(wall(1, a, b, -1, -1));
		walls1.add(wall(2, b, e, 8, sectorB));
		walls1.add(wall(3, e, f, -1, -1));
		walls1.add(wall(4, f, a, -1, -1));

		walls1.add(wall(5, b, c, -1, -1));
		walls1.add(wall(6, c, d, 11, sectorC));
		walls1.add(wall(7, d, e, -1, -1));
		walls1.add(wall(8, e, b, 2, sectorA));

		// NOTE: caller is responsible for NOT sending these
		// walls1.add(wall(9, c, g, -1, -1));
		// walls1.add(wall(10, g, h, -1, -1));
		// walls1.add(wall(11, h, d, -1, -1));
		// walls1.add(wall(12, d, c, 5, sectorB));

		List<WallView> results = followWallsForJoin(sectorA, sectorB, walls1.get(0), walls1);
		Assert.assertEquals(6, results.size());
		List<Integer> resultsIds = new ArrayList<>(results.size());
		for(WallView r : results){
			resultsIds.add(r.getWallId());
		}
		Assert.assertTrue(resultsIds.contains(1));
		Assert.assertTrue(resultsIds.contains(4));
		Assert.assertTrue(resultsIds.contains(5));
		Assert.assertTrue(resultsIds.contains(6));
		Assert.assertTrue(resultsIds.contains(3));
		Assert.assertTrue(resultsIds.contains(4));

		List<List<WallView>> moreResults = Map.followAllWallsForJoin(sectorA, sectorB, walls1);
		Assert.assertEquals(1, moreResults.size());
	}

	@Test
	public void testFollowWallsForJoin3(){
		/*
		 *     A---------------B---------------C---------------D---------------E
		 *     |      (A)      .      (B)      .      (C)      .     (D)       |
		 *     |               .               .               .               |
		 *     |               .    P...Q      .     K---L     .               |
		 *     |               .    .(E).      .     |   |     .               |
		 *     |               .    S...R      .     N---M     .               |
		 *     |               .               .               .               |
		 *     J---------------I---------------H---------------G---------------F
		 *
		 */
		int sectorA = 0;
		int sectorB = 1;
		int sectorC = 2;
		int sectorD = 3;
		int sectorE = 4;

		PointXY a = p(-2048, -4096), b = p(-1024, -4096), c = p(0, -4096), d = p(1024, -4096), e = p(2048, -4096);

		PointXY p = p(-512, -1024), q = p(-256, -1024);
		PointXY s = p(-512,  1024), r = p(-256,  1024);

		PointXY k = p(256, -1024), L = p(512, -1024);
		PointXY n = p(256,  1024), m = p(512,  1024);

		PointXY j = p(-2048,  4096), i = p(-1024,  4096), h = p(0,  4096), g = p(1024,  4096), f = p(2048,  4096);

		List<WallView> walls1 = new ArrayList<>();
		// walls1.add(wall(1, a, b, -1, -1)); // sectorA
		// walls1.add(wall(2, b, i, 8, sectorB));
        // walls1.add(wall(3, i, j, -1, -1));
        // walls1.add(wall(4, j, a, -1, -1));

        walls1.add(wall(5, b, c, -1, -1)); //sectorB
        walls1.add(wall(6, c, h, 12, sectorC));
		walls1.add(wall(7, h, i, -1, -1));
		walls1.add(wall(8, i, b, 2, sectorA));

		walls1.add(wall(9, c, d, -1, -1)); // sectorC
		walls1.add(wall(10, d, g, 16, sectorD));
		walls1.add(wall(11, g, h, -1, -1));
		walls1.add(wall(12, h, c, 6, sectorB));

		// walls1.add(wall(13, d, e, -1, -1)); // sectorD
		// walls1.add(wall(14, e, f, -1, -1));
		// walls1.add(wall(15, f, g, -1, -1));
		// walls1.add(wall(16, g, d, 10, sectorC));

		// sector B again, inner
		walls1.add(wall(17, q, p, 21, sectorE));
		walls1.add(wall(18, p, s, 24, sectorE));
		walls1.add(wall(19, s, r, 23, sectorE));
		walls1.add(wall(20, r, q, 22, sectorE));

		// sector E, inside sector B
        // walls1.add(wall(21, p, q, 17, sectorB));
		// walls1.add(wall(22, q, r, 20, sectorB));
		// walls1.add(wall(23, r, s, 19, sectorB));
		// walls1.add(wall(24, s, p, 18, sectorB));

		// sector C again, inner
		walls1.add(wall(25, L, k, -1, -1));
		walls1.add(wall(26, k, n, -1, -1));
		walls1.add(wall(27, n, m, -1, -1));
		walls1.add(wall(28, m, L, -1, -1));

		List<List<WallView>> results = Map.followAllWallsForJoin(sectorB, sectorC, walls1);
		Assert.assertEquals(3, results.size());

		List<Integer> outer = null;
		List<Integer> innerB = null;
		List<Integer> innerC = null;
		for(List<WallView> path: results){
			List<Integer> ids = new ArrayList<Integer>(path.size());
			for(WallView wv: path){
				ids.add(wv.getWallId());
			}
			if(ids.contains(5)){
				outer = ids;
			}else if(ids.contains(17)){
				innerB = ids;
			}else if(ids.contains(25)){
				innerC = ids;
			}else{
				Assert.fail();
			}
		}
		Assert.assertEquals(6, outer.size());
		Assert.assertTrue(outer.contains(5));
		Assert.assertTrue(outer.contains(9));
		Assert.assertTrue(outer.contains(10));
		Assert.assertTrue(outer.contains(11));
		Assert.assertTrue(outer.contains(7));
		Assert.assertTrue(outer.contains(8));

		Assert.assertEquals(4, innerB.size());
		Assert.assertTrue(innerB.contains(17));
		Assert.assertTrue(innerB.contains(18));
		Assert.assertTrue(innerB.contains(19));
		Assert.assertTrue(innerB.contains(20));

		Assert.assertEquals(4, innerC.size());
		Assert.assertTrue(innerC.contains(25));
		Assert.assertTrue(innerC.contains(26));
		Assert.assertTrue(innerC.contains(27));
		Assert.assertTrue(innerC.contains(28));


		// TODO - next, an endpoint that gets the walls on its own?  might need a real map for that
	}

	private List<List<WallView>> toLists(List<WallView> ... lists){
		return Arrays.asList(lists);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFirstWallFirstThrows(){
		List<WallView> list1 = new ArrayList<>();
		PointXY p1 = p(0, 0);
		PointXY p2 = p(64, 0);
		list1.add(wall(42, p1, p2, -1, -1));

		List<List<WallView>> results = Map.firstWallFirst(toLists(list1), 1);
	}

	@Test(expected = IllegalArgumentException.class)
	public void testFirstWallFirstThrows2(){
		Map.firstWallFirst(Collections.emptyList(), 42);
	}

	@Test
	public void testFirstWallFirst(){
		PointXY p1 = p(0, 0);
		PointXY p2 = p(64, 0);
		List<WallView> list1 = new ArrayList<>();
		list1.add(wall(42, p1, p2, -1, -1));

		List<List<WallView>> results1 = Map.firstWallFirst(toLists(list1), 42);
		Assert.assertEquals(1, results1.size());
		Assert.assertEquals(1, results1.get(0).size());
		Assert.assertEquals(42, results1.get(0).get(0).getWallId());

		List<WallView> list2 = new ArrayList<>();
		list2.add(wall(42, p1, p2, -1, -1));
		list2.add(wall(43, p1, p2, -1, -1));

		List<List<WallView>> results2 = Map.firstWallFirst(toLists(list2), 42);
		Assert.assertEquals(1, results2.size());
		Assert.assertEquals(2, results2.get(0).size());
		Assert.assertEquals(42, results2.get(0).get(0).getWallId());

		List<WallView> list3 = new ArrayList<>();
		list3.add(wall(43, p1, p2, -1, -1));
		list3.add(wall(42, p1, p2, -1, -1));

		List<List<WallView>> results3 = Map.firstWallFirst(toLists(list2), 42);
		Assert.assertEquals(1, results3.size());
		Assert.assertEquals(2, results3.get(0).size());
		Assert.assertEquals(42, results3.get(0).get(0).getWallId());

		List<WallView> list4a = new ArrayList<>();
		list4a.add(wall(1, p1, p2, -1, -1));
		list4a.add(wall(2, p1, p2, -1, -1));
		list4a.add(wall(3, p1, p2, -1, -1));
		list4a.add(wall(4, p1, p2, -1, -1));
		list4a.add(wall(5, p1, p2, -1, -1));

		List<WallView> list4b = new ArrayList<>();
		list4b.add(wall(6, p1, p2, -1, -1));
		list4b.add(wall(7, p1, p2, -1, -1));
		list4b.add(wall(8, p1, p2, -1, -1));
		list4b.add(wall(9, p1, p2, -1, -1));
		list4b.add(wall(10, p1, p2, -1, -1));

		List<WallView> list4c = new ArrayList<>();
		list4c.add(wall(11, p1, p2, -1, -1));
		list4c.add(wall(12, p1, p2, -1, -1));
		list4c.add(wall(13, p1, p2, -1, -1));
		list4c.add(wall(14, p1, p2, -1, -1));
		list4c.add(wall(15, p1, p2, -1, -1));

		List<List<List<WallView>>> combos = new ArrayList<>(6);
		combos.add(toLists(list4a, list4b, list4c));
		combos.add(toLists(list4a, list4c, list4b));
		combos.add(toLists(list4b, list4a, list4c));
		combos.add(toLists(list4b, list4c, list4a));
		combos.add(toLists(list4c, list4a, list4b));
		combos.add(toLists(list4c, list4b, list4a));


		for(int i = 1; i <= 15; ++i){
			for(List<List<WallView>> testList: combos){
				//List<List<WallView>> results = Map.firstWallFirst(toLists(list4a, list4b, list4c), i);
				List<List<WallView>> results = Map.firstWallFirst(testList, i);
				Assert.assertEquals(i, results.get(0).get(0).getWallId());
				Assert.assertEquals(3, results.size());
				for(int j = 0; j < 3; ++j){
					Assert.assertEquals(5, results.get(j).size());
				}

				if(i == 3){
					Assert.assertEquals(3, results.get(0).get(0).getWallId());
					Assert.assertEquals(4, results.get(0).get(1).getWallId());
					Assert.assertEquals(5, results.get(0).get(2).getWallId());
					Assert.assertEquals(1, results.get(0).get(3).getWallId());
					Assert.assertEquals(2, results.get(0).get(4).getWallId());
				}

			}
		}
	}

	@Test
	public void testFollowAllWallsForJoinRealMap() throws Exception {
		Map map = JavaTestUtils.readTestMap(JavaTestUtils.JOIN);
		Map map1 = PrefabPalette.fromMap(map).getSG(1).getMap();

		int sectorIdA = 0;
		int sectorIdB = 1;

		Set<Integer> expectedWallIds = new HashSet<>();

		List<WallView> walls = new ArrayList<>();
		for(int i: map1.getAllSectorWallIds(map1.getSector(sectorIdA))){
			walls.add(map1.getWallView(i));
			expectedWallIds.add(map1.getWallView(i).getWallId());
		}
		for(int i: map1.getAllSectorWallIds(map1.getSector(sectorIdB))){
			walls.add(map1.getWallView(i));
			expectedWallIds.add(map1.getWallView(i).getWallId());
		}
		Assert.assertEquals(expectedWallIds.size(), walls.size());
		List<List<WallView>> results1 = Map.followAllWallsForJoin(sectorIdA, sectorIdB, walls);

		Set<Integer> resultIds = new HashSet<>();
		for(List<WallView> loop: results1){
			for(WallView view: loop){
				resultIds.add(view.getWallId());
			}
		}

		// -2 because of the red walls!
		Assert.assertEquals(expectedWallIds.size() - 2, resultIds.size());
		for(int w: expectedWallIds){
			if(! resultIds.contains(w)){
				Assert.assertTrue(map1.getWall(w).isRedWall());
			}else{
				Assert.assertFalse(map1.getWall(w).isRedWall());
			}
		}
	}

	private PlayerStart findPlayerStart(Map map){
		for(int i = 0; i < map.getSpriteCount(); ++i){
			Sprite sprite = map.getSprite(i);
			if(sprite.getTex() == PrefabUtils.MARKER_SPRITE_TEX && sprite.getLotag() == PrefabUtils.MarkerSpriteLoTags.PLAYER_START){
				return new PlayerStart(sprite);
			}
		}
		throw new RuntimeException("cant find player start marker");
	}

	@Test
	public void testJoinSectors1() throws Exception {
		Map map = JavaTestUtils.readTestMap(JavaTestUtils.JOIN);
		PrefabPalette palette = PrefabPalette.fromMap(map);

		SectorGroup sg1 = palette.getSG(1);
		Map map1 = sg1.getMap();
		Assert.assertEquals(2, map1.getSectorCount());

		int sectorIdA = 0;
		int sectorIdB = 1;

		PlayerStart pstart = findPlayerStart(map);
        //for(int i = 0; i < sg1.map().getSpriteCount(); ++i){
        //	Sprite sprite = sg1.map().getSprite(i);
        //	if(sprite.getTex() == PrefabUtils.MARKER_SPRITE_TEX && sprite.getLotag() == PrefabUtils.MarkerSpriteLoTags.PLAYER_START){
        //		pstart = new PlayerStart(sprite);

		//	}
		//}

		Assert.assertEquals(2, map1.getSectorCount());
		Assert.assertEquals(4, map1.getSpriteCount());
		Assert.assertEquals(8, map1.getWallCount());

		Assert.assertTrue(map1.getSector(0).getFloorTexture() > 0);

		map1.joinSectors(sectorIdA, sectorIdB);
		map1.assertIntegrity();
		Assert.assertEquals(1, map1.getSectorCount());
		Assert.assertEquals(4, map1.getSpriteCount());
		Assert.assertEquals(6, map1.getWallCount());
		Assert.assertTrue(map1.getSector(0).getFloorTexture() > 0);

		map1.setPlayerStart(pstart);
		// Main.deployTest(map1);
	}

	private int sectorWithSprite(Map map, int spriteTex){
		for(int i = 0; i < map.getSpriteCount(); ++i){
			Sprite s = map.getSprite(i);
			if(s.getTex() == spriteTex){
				return s.getSectorId();
			}
		}
		throw new RuntimeException("could not find sprite with tex " + spriteTex);
	}

	@Test
	public void testJoinSectors2() throws Exception {
		Map map = JavaTestUtils.readTestMap(JavaTestUtils.JOIN);
		PrefabPalette palette = PrefabPalette.fromMap(map);
		SectorGroup sg2 = palette.getSG(2);
		Map map2 = sg2.getMap();
		Assert.assertEquals(3, map2.getSectorCount());

		int sectorIdA = sectorWithSprite(map2, TextureList.Items.CHAINGUN);
		int sectorIdB = sectorWithSprite(map2, TextureList.Items.RPG);

		Assert.assertEquals(3, map2.getSectorCount());
		map2.joinSectors(sectorIdA, sectorIdB);
		map2.assertIntegrity();
		Assert.assertEquals(2, map2.getSectorCount());

		map2.setPlayerStart(findPlayerStart(map2));
		// Main.deployTest(map2);
	}

	@Test
	public void testJoinSectors3() throws Exception {
		Map map = JavaTestUtils.readTestMap(JavaTestUtils.JOIN);
		PrefabPalette palette = PrefabPalette.fromMap(map);
		SectorGroup sg3 = palette.getSG(3);
		Map map3 = sg3.getMap();
		Assert.assertEquals(4, map3.getSectorCount());

		int sectorIdA = sectorWithSprite(map3, TextureList.Items.CHAINGUN);
		int sectorIdB = sectorWithSprite(map3, TextureList.Items.RPG);

		Assert.assertEquals(4, map3.getSectorCount());
		Assert.assertEquals(32, map3.getWallCount());
		Assert.assertEquals(4, map3.getSpriteCount());
		map3.joinSectors(sectorIdA, sectorIdB);
		map3.assertIntegrity();
		Assert.assertEquals(3, map3.getSectorCount());
		Assert.assertEquals(30, map3.getWallCount());
		Assert.assertEquals(4, map3.getSpriteCount());

		map3.setPlayerStart(findPlayerStart(map3));
		// Main.deployTest(map3);
	}

	@Test
	public void testJoinSectors4() throws Exception {
		Map map = JavaTestUtils.readTestMap(JavaTestUtils.JOIN);
		PrefabPalette palette = PrefabPalette.fromMap(map);
		SectorGroup sg4 = palette.getSG(4);
		Map map4 = sg4.getMap();
		Assert.assertEquals(12, map4.getSectorCount());

		int sectorIdA = sectorWithSprite(map4, TextureList.Items.CHAINGUN);
		int sectorIdB = sectorWithSprite(map4, TextureList.Items.RPG);

		Assert.assertEquals(12, map4.getSectorCount());
		Assert.assertEquals(91, map4.getWallCount());
		Assert.assertEquals(4, map4.getSpriteCount());
		map4.joinSectors(sectorIdA, sectorIdB);
		map4.assertIntegrity();
		Assert.assertEquals(11, map4.getSectorCount());
		Assert.assertEquals(91-2, map4.getWallCount());
		Assert.assertEquals(4, map4.getSpriteCount());

		map4.setPlayerStart(findPlayerStart(map4));
		// Main.deployTest(map4);
	}

	@Test
	public void testJoinSectors5() throws Exception {
		Map map = PrefabPalette.fromMap(JavaTestUtils.readTestMap(JavaTestUtils.JOIN)).getSG(5).getMap();
		Assert.assertEquals(6, map.getSectorCount());

		int sectorIdA = sectorWithSprite(map, TextureList.Items.CHAINGUN);
		int sectorIdB = sectorWithSprite(map, TextureList.Items.RPG);

		Assert.assertEquals(6, map.getSectorCount());
		Assert.assertEquals(4, map.getSpriteCount());
		map.joinSectors(sectorIdA, sectorIdB);
		map.assertIntegrity();
		Assert.assertEquals(5, map.getSectorCount());
		Assert.assertEquals(4, map.getSpriteCount());

		map.setPlayerStart(findPlayerStart(map));
		// Main.deployTest(map);
	}

	@Test
	public void testJoinSectors6() throws Exception {
		Map map = PrefabPalette.fromMap(JavaTestUtils.readTestMap(JavaTestUtils.JOIN)).getSG(6).getMap();
		Assert.assertEquals(3, map.getSectorCount());

		int sectorIdA = sectorWithSprite(map, TextureList.Items.CHAINGUN);
		int sectorIdB = sectorWithSprite(map, TextureList.Items.RPG);

		Assert.assertEquals(3, map.getSectorCount());
		Assert.assertEquals(4, map.getSpriteCount());
		map.joinSectors(sectorIdA, sectorIdB);
		map.assertIntegrity();
		Assert.assertEquals(2, map.getSectorCount());
		Assert.assertEquals(4, map.getSpriteCount());

		map.setPlayerStart(findPlayerStart(map));
		// Main.deployTest(map);
	}


}
