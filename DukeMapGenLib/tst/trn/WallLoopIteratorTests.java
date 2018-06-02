package trn;

import java.util.Collection;
import java.util.Iterator;

import org.junit.Assert;

import org.junit.Test;

public class WallLoopIteratorTests {
	
	Wall createWall(int nextWallId){
		Wall w = new Wall();
		w.point2 = (short)nextWallId;
		return w;
	}
	
	@Test
	public void testSomething(){
		
		Map map = Map.createNew();
		int firstWallId = 0;
		int wallCount = 3 + 4 + 5;
				
		int sectorId = map.addSector(new Sector(firstWallId, wallCount));
		
		map.addWall(createWall(1)); // 0
		map.addWall(createWall(2));
		map.addWall(createWall(0));
		
		map.addWall(createWall(4)); // this is 3
		map.addWall(createWall(5)); // this is 4
		map.addWall(createWall(6)); // 5
		map.addWall(createWall(3)); // 6
		
		map.addWall(createWall(8)); // 7
		map.addWall(createWall(9));
		map.addWall(createWall(10));
		map.addWall(createWall(11));
		map.addWall(createWall(7));
		
		Iterator<Collection<Integer>> wallLoopIterator = map.wallLoopIterator(sectorId);
		Assert.assertTrue(wallLoopIterator.hasNext());
		
		Collection<Integer> loop = wallLoopIterator.next();
		Assert.assertEquals(3, loop.size());
		Assert.assertTrue(wallLoopIterator.hasNext());
		
		loop = wallLoopIterator.next();
		Assert.assertEquals(4, loop.size());
		Assert.assertTrue(wallLoopIterator.hasNext());
		
		loop = wallLoopIterator.next();
		Assert.assertEquals(5, loop.size());
		Assert.assertFalse(wallLoopIterator.hasNext());
		
	}

}
