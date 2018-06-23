package trn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import org.apache.commons.io.EndianUtils;


/**
 * So apparently java sucks balls at dealing with byte order.  The best libs I've found are:
 * 	- apache endianutils https://commons.apache.org/proper/commons-io/apidocs/org/apache/commons/io/EndianUtils.html
 *  - guava   http://docs.guava-libraries.googlecode.com/git/javadoc/com/google/common/io/LittleEndianDataInputStream.html
 *  
 *  Neither seem to be specific enough for my tastes.
 *  
 * 
 * See this:  http://www.shikadi.net/moddingwiki/MAP_Format_%28Build%29
 * and hope its right.
 * 
 * Also in buildhlp.exe its page 40.
 * 
 * TODO:  maybe a custom class extending InputStream would be nice.
 * 
 * @author Dave
 *
 */
public class ByteUtil {

	
	/**
	 * reads a little-endian, unsigned integer.
	 * 
	 * @param bytes
	 * @param start the index to start reading from
	 * @return
	 */
	public static long readUint32LE(byte[] bytes, int start){
		
		//accordding to the apache docs, this is a 32 bit integer.
		return EndianUtils.readSwappedUnsignedInteger(bytes, start);
	}
	
	public static long readUint32LE(InputStream input) throws IOException{
		
		//accordding to the apache docs, this is a 32 bit integer.
		return EndianUtils.readSwappedUnsignedInteger(input);
	}
	
	public static void writeUint32LE(OutputStream output, long value) throws IOException {
		
		//fuck I hope this is right
		//(counting on java to just discard higher bits...)
		EndianUtils.writeSwappedInteger(output, (int)value);
	}
	
	
	
	
	
	
	public static int readUint16LE(byte[] bytes, int start){
		
		//according to apache docs, this is a 16 bit integer
		return EndianUtils.readSwappedUnsignedShort(bytes, start);
	}
	
	public static int readUint16LE(InputStream input) throws IOException{
		
		//according to apache docs, this is a 16 bit integer
		return EndianUtils.readSwappedUnsignedShort(input);
	}
	
	public static void writeUint16LE(OutputStream output, int value) throws IOException {
		
		//unsigned content of larger(but signed) data type converted by truncating upper bits
		EndianUtils.writeSwappedShort(output, (short)value);
	}
	
	
	
	
	
	public static int readInt32LE(InputStream input) throws IOException {
		return EndianUtils.readSwappedInteger(input);
	}
	
	public static void writeInt32LE(OutputStream output, int signedValue) throws IOException {
		EndianUtils.writeSwappedInteger(output, signedValue);
	}
	
	
	
	
	
	public static short readInt16LE(InputStream input) throws IOException {
		return EndianUtils.readSwappedShort(input);
	}
	
	public static void writeInt16LE(OutputStream output, short signedValue) throws IOException {
		
		EndianUtils.writeSwappedShort(output, signedValue);
	}
	
	
	
	//endian-ness is only for values greater than a single byte...but what about signed vs unsinged?
	
	
	//still Little Endian (LE)
	public static short readUInt8(InputStream input) throws IOException {
		
		//originally wrote this function for sector ceilingshade, which is: 
		
		////this is an INT8 ; a.k.a. signed integer
		//byte is signed...
		//char is two-byte unsigned:  http://stackoverflow.com/questions/4458352/purpose-of-char-in-java
		//so char is really more of an unsigned short...
		
		
		int i = input.read(); //returns int [0,255]
		
		if(i < 0){
			throw new RuntimeException("read() javadoc lied");
		}
		
		//so I have a...signed value, but its stored unsigned in a signed type.
		//fuck my head hurts
		//can i just subtract 128?
		
		//according to what I've read, we need to do this:
		//i = i - 128; //so now its in the range [-128,127]
		//however that gives a number that doesnt match what build reports
		
		/*
		 * NOTE:  converting to byte before a short causes the value to overflow into two's complement
		 * (int sees 255 while byte sees -1, and the negative bullshit stays when converted back to a
		 * short.  I believe java extends the sign bit or whatever)
		 *
		 * So bytes are toxic and infect shit with their signed-ness.
		 *
		{
			byte b = (byte)i;
			short sanityCheck = (short)b;
			if(sanityCheck < 0){
				throw new RuntimeException("sanity check failed i=" + i + " b=" + b);
			}
		}
		*/
		
		
		
		short s = (short)i;
		
		if(s < 0){
			throw new RuntimeException("shit");
		}
		
		return s;
		
		//somehow, unit tests worked with this including the intput/output serialization test (which is working):
		//return (byte)i;
		
		

		
	}
	
	public static void writeUint8(OutputStream output, short unsignedValue) throws IOException {
		output.write((int)unsignedValue); // write() only writes the low order bits
	}
	
}
