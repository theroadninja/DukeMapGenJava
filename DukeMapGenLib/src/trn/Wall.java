package trn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Wall {
	
	int x; //INT32LE
	int y; //INT32LE
	short point2; //INT16LE
	
	short nextWall; //INT16LE ... -1 means no sector on other side of wall
		//think this is for red (two-sided) walls
	
	short nextSector; //INT16LE ... -1 means no sector on other side of wall
	
	short cstat; //INT16LE
	
	short picnum; //INT16LE --- why the fuck is this signed?
	
	short overpicnum; //INT16LE  masked/oneway walls
	
	short shade; //should be INT8, i'm pretending its UINT8
	
	short pal; //UINT8
	
	short xrepeat; //UINT8
	
	short yrepeat; //UINT8
	
	short xpanning; //UINT8
	
	short ypanning; //UINT8
	
	short lotag; //INT16LE
	
	short hitag; //INT16LE
	
	short extra; //INT16LE
	
	public Wall(){
		
	}
	
	
	public Wall(int x, int y, int point2){
		this(x, y, point2, 0);
	}
	
	
	public Wall(int x, int y, int point2, int wallTex){ //accepting ints because java is a pain in the ass about literals
		this.x = x;
		this.y = y;
		this.point2 = (short)point2;
		
		this.nextWall = -1;
		this.nextSector = -1;
		
		this.cstat = 0;
		
		this.picnum = (short)wallTex; //0 is the ugly brick
		this.overpicnum = 0;
		this.shade = 0;
		this.pal = 0;
		
		//xrepeat = ?
		//yrepeat = ?
		
		this.xpanning = 0;
		this.ypanning = 0;
		
		this.lotag = 0;
		this.hitag = 0;
		this.extra = -1;
	}
	
	public void setOtherSide(int nextWall, int nextSector){
		this.nextWall = (short)nextWall;
		this.nextSector = (short)nextSector;
	}
	
	public void setTexture(int texture){
		this.picnum = (short)texture;
	}
	
	/** texture index, a.k.a. picnum */
	public short getTexture(){
		return this.picnum;
	}
	
	public void setXRepeat(short s){
		this.xrepeat = s;
	}
	public void setXRepeat(int i){ setXRepeat((short)i); }
	
	public void setYRepeat(short yr){
		this.yrepeat = yr;
	}
	public void setYRepeat(int i){ setYRepeat((short)i); }
	
	@Override
	public String toString(){
		String ln = "\n"; //why isn't there an appenln() ?
		
		StringBuilder sb = new StringBuilder();
		sb.append("{ wall").append("\n");
		sb.append("x : ").append(x).append(ln);
		sb.append("y : ").append(y).append(ln);
		
		sb.append("point2 : ").append(point2).append(ln);
		sb.append("nextWall : ").append(nextWall).append(ln);
		sb.append("nextSector : ").append(nextSector).append(ln);
		sb.append("cstat : ").append(cstat).append(ln);
		sb.append("picnum : ").append(picnum).append(ln);
		
		sb.append("overpicnum : ").append(overpicnum).append(ln);
		
		sb.append("shade : ").append(shade).append(ln);
		sb.append("pal : ").append(pal).append(ln);
		sb.append("xrepeat : ").append(xrepeat).append(ln);
		sb.append("yrepeat : ").append(yrepeat).append(ln);
		sb.append("xpanning : ").append(xpanning).append(ln);
		sb.append("ypanning : ").append(ypanning).append(ln);
		
		sb.append("lotag : ").append(lotag).append(ln);
		sb.append("hitag : ").append(hitag).append(ln);
		sb.append("extra : ").append(extra).append(ln);
		sb.append("}").append(ln);
		
		return sb.toString();
	}
	
	public void toBytes(OutputStream output) throws IOException {
		
		ByteUtil.writeInt32LE(output, x);
		ByteUtil.writeInt32LE(output, y);
		
		ByteUtil.writeInt16LE(output, point2);
		ByteUtil.writeInt16LE(output, nextWall);
		ByteUtil.writeInt16LE(output, nextSector);
		ByteUtil.writeInt16LE(output, cstat);
		ByteUtil.writeInt16LE(output, picnum);
		
		ByteUtil.writeInt16LE(output, overpicnum);
		
		ByteUtil.writeUint8(output, shade);
		ByteUtil.writeUint8(output, pal);
		ByteUtil.writeUint8(output, xrepeat);
		ByteUtil.writeUint8(output, yrepeat);
		ByteUtil.writeUint8(output, xpanning);
		ByteUtil.writeUint8(output, ypanning);
		
		ByteUtil.writeInt16LE(output, lotag);
		ByteUtil.writeInt16LE(output, hitag);
		ByteUtil.writeInt16LE(output, extra);
		
	}
	
	public static Wall readWall(InputStream input) throws IOException {
		Wall w = new Wall();
		
		w.x = ByteUtil.readInt32LE(input);
		w.y = ByteUtil.readInt32LE(input);
		w.point2 = ByteUtil.readInt16LE(input);
		w.nextWall = ByteUtil.readInt16LE(input);
		w.nextSector = ByteUtil.readInt16LE(input);
		w.cstat = ByteUtil.readInt16LE(input);
		w.picnum = ByteUtil.readInt16LE(input);
		
		//System.out.println("wall pic index: " + w.picnum); //oneroom.map wall pic is 191
		
		w.overpicnum = ByteUtil.readInt16LE(input);
		w.shade = ByteUtil.readUInt8(input);
		w.pal = ByteUtil.readUInt8(input);
		w.xrepeat = ByteUtil.readUInt8(input);
		w.yrepeat = ByteUtil.readUInt8(input);
		w.xpanning = ByteUtil.readUInt8(input);
		w.ypanning = ByteUtil.readUInt8(input);
		w.lotag = ByteUtil.readInt16LE(input);
		w.hitag = ByteUtil.readInt16LE(input);
		w.extra = ByteUtil.readInt16LE(input);
		
		
		
		return w;
	}

}
