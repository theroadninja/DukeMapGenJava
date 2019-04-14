package trn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import trn.duke.MapErrorException;

public class Wall {
	
	public static final class CSTAT_FLAGS {
		
		public static final int BIT_2_ALIGN_TEX_ON_BOTTOM = 4;
	}
	
	int x; //INT32LE
	int y; //INT32LE
	
	//apparently this is the actual next wall
	short point2; //INT16LE 
	
	
	// other "other" wall with a red wall ...
	short nextWall; //INT16LE ... -1 means no sector on other side of wall
		//think this is for red (two-sided) walls

	public boolean isRedWall(){
		return nextWall != -1;
	}
	
	// TODO - make this not public
	public short nextSector; //INT16LE ... -1 means no sector on other side of wall
	
	short cstat; //INT16LE
	// bit 0: B (blocking wall)
	// bit 1: 2
	// bit 2: O
	// bit 3: F
	// bit 4: M (masked wall)
	// bit 5: 1 (1-way wall)
	// bit 6: H  (another blocking wall)
	// bit 7: T
	// bit 8: F
	//bit 9: T
	
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
	
	public Wall(int x, int y){
		this(x, y, 0);
	}
	
	public Wall(PointXY xy, WallPrefab spec){
		this(xy.x, xy.y);
		
		if(spec != null){
			spec.writeTo(this);
		}
	}
	
	
	public Wall(int x, int y, int wallTex){
		this(x, y, -1, wallTex);
	}
	
	
	public Wall(PointXY xy, int wallTex, int xrepeat, int yrepeat){
		this(xy.x, xy.y, wallTex, xrepeat, yrepeat);
	}
	
