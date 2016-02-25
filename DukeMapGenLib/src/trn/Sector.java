package trn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Sector {

	private short firstWall; //a.k.a. wallprt
	private short wallCount; //a.k.a. wallnum
	
	
	private int ceilingz; //z coord of ceiling at first point in sector
	private int floorz; //z coord of floor at first point of sector
	
	
	private short ceilingStat;
	private short floorstat;
	private short ceilingPicNum;
	private short ceilingheinum; //slope value?
	
	
	/** note:  buildhlp and the wiki say this is signed, but i'm pretending its unsigned */
	private short ceilingshade; 
		//this should be, INT8, a.k.a. signed byte.  however i'm pretending it is unsinged
		//because that way at least I can get the number to match what build reports when you
		//press h,esc
	
	
	private short ceilingpal; //pallet lookup num, UINT8
	private short ceilingxpanning; //tex coordinate X-offset for ceiling, UINT8
	private short ceilingypanning; //tex coordinate Y-offset for ceiling, UINT8
	
	private short floorpicnum; //floor texture index - INT16LE
	private short floorheinum; //floor slope value, INT16LE
	
	private short floorshade; //should be INT8, but i'm making it UINT8, see ceilingshade
	
	private short floorpal; //floor pallette.  UINT8
	
	private short floorxpanning;//UINT8
	private short floorypanning;//UINT8
	private short visibility;//UINT8
	private short filler; //UINT8 just a padding byte
	
	private short lotag; //INT16LE
	private short hitag; //INT16LE
	private short extra; //INT16LE
	
	private Sector(){
		
	}
	
	public Sector(int firstWall, int wallCount){
		this.firstWall = (short)firstWall;
		this.wallCount = (short)wallCount;
		
		this.ceilingStat = 0;
		this.floorstat = 0;
		
		this.ceilingheinum = 0;
		this.ceilingshade = 0;
		this.ceilingpal = 0;
		this.ceilingxpanning = 0;
		this.ceilingypanning = 0;
		
		this.floorheinum = 0;
		this.floorshade = 0;
		this.floorpal = 0;
		this.floorxpanning = 0;
		this.floorypanning = 0;
		
		this.visibility = 0;
		this.filler = 0;
		
		this.lotag = 0;
		this.hitag = 0;
		this.extra = -1;
	}
	
	public void setCeilingZ(int z){
		this.ceilingz = z;
	}
	public int getCeilingZ(){
		return this.ceilingz;
	}
	
	public void setFloorZ(int z){
		this.floorz = z;
	}
	public int getFloorZ(){
		return this.floorz;
	}
	
	public void setCeilingTexture(int i){
		this.ceilingPicNum = (short)i;
	}
	public short getCeilingTexture(){
		return this.ceilingPicNum;
	}
	
	
	public void setFloorTexture(int i){
		this.floorpicnum = (short)i;
	}
	public short getFloorTexture(){
		return this.floorpicnum;
	}
	
	public short getCeilingShadeAsUnsigned(){
		return this.ceilingshade;
	}
	
	public short getCeilingPallette(){
		return this.ceilingpal;
	}
	
	public void toBytes(OutputStream output) throws IOException {
		ByteUtil.writeInt16LE(output, firstWall);
		ByteUtil.writeInt16LE(output, wallCount);
		
		ByteUtil.writeInt32LE(output, ceilingz);
		ByteUtil.writeInt32LE(output, floorz);
		
		ByteUtil.writeInt16LE(output, ceilingStat);
		ByteUtil.writeInt16LE(output, floorstat);
		ByteUtil.writeInt16LE(output, ceilingPicNum);
		ByteUtil.writeInt16LE(output, ceilingheinum);
		
		ByteUtil.writeUint8(output, ceilingshade);
		ByteUtil.writeUint8(output, ceilingpal);
		ByteUtil.writeUint8(output, ceilingxpanning);
		ByteUtil.writeUint8(output, ceilingypanning);
		
		ByteUtil.writeInt16LE(output, floorpicnum);
		ByteUtil.writeInt16LE(output, floorheinum);
		
		ByteUtil.writeUint8(output, floorshade);
		ByteUtil.writeUint8(output, floorpal);
		
		
		
		ByteUtil.writeUint8(output, floorxpanning);
		ByteUtil.writeUint8(output, floorypanning);
		ByteUtil.writeUint8(output, visibility);
		ByteUtil.writeUint8(output, filler);
		
		
		ByteUtil.writeInt16LE(output, lotag);
		ByteUtil.writeInt16LE(output, hitag);
		ByteUtil.writeInt16LE(output, extra);
		
	}
	
	public static Sector readSector(InputStream input) throws IOException {
		Sector s = new Sector();
		
		s.firstWall = ByteUtil.readInt16LE(input);
		s.wallCount = ByteUtil.readInt16LE(input);
		
		
		//s.ceilingz = EndianUtils.readSwappedLong(input);
		s.ceilingz = ByteUtil.readInt32LE(input);
		s.floorz = ByteUtil.readInt32LE(input);
		//note: in build, if you press h,esc it tells you a bunch of shit,
		//confirming that the ceiling z really is -8192 and the floor z really is 8192
		//and if you rraise the ceiling, it gets even more negative.  so i guess z values are
		//upside down?
		
		//XXX: what is the shit about z coords being shifted up by 4?
		
		
		s.ceilingStat = ByteUtil.readInt16LE(input);
		s.floorstat = ByteUtil.readInt16LE(input);
		s.ceilingPicNum = ByteUtil.readInt16LE(input);
		s.ceilingheinum = ByteUtil.readInt16LE(input);
		

		s.ceilingshade = ByteUtil.readUInt8(input);
		//in oneroom.map build says ceiling shade is 3 after i darkened it a bit (full bright seems to be 0)
		
		
		s.ceilingpal = ByteUtil.readUInt8(input);
		
		
		s.ceilingxpanning = ByteUtil.readUInt8(input);
		s.ceilingypanning = ByteUtil.readUInt8(input);
		
		
		s.floorpicnum = ByteUtil.readInt16LE(input);
		
		
		s.floorheinum = ByteUtil.readInt16LE(input); 
		
		
		s.floorshade = ByteUtil.readUInt8(input);
		
		
		s.floorpal = ByteUtil.readUInt8(input);
		
		s.floorxpanning = ByteUtil.readUInt8(input);//UINT8
		s.floorypanning = ByteUtil.readUInt8(input);//UINT8
		s.visibility = ByteUtil.readUInt8(input);//UINT8
		s.filler = ByteUtil.readUInt8(input);//UINT8
		
		s.lotag = ByteUtil.readInt16LE(input);//INT16LE
		s.hitag = ByteUtil.readInt16LE(input);//INT16LE
		s.extra = ByteUtil.readInt16LE(input);//INT16LE
		
		return s;
	}
	
	public void print(){
		
		final String ln = "\n";
		
		StringBuilder sb = new StringBuilder();
		sb.append("first wall: ").append(this.firstWall).append(ln);
		
		
		sb.append("ceilingstat: ").append(this.ceilingStat).append(ln);
		sb.append("floorstat: ").append(this.floorstat).append(ln);
		
		sb.append("ceiling picnum: ").append(this.ceilingPicNum).append(ln);
		sb.append("floor picnum: ").append(this.floorpicnum).append(ln);
		System.out.println(sb.toString());
		
		Sector s = this; //being lazy with cut&paste
		
		
		
		
		
		System.out.println("wall count: " + s.wallCount);
		System.out.println("ceilingz: " + s.ceilingz);
		System.out.println("floorz: " + s.floorz);
		System.out.println("ceilingheinum: " + s.ceilingheinum);
		
		System.out.println("ceiling shade(int8): " + s.ceilingshade);
		
		System.out.println("ceilingpal: " + s.ceilingpal);
		
		System.out.println("floor texture index: " + s.floorpicnum);
		
		System.out.println("floor slope: " + s.floorheinum);
		
		System.out.println("floor shade: " + s.floorshade);
		
		System.out.println("floor pallette: " + s.floorpal);
		
		System.out.println("floor x panning: " + s.floorxpanning);
		System.out.println("floor y panning: " + s.floorypanning);
		System.out.println("visibility: " + visibility);
		System.out.println("filler byte (i hope its zero!): " + s.filler);
		
		System.out.println("lotag: " + s.lotag);
		System.out.println("hitag: " + s.hitag);
		System.out.println("extra:" + extra);
		
	}
}
