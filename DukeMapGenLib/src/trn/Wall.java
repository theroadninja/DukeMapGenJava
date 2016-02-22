package trn;

import java.io.IOException;
import java.io.InputStream;

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
 
	
	/** texture index, a.k.a. picnum */
	public short getTexture(){
		return this.picnum;
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
		
		System.out.println("wall pic index: " + w.picnum); //oneroom.map wall pic is 191
		
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
