package trn;

import java.io.IOException;
import java.io.InputStream;

public class Sprite {
	
	int x; //INT32LE
	int y; //INT32LE
	int z; //INT32LE -- possibly fucked by 4
	
	short cstat; //INT16LE
	short picnum; //INT16LE
	
	short shade; //should be INT8, but i'm treating it as UINT8
	short pal; //UINT8
	short clipdist; //UINT8
	short filler; //UINT8
	short xrepeat; //UINT8
	short yrepeat; //UINT8
	short xoffset; //wiki says INT8, i'm using it as UINT8
	short yoffset; //wiki says INT8, i'm using it as UINT8
	
	short sectnum; //INT16LE sector of sprite's location
	short statnum; //INT16LE.  status or sprite
	short ang; //INT16LE - angle
	
	//the rest are INT16LE.  are some of these only used at runtime?
	short owner;
	short xvel;
	short yvel;
	short zvel;
	short lotag;
	short hitag;
	short extra;
	
	public short getTexture(){
		return this.picnum;
	}
	
	
	public static Sprite readSprite(InputStream input) throws IOException {
		Sprite s = new Sprite();
		
		s.x = ByteUtil.readInt32LE(input);
		s.y = ByteUtil.readInt32LE(input);
		s.z = ByteUtil.readInt32LE(input);
		
		s.cstat = ByteUtil.readInt16LE(input);
		s.picnum = ByteUtil.readInt16LE(input);
		
		s.shade = ByteUtil.readUInt8(input);
		s.pal = ByteUtil.readUInt8(input);
		s.clipdist = ByteUtil.readUInt8(input);
		s.filler = ByteUtil.readUInt8(input);
		s.xrepeat = ByteUtil.readUInt8(input);
		s.yrepeat = ByteUtil.readUInt8(input);
		s.xoffset = ByteUtil.readUInt8(input);
		s.yoffset = ByteUtil.readUInt8(input);
		
		s.sectnum = ByteUtil.readInt16LE(input);
		s.statnum = ByteUtil.readInt16LE(input);
		s.ang = ByteUtil.readInt16LE(input);
		
		s.owner = ByteUtil.readInt16LE(input);
		s.xvel = ByteUtil.readInt16LE(input);
		s.yvel = ByteUtil.readInt16LE(input);
		s.zvel = ByteUtil.readInt16LE(input);
		s.lotag = ByteUtil.readInt16LE(input);
		s.hitag = ByteUtil.readInt16LE(input);
		s.extra = ByteUtil.readInt16LE(input);
		
		
		
		
		return s;
		
	}
	
	

}
