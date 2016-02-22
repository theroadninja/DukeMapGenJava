package trn;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;

import org.junit.Assert;

import org.junit.Test;

public class ByteUtilTests {
	
	static byte[] bytes0 = new byte[]{ 
		(byte)255, 
		(byte)128, 
		(byte)-64, 
		(byte)32,
		
		(byte)0,
		(byte)1,
		(byte)2,
		(byte)-256,
		};

	@Test
	public void testSignedUnsigned32() throws Exception {
		
		
		ByteArrayInputStream input = new ByteArrayInputStream(bytes0);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		{
			long first = ByteUtil.readUint32LE(input);
			long second = ByteUtil.readUint32LE(input);
			ByteUtil.writeUint32LE(output, first);
			ByteUtil.writeUint32LE(output, second);
		}

		byte[] bytes1 = output.toByteArray();
		
		Assert.assertEquals(bytes0.length, bytes1.length);
		
		Assert.assertArrayEquals(bytes0, bytes1);
		
		Assert.assertEquals((byte)-256, bytes1[7]);
		

		//one more time
		
		ByteArrayInputStream input2 = new ByteArrayInputStream(bytes1);
		ByteArrayOutputStream output2 = new ByteArrayOutputStream();
		{
			long first = ByteUtil.readUint32LE(input2);
			long second = ByteUtil.readUint32LE(input2);
			ByteUtil.writeUint32LE(output2, first);
			ByteUtil.writeUint32LE(output2, second);
		}
		
		byte[] bytes2 = output2.toByteArray();
		
		Assert.assertEquals(bytes0.length, bytes2.length);
		
		Assert.assertArrayEquals(bytes0, bytes2);
		
		
	}
	
	
	@Test
	public void testSignedUnsigned16() throws Exception {

		ByteArrayInputStream input = new ByteArrayInputStream(bytes0);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		{
			int first = ByteUtil.readUint16LE(input);
			int second = ByteUtil.readUint16LE(input);
			int third = ByteUtil.readUint16LE(input);
			int fourth = ByteUtil.readUint16LE(input);
			
			ByteUtil.writeUint16LE(output, first);
			ByteUtil.writeUint16LE(output, second);
			ByteUtil.writeUint16LE(output, third);
			ByteUtil.writeUint16LE(output, fourth);
		}

		byte[] bytes1 = output.toByteArray();
		
		Assert.assertEquals(bytes0.length, bytes1.length);
		
		Assert.assertArrayEquals(bytes0, bytes1);
		
		
	}
	
	
	@Test
	public void testSigned16() throws Exception {
		
		ByteArrayInputStream input = new ByteArrayInputStream(bytes0);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		{
			short first = ByteUtil.readInt16LE(input);
			short second = ByteUtil.readInt16LE(input);
			short third = ByteUtil.readInt16LE(input);
			short fourth = ByteUtil.readInt16LE(input);
			
			ByteUtil.writeInt16LE(output, first);
			ByteUtil.writeInt16LE(output, second);
			ByteUtil.writeInt16LE(output, third);
			ByteUtil.writeInt16LE(output, fourth);
		}
		

		byte[] bytes1 = output.toByteArray();
		
		Assert.assertEquals(bytes0.length, bytes1.length);
		
		Assert.assertArrayEquals(bytes0, bytes1);
		
	}
	
	@Test
	public void testUnsigned8() throws Exception {
		
		ByteArrayInputStream input = new ByteArrayInputStream(bytes0);
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		{
			short first = ByteUtil.readUInt8(input);
			short second = ByteUtil.readUInt8(input);
			short third = ByteUtil.readUInt8(input);
			short fourth = ByteUtil.readUInt8(input);
			
			short five = ByteUtil.readUInt8(input);
			short six = ByteUtil.readUInt8(input);
			short seven = ByteUtil.readUInt8(input);
			short eight = ByteUtil.readUInt8(input);
			
			ByteUtil.writeUint8(output, first);
			ByteUtil.writeUint8(output, second);
			ByteUtil.writeUint8(output, third);
			ByteUtil.writeUint8(output, fourth);
			
			ByteUtil.writeUint8(output, five);
			ByteUtil.writeUint8(output, six);
			ByteUtil.writeUint8(output, seven);
			ByteUtil.writeUint8(output, eight);
		}
		

		byte[] bytes1 = output.toByteArray();
		
		Assert.assertEquals(bytes0.length, bytes1.length);
		
		Assert.assertArrayEquals(bytes0, bytes1);
		
	}
}
