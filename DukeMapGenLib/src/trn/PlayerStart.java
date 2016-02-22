package trn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class PlayerStart {
	
	public static final int BYTE_COUNT = 14;

	
	final long x; //UINT32LE
	final long y; //UINT32LE
	final long z; //UINT32LE
	final int angle; //UINT16LE
	
	public PlayerStart(long x, long y, long z, int angle){
		this.x = x;
		this.y = y;
		this.z = z;
		this.angle = angle;
		
	}
	
	public long x(){
		return this.x;
	}
	
	public long getX(){ return this.x(); }
	
	public long y(){
		return this.y;
	}
	
	public long getY(){ return this.y(); }
	
	public long z(){
		return this.z; //TODO: should we deal with the shift here?
	}
	
	public long getZ(){ return this.z(); }
	
	public int getAngle(){
		return this.angle;
	}
	
	@Override
	public String toString(){
		
		return String.format("{ x=%d, y=%d, z=%d angle=%d", x, y, z, angle);
	}
	
	public void toBytes(OutputStream output) throws IOException {
		ByteUtil.writeUint32LE(output, x);
		ByteUtil.writeUint32LE(output, y);
		ByteUtil.writeUint32LE(output, z);
		ByteUtil.writeUint16LE(output, angle);
	}
	
	public static PlayerStart fromBytes(byte[] bytes, int start){
		
		//note:  ByteArrayInputStream does have a constructor that takes an offset...
		
		return new PlayerStart(
				ByteUtil.readUint32LE(bytes, start),
				ByteUtil.readUint32LE(bytes, (start += 4)),
				ByteUtil.readUint32LE(bytes, (start += 4)),
				ByteUtil.readUint16LE(bytes, (start += 2))
				);
	}
	
	public static PlayerStart readPlayerStart(InputStream input) throws IOException{
		
		return new PlayerStart(
				ByteUtil.readUint32LE(input),
				ByteUtil.readUint32LE(input),
				ByteUtil.readUint32LE(input),
				ByteUtil.readUint16LE(input)
				);
		
	}
	
}
