package trn;

import java.io.File;
import java.io.FileInputStream;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Assert;
import org.junit.Test;

public class ParseOneRoomTests {

	@Test
	public void testParseOneRoom() throws Exception {
		
		String fname =  "ONEROOM.MAP";
		String filepath = System.getProperty("user.dir") + File.separator + "testdata" + File.separator + fname;
		File f = new File(filepath);
		
		Assert.assertTrue(f.exists() && f.isFile());
		
		Map m = Map.readMap(new FileInputStream(f));
		
		
		Assert.assertEquals(7, m.getMapVersion());
		
		Assert.assertEquals(32768, m.getPlayerStart().x());
		Assert.assertEquals(32768, m.getPlayerStart().y());
		Assert.assertEquals(1536, m.getPlayerStart().getAngle());
		
		Assert.assertEquals(0, m.getStartSector());
		Assert.assertEquals(1, m.getSectorCount());
		Assert.assertEquals(4, m.getWallCount());
		
		
		Sector sector = m.getSector(0);
		Assert.assertEquals(183, sector.getFloorPicIndex());
		Assert.assertEquals(1, sector.getCeilingPallette());
		Assert.assertEquals(3, sector.getCeilingShadeAsUnsigned());
		
		Assert.assertEquals(191, m.getWall(0).getTexture());
		
		Assert.assertEquals(2, m.getSpriteCount());
		Set<Integer> tex = new TreeSet<Integer>();
		for(int i = 0; i < m.getSpriteCount(); ++i){
			tex.add((int)m.getSprite(i).getTexture());
		}
		Assert.assertTrue(tex.contains(22));
		Assert.assertTrue(tex.contains(24));
		
		
	}
}
