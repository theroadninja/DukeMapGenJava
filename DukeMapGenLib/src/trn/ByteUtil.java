package trn;

import java.io.IOException;
import java.io.InputStream;

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
	
	public static int readUint16LE(byte[] bytes, int start){
		
		//according to apache docs, this is a 16 bit integer
		return EndianUtils.readSwappedUnsignedShort(bytes, start);
	}
	
	public static int readUint16LE(InputStream input) throws IOException{
		
		//according to apache docs, this is a 16 bit integer
		return EndianUtils.readSwappedUnsignedShort(input);
	}
	
	
	
	public static int readInt32LE(InputStream input) throws IOException {
		return EndianUtils.readSwappedInteger(input);
	}
	
	
	public static short readInt16LE(InputStream input) throws IOException {
		return EndianUtils.readSwappedShort(input);
	}
	
	
	
	
	
	//endian-ness is only for values greater than a single byte...but what about signed vs unsinged?
	
	
	
	public static short readUInt8(InputStream input) throws IOException {
		
		//originally wrote this function for sector ceilingshade, which is: 
		
		////this is an INT8 ; a.k.a. signed integer
		//byte is signed...
		//char is two-byte unsigned:  http://stackoverflow.com/questions/4458352/purpose-of-char-in-java
		//so char is really more of an unsigned short...
		
		
		int i = input.read(); //returns int [0,255]
		
		//so I have a...signed value, but its stored unsigned in a signed type.
		//fuck my head hurts
		//can i just subtract 128?
		
		//according to what I've read, we need to do this:
		//i = i - 128; //so now its in the range [-128,127]
		//however that gives a number that doesnt match what build reports
		
		
		
		return (byte)i;
		
	}
	
}
