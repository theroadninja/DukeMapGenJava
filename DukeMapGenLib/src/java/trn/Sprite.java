package trn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Sprite implements IRayXY {

	/**
	 * TODO - is this correct??
	 *
	 * @param angle the angle, in weird duke angle units (0 to 2047, clockwise)
	 * @return
	 */
	public static int rotateAngleCW(int angle){
		angle -= 512;
		if(angle < 0){
			angle += 2048;
		}
		return angle;
	}

	public static int rotateAngleCCW(int angle){
		angle += 512;
		angle = angle % 2048;
		return angle;
	}
	
	public static final short DEFAULT_X_REPEAT = 64;
	public static final short DEFAULT_Y_REPEAT = 64;
	
	public static class CSTAT_FLAGS {
		
		/** cstat you get if you place a sprite on the floor in build */
		public static final int PLACED_ON_FLOOR = 1;
		
		/** cstat you get if you place sprite on floor, then wall align it with 'r' */
		public static final int PLACED_ON_FLOOR_WALL_ALIGNED = 17; // bit 0 blocking, bit 4 wall
		
		/** cstat you get if you place sprite on floor, then floor align it with 'r' */
		public static final int PLACED_ON_FLOOR_FLOOR_ALIGNED = 33; // bit 0 blocking, bit 5 floor
		
		/** cstat you get if you place a sprite directly on the wall */
		public static final short PLACED_ON_WALL = 80; // bit 4 wall sprite, bit 6 one-sided sprite
		
		
		//TODO
		
		// http://www.shikadi.net/moddingwiki/MAP_Format_%28Build%29
		
		public static final int BIT_0_BLOCKING_SPRITE = 1; //a.k.a. the 1 bit
	}
	
	/*
	 * CSTAT notes
	 * 
	 * http://www.shikadi.net/moddingwiki/MAP_Format_%28Build%29
	 * 
	 * 'r' in build changes the floor/wall/player alignment
	 * 
	 * When I put a sprint on the floor and "floor aligned it" it had
	 * cstat 33 = 
	 * 	* bit 0 - blocking sprite
	 *  * bit 5 - floor sprite
	 *  
	 * when I put a sprite on the floor and 'wall sligned' it, it had
	 * cstat 17
	 *  * bit 0 - blocking sprite
	 *  * bit 4 - wall sprite
	 *  
	 * when I put a sprite on the floor and left it player aligned it had
	 * cstat 1
	 *  * bit 0 - blocking sprite
	 *  
	 * when I put a sprite directly onto the wall, it had
	 * cstat 80
	 *  * bit 4 - wall sprite
	 *  * bit 6 - one-sided sprite
	 * 
	 * 
	 * 
	 * 
	 * 
	 */
	
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
	short ang; //INT16LE - angle  (angles are 0-2047 clockwise?) - so 90 degrees = 512 ?
	
	//the rest are INT16LE.  are some of these only used at runtime?
	short owner;
	short xvel;
	short yvel;
	short zvel;

	// lotag is a short, but there are no unsigned shorts in java ...
	int lotag;

	short hitag;
	short extra;
	
	public Sprite(){
		

	}
	
	public Sprite(PointXY xy, int z, int sectnum){
		this(xy.x, xy.y, z, (short)sectnum);
	}
	
	public Sprite(int x, int y, int z, short sectnum){
		
		this.x = x;
		this.y = y;
		this.z = z;
		
		//todo:  picnum ?
		
		//initializing some fields to defaults they'd get if you just place them
		this.cstat = CSTAT_FLAGS.BIT_0_BLOCKING_SPRITE;
		this.shade = 0;
		this.pal = 0;
		this.clipdist = 32;
		this.filler = 0;
		this.xrepeat = DEFAULT_X_REPEAT;
		this.yrepeat = DEFAULT_Y_REPEAT;
		this.xoffset = 0;
		this.yoffset = 0;
		this.sectnum = sectnum;
		this.statnum = 0;
		this.ang = DukeConstants.DEFAULT_ANGLE;
		this.owner = -1;
		this.xvel = this.yvel = this.zvel = 0;
		this.lotag = 0;
		this.hitag = 0;
		this.extra = -1;
		
	}

	public Sprite(PointXYZ xyz, int sectnum, int picnum, int hitag, int lotag){
		this(xyz.asXY(), xyz.z, sectnum);
		this.picnum = (short)picnum;
		this.hitag = (short)hitag;
		this.lotag = lotag;
	}

	public Sprite copy(){
		return copy(this.getSectorId());
	}

	public Sprite copy(short newSectorId){
		Sprite sprite = new Sprite();
		sprite.x = this.x;
		sprite.y = this.y;
		sprite.z = this.z;
		sprite.cstat = this.cstat;
		sprite.picnum = this.picnum;
		sprite.shade = this.shade;
		sprite.pal = this.pal;
		sprite.clipdist = this.clipdist;
		sprite.filler = this.filler;
		sprite.xrepeat = this.xrepeat;
		sprite.yrepeat = this.yrepeat;
		sprite.xoffset = this.xoffset;
		sprite.yoffset = this.yoffset;
		sprite.sectnum = newSectorId;
		sprite.statnum = this.statnum;
		sprite.ang = this.ang;
		sprite.owner = this.owner;
		sprite.xvel = this.xvel;
		sprite.yvel = this.yvel;
		sprite.zvel = this.zvel;
		sprite.lotag = this.lotag;
		sprite.hitag = this.hitag;
		sprite.extra = this.extra;
		return sprite;
	}
	
	public Sprite translate(final PointXYZ transform){
		this.x += transform.x;
		this.y += transform.y;
		this.z += transform.z;
		return this;
	}

	public int getAngle() {
		return this.ang;
	}

	public void setAng(int a){
		this.ang = (short)a;
	}
	public void setAngle(short angle){
		this.ang = angle;
	}
	public void setAngle(int i){ setAngle((short)i); }

	public PointXYZ getLocation(){
		return new PointXYZ(this.x, this.y, this.z);
	}

	@Override
	public PointXY getPoint(){
		return getLocation().asXY();
	}

	/**
	 * @return the sprites angle, as a vector: PointXY
	 */
	@Override
	public PointXY getVector(){
		return AngleUtil.unitVector(getAngle());
	}


	public void setLocation(PointXY point){
		this.x = point.x;
		this.y = point.y;
	}

	// // convenience method
	// public PointXYZ getTransformTo(PointXYZ dest){
	// 	return getLocation().getTransformTo(dest);
	// }

	public void setLocation(PointXYZ point){
		this.x = point.x;
		this.y = point.y;
		this.z = point.z;
	}
	
	public short getSectorId(){
		return this.sectnum;
	}
	void setSectorId(int i){
		this.sectnum = (short)i;
	}
	
	public short getTexture(){
		return this.picnum;
	}
	public short getTex(){
		return getTexture();
	}
	
	public void setTexture(short s){
		this.picnum = s;
	}
	
	public void setTexture(int i){
		this.picnum = (short)i;
	}

	public void setShade(int i){
	    // TODO - throw exception if i is out of range
		this.shade = (short)i;
	}
	
	public void setPal(int pallette){
		this.pal = (short)pallette;
	}
	public int getPal(){
		return this.pal;
	}
	
	public int getLotag(){
		return this.lotag;
	}
	
	public int getHiTag(){
		return this.hitag;
	}

	public void setHiTag(short s){
		this.hitag = s;
	}
	public void setHiTag(int i){ setHiTag((short)i); }
	
	public void setLotag(short s){
		this.lotag = s;
	}
	
	public void setLotag(int i){ setLotag((short)i); }
	
	public short getCstat(){
		return this.cstat;
	}
	
	public void setCstat(short cstat){
		this.cstat = cstat;
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
		
		final String ln = "\n";
		
		StringBuilder sb = new StringBuilder();
		sb.append("{ sprite").append(ln);
		sb.append("x: ").append(x).append(ln);
		sb.append("y: ").append(y).append(ln);
		sb.append("z: ").append(z).append(ln);
		sb.append("cstat: ").append(cstat).append(ln);
		sb.append("picnum: ").append(picnum).append(ln);
		
		sb.append("shade: ").append(shade).append(ln);
		sb.append("pal: ").append(pal).append(ln);
		sb.append("clipdist: ").append(clipdist).append(ln);
		sb.append("filler: ").append(filler).append(ln);
		sb.append("xrepeat: ").append(xrepeat).append(ln);
		sb.append("yrepeat: ").append(yrepeat).append(ln);
		sb.append("xoffset: ").append(xoffset).append(ln);
		sb.append("yoffset: ").append(yoffset).append(ln);
		
		sb.append("sectnum: ").append(sectnum).append(ln);
		sb.append("statnum: ").append(statnum).append(ln);
		sb.append("ang: ").append(ang).append(ln);
		
		sb.append("owner: ").append(owner).append(ln);
		sb.append("xvel: ").append(xvel).append(ln);
		sb.append("yvel: ").append(yvel).append(ln);
		sb.append("zvel: ").append(zvel).append(ln);
		sb.append("lotag: ").append(lotag).append(ln);
		sb.append("hitag: ").append(hitag).append(ln);
		sb.append("extra: ").append(extra).append(ln);
		sb.append("}").append(ln);
		
		
		return sb.toString();
		
	}
	
	public void toBytes(OutputStream output) throws IOException {
		ByteUtil.writeInt32LE(output, x);
		ByteUtil.writeInt32LE(output, y);
		ByteUtil.writeInt32LE(output, z);
		
		ByteUtil.writeInt16LE(output, cstat);
		ByteUtil.writeInt16LE(output, picnum);
		
		ByteUtil.writeUint8(output, shade);
		ByteUtil.writeUint8(output, pal);
		ByteUtil.writeUint8(output, clipdist);
		ByteUtil.writeUint8(output, filler);
		ByteUtil.writeUint8(output, xrepeat);
		ByteUtil.writeUint8(output, yrepeat);
		ByteUtil.writeUint8(output, xoffset);
		ByteUtil.writeUint8(output, yoffset);
		
		ByteUtil.writeInt16LE(output, sectnum);
		ByteUtil.writeInt16LE(output, statnum);
		ByteUtil.writeInt16LE(output, ang);
		
		ByteUtil.writeInt16LE(output, owner);
		ByteUtil.writeInt16LE(output, xvel);
		ByteUtil.writeInt16LE(output, yvel);
		ByteUtil.writeInt16LE(output, zvel);
		ByteUtil.writeInt16LE(output, (short)lotag);
		ByteUtil.writeInt16LE(output, hitag);
		ByteUtil.writeInt16LE(output, extra);
	}
	
	
	public static Sprite readSprite(InputStream input) throws IOException {
		Sprite s = new Sprite();


		// TODO - anything that is supposed to be an usigned short is getting fucked up
		
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
		s.lotag = ByteUtil.readInt16LEasInt(input);
		s.hitag = ByteUtil.readInt16LE(input);
		s.extra = ByteUtil.readInt16LE(input);
		
		
		
		
		return s;
		
	}

	/**
	 * @param p1  one end of the line segment
	 * @param p2  the other send of the line segment (NOT the vector expressing the delta)
	 * @return true if the ray defined by this sprite intersects the line segment (p1, p2)
	 */
	public boolean intersectsSegment(PointXY p1, PointXY p2){
		PointXY sv = AngleUtil.unitVector(this.getAngle());
		return PointXY.raySegmentIntersect(getLocation().asXY(), sv, p1, p2.subtractedBy(p1), false);
	}
	
	

}