	public Wall(int x, int y, int wallTex, int xrepeat, int yrepeat){
		this(x, y, -1, wallTex);
		this.xrepeat = (short)xrepeat;
		this.yrepeat = (short)yrepeat;
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
	
	public Wall copy(){
		Wall w = new Wall();
		w.x = this.x;
		w.y = this.y;
		w.point2 = this.point2;
		w.nextWall = this.nextWall;
		w.nextSector = this.nextSector;
		w.cstat = this.cstat;
		w.picnum = this.picnum;
		w.overpicnum = this.overpicnum;
		w.shade = this.shade;
		w.pal = this.pal;
		w.xrepeat = this.xrepeat;
		w.yrepeat = this.yrepeat;
		w.xpanning = this.xpanning;
		w.ypanning = this.ypanning;
		w.lotag = this.lotag;
		w.hitag = this.hitag;
		w.extra = this.extra;
		return w;
	}
	
	/**
	 * 
	 * @param idmap
	 * @param wallsOnly - cheap debug param to skip translating sectors
	 */
	public void translateIds(final IdMap idmap, final boolean wallsOnly){
		this.point2 = idmap.wall(this.point2);
		if(this.nextWall != -1){
			this.nextWall = idmap.wall(this.nextWall);
		}
		if(this.nextSector != -1 && ! wallsOnly){
			this.nextSector = idmap.sector(this.nextSector);
		}
	}
	
	public Wall translate(final PointXYZ delta){
		this.x += delta.x;
		this.y += delta.y;
		return this;
	}
	
	public int getX(){
		return x;
	}
	
	public int getY(){
		return y;
	}

	/**
	 * @returns the X,Y coordinates of the first point of this wall as a PointXY.  Note this is similar to the
	 * Sprite.getLocation() method except that it lacks a z coordinate because walls do not have z coordinates.
	 */
	public PointXY getLocation(){
		return new PointXY(getX(), getY());
	}

	public void setLocation(PointXY p){
		if(p == null) throw new IllegalArgumentException();
		this.x = p.x;
		this.y = p.y;
	}

	/**
	 * @param point2
	 * @return the vector indicating the direction from point 1 (this wall)
	 * 		to point 2 (the next wall in the wall loop).  According to
	 * 	    http://fabiensanglard.net/duke3d/BUILDINF.TXT  this next wall
	 * 	    is always "to the right" I assume from a point of view inside
	 * 	    the sector.  Which would mean walls are defined clockwise.
	 *
	 *
	 * 	    NOTE:  this appears to be wrong!!  I think walls might be counter clockwise
	 * 	    See the SimpleConnector constructor (or wherever the code is that guesses
	 * 	    connector types).  Based on experimentation, these can go either way
	 * 	    CORRECTON:  ignore note...maybe i just suck at writing unit vectors
	 */
	public PointXY getUnitVector(Wall point2){
		int dx = point2.x - x;
		int dy = point2.y - y;
		if(dx == 0 && dy == 0){
			throw new RuntimeException("invalid vector");
		}else if(dx == 0){
			dy = (dy > 0) ? 1 : -1;
			return new PointXY(0, dy);
		}else if(dy == 0){
			dx = (dx > 0) ? 1 : -1;
			return new PointXY(dx, 0);
		}else{
			double magnitude = Math.sqrt(dx*dx + dy*dy);
			return new PointXY((int)(dx/magnitude), (int)(dy/magnitude));
		}
	}
	
	public boolean sameXY(Wall rh){
		return rh != null && x == rh.x && y == rh.y;
	}
	
	/**
	 * sets the next wall in the loop.  calling this 'setPoint2Id' because the field 'nextWall' is already
	 * take up by another field, which refers to the wall on the other side of this wall
	 * @param point2
	 */
	public void setPoint2Id(int point2){
		this.point2 = (short)point2;
	}
	
	public int getPoint2Id(){
		return this.point2;
	}

	public int getNextWallInLoop(){
		return getPoint2Id();
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
	
	public Wall addCstat(int flag){
		this.cstat |= flag;
		return this;
	}

	public void setPal(int i){
		// TODO - throw if i is out of range
		this.pal = (short)i;
	}
	
	public void setXRepeat(short s){
		this.xrepeat = s;
	}
	public void setXRepeat(int i){ setXRepeat((short)i); }
	
	public void setYRepeat(short yr){
		this.yrepeat = yr;
	}
	public void setYRepeat(int i){ setYRepeat((short)i); }
	
	public void setTexture(int texture, int xr, int yr){
		this.setTexture(texture);
		this.setXRepeat((short)xr);
		this.setYRepeat((short)yr);
	}
	
	public void setShade(short shade){
		this.shade = shade;
	}
	
	public short getLotag(){
		return this.lotag;
	}

	public void setLotag(int i){
		this.lotag = (short)i;
	}
	
	
	
	//TODO:  need an optional param to hide fields with default values
	@Override
	public String toString(){
		return toString(null);
	}
	
	public String toString(Integer wallId){
		String ln = "\n"; //why isn't there an appenln() ?
		
		StringBuilder sb = new StringBuilder();
		sb.append("{ wall").append("\n");
		if(wallId != null){
			sb.append("(id): ").append(wallId).append(ln);
		}
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
	
	
	
	
	
	public void toBytes(OutputStream output) throws IOException, MapErrorException {
	
		if(point2 == -1){
			throw new MapErrorException("cannot serialize; point2 is -1");
		}
		
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
	
	
	
	public static Wall[] createLoop(PointXY[] points, WallPrefab spec){
		Wall[] walls = new Wall[points.length];
		
		for(int i = 0; i < points.length; ++i){
			walls[i] = new Wall(points[i], spec);
			
		}
		
		return walls;
	}
	
	
	public static Wall[] createLoop(PointXY[] points, int wallTex, int xrepeat, int yrepeat){
		Wall[] walls = new Wall[points.length];
		
		for(int i = 0; i < points.length; ++i){
			walls[i] = new Wall(points[i], wallTex, xrepeat, yrepeat);
		}
		
		return walls;
	}

}
