package trn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;

import org.apache.commons.io.IOUtils;
import org.junit.Assert;
import org.junit.Test;

public class MapSerializationTests {

	
	/**
	 * read in a map and write back out to make sure the bytes match perfectly
	 * 
	 * @throws Exception
	 */
	@Test
	public void testSerialize() throws Exception {
		
		byte[] bytes = IOUtils.toByteArray(new FileInputStream(new File(ParseOneRoomTests.oneRoomFilename())));
		
		
		//read in map
		Map m = Map.readMap(new ByteArrayInputStream(bytes));
		
		//write back out
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		m.toBytes(output);
		
		byte[] bytes1 = output.toByteArray();
		Assert.assertArrayEquals(bytes, bytes1);
		
		//in and out again
		ByteArrayOutputStream output2 = new ByteArrayOutputStream();
		Map.readMap(new ByteArrayInputStream(bytes1)).toBytes(output2);
		
		Assert.assertArrayEquals(bytes, output2.toByteArray());
		
	}
}
