package trn;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;

public class Sector {
	
	public static final int DEFAULT_FLOOR_Z = 8192;
	public static final int DEFAULT_CEILING_Z = -8192;

	private short firstWall; //a.k.a. wallprt
	
	/**
	 * NOTE:  i think this is necessary to know if the sector
	 * 	has more than 1 wall loop!
	 */
	private short wallCount; //a.k.a. wallnum
	
	
	private int ceilingz; //z coord of ceiling at first point in sector
	private int floorz; //z coord of floor at first point of sector
	
	
	private short ceilingStat;

	/**
	 * bit 0: 1 = parallaxing, 0 = not [P]
	 * bit 1: 1 = sloped, 0 = not
	 * bit 2: 1 = swap x&y, 0 = not   [F]
	 * bit 3: double smooshiness (make it smaller)  [E]
	 * bit 4: x-flip [F]
	 * bit 5: y-flip [F]
	 * bit 6: align texture to first wall of sector [R]
	 */
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
	
	
	/*
	 * Shading notes:
	 * 32 appears to be completely black, althought it might not be
	 */
	
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
		
		this.ceilingz = DEFAULT_CEILING_Z;
		this.floorz = DEFAULT_FLOOR_Z;
	}
	
	public Sector copy(){
		Sector sector = new Sector();
		sector.firstWall = this.firstWall;
		sector.wallCount = this.wallCount;
		sector.ceilingz = this.ceilingz;
		sector.floorz = this.floorz;
		sector.ceilingStat = this.ceilingStat;
		sector.floorstat = this.floorstat;
		sector.ceilingPicNum = this.ceilingPicNum;
		sector.ceilingheinum = this.ceilingheinum;
		sector.ceilingshade = this.ceilingshade;
		sector.ceilingpal = this.ceilingpal;
		sector.ceilingxpanning = this.ceilingxpanning;
		sector.ceilingypanning = this.ceilingypanning;
		sector.floorpicnum = this.floorpicnum;
		sector.floorheinum = this.floorheinum;
		sector.floorshade = this.floorshade;
		sector.floorpal = this.floorpal;
		sector.floorxpanning = this.floorxpanning;
		sector.floorypanning = this.floorypanning;
		sector.visibility = this.visibility;
		sector.filler = this.filler;
		sector.lotag = this.lotag;
		sector.hitag = this.hitag;
		sector.extra = this.extra;
		return sector;
	}
	
	public void translateIds(final IdMap idmap){
		this.firstWall = idmap.wall(this.firstWall);
	}
	
	public Sector translate(final PointXYZ delta){
		this.floorz += delta.z;
		this.ceilingz += delta.z;
		return this;
	}

	/**
	 * This is called when walls in the map are added/removed, to update the integer "pointers" to other walls.
	 * @param startIndex the first wallId that is affected.  Any wall ids >= startIndex will be modified
	 * @param delta how to shift the indexes.  Negative shifts them down, positive shifts them up.
	 * @return
	 */
	void shiftWallPointers(int startIndex, int delta){
		if(startIndex < 1 || startIndex + delta < 0){
			throw new IllegalArgumentException(String.format("invalid args startIndex=%s delta=%s", startIndex, delta));
		}
		if(this.firstWall >= startIndex){
			this.firstWall += delta;
		}
	}

	@Override
	public String toString(){
		String ln = "\n";
		StringBuilder sb = new StringBuilder();
		sb.append("{ sector \n").append(ln);
		sb.append("first wall: " + this.firstWall).append(ln);
		sb.append("}").append(ln);
		return sb.toString();
	}


	public void setFloorXPan(int xpan){
		this.floorxpanning = (short)xpan;
	}
	public void setFloorYPan(int ypan){
		this.floorypanning = (short)ypan;
	}

	public void setCeilingXPan(int xpan){
		this.ceilingxpanning = (short)xpan;
	}
	public void setCeilingYPan(int ypan){
		this.ceilingypanning = (short)ypan;
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

	public void setFloorParallax(boolean parallax){
		if(parallax){
			this.floorstat |= FloorCeilStat.PARALLAX;
		}else{
			throw new RuntimeException("Not Implemented Yet");
		}
	}

	public void setFloorSloped(boolean sloped){
		if(sloped){
			this.floorstat |= FloorCeilStat.SLOPED;
		}else{
			throw new RuntimeException("Not Implemented Yet");
		}
	}

	public void setFloorRelative(boolean relative){
	    // TODO make a FloorState object just like WallStat
		if(relative){
			this.floorstat |= FloorCeilStat.RELATIVE;
		}else{
		    // this.floorstat = WallState.removebits(this.floorstate, FloorCeilStat.RELATIVE)
			throw new RuntimeException("Not Implemented Yet");
		}
	}

	public void setFloorSwapXY(boolean swap){
		if(swap){
			this.floorstat |= FloorCeilStat.SWAPXY;
		}else{
			throw new RuntimeException("Not Implemented Yet");
		}
	}

	public void setFloorSmaller(boolean smaller){
		if(smaller){
			this.floorstat |= FloorCeilStat.SMALLER;
		}else{
			throw new RuntimeException("Not Implemented Yet");
		}
	}

	public void setFloorYFlip(boolean yflip){
		if(yflip){
			this.floorstat |= FloorCeilStat.YFLIP;
		}else{
			throw new RuntimeException("Not Implemented Yet");
		}
	}

	/**
	 * @return the z coord of the floor at the first wall only.  If the sector is sloped you need to use
	 * MapUtils.getFloorZAt() to get the correct height for any point not on the sector's first wall.
	 */
	public int getFloorZ(){
		return this.floorz;
	}

	public void setCeilingTexture(int i){
		this.ceilingPicNum = (short)i;
	}
	public short getCeilingTexture(){
		return this.ceilingPicNum;
	}

	public void setCeilingParallax(boolean parallax){
		if(parallax){
			this.ceilingStat |= FloorCeilStat.PARALLAX;
		}else{
			throw new RuntimeException("Not Implemented Yet");
		}
	}

	public void setCeilingSloped(boolean sloped){
		if(sloped){
			this.ceilingStat |= FloorCeilStat.SLOPED;
		}else{
			throw new RuntimeException("Not Implemented Yet");
		}
	}

	public void setCeilingRelative(boolean relative){
		if(relative){
			this.ceilingStat |= FloorCeilStat.RELATIVE;
		}else{
			throw new RuntimeException("Not Implemented Yet");
		}
	}

	public void setCeilingSwapXY(boolean swap){
		if(swap){
			this.ceilingStat |= FloorCeilStat.SWAPXY;
		}else{
			throw new RuntimeException("Not Implemented Yet");
		}
	}

	public void setCeilingSmaller(boolean smaller){
		if(smaller){
			this.ceilingStat |= FloorCeilStat.SMALLER;
		}else{
			throw new RuntimeException("Not Implemented Yet");
		}
	}

	// TODO: should have a single method like: setSomething(Floor vs Ceiling, bit, value)
	public void setCeilingYFlip(boolean yflip){
		if(yflip){
			this.floorstat |= FloorCeilStat.YFLIP;
		}else{
			throw new RuntimeException("Not Implemented Yet");
		}
	}

	public void setFloorTexture(int i){
		this.floorpicnum = (short)i;
	}
	public short getFloorTexture(){
		return this.floorpicnum;
	}

	/**
	 * TODO explanation of how the value works...  rise/run, 0 = flat and 4096 = 45 degrees...
	 * @return
	 */
	public int getFloorSlope(){
		return this.floorheinum;
	}
	public void setFloorSlope(int slope){
		this.floorheinum = (short)slope;
	}

	public int getCeilingSlope(){
		return this.ceilingheinum;
	}
	public void setCeilingSlope(int slope){
		this.ceilingheinum = (short)slope;
	}

	public CFStat getFloorStat(){
		return new CFStat(this.floorstat);
	}
	public int getFloorXPan(){
		return this.floorxpanning;
	}
	public int getFloorYPan(){
		return this.floorypanning;
	}

	public short getCeilingShadeAsUnsigned(){
		return this.ceilingshade;
	}
	
	public void setCeilingShade(short unsigned){
		this.ceilingshade = unsigned;
	}
	
	public short getFloorShadeUnsigned(){
		return this.floorshade;
	}
	
	public void setFloorShade(short unsigned){
		this.floorshade = unsigned;
	}

	public short getCeilingPallette(){
		return this.ceilingpal;
	}
	public void setCeilingPalette(int i){
		this.ceilingpal = (short)i;
	}

	public void setFloorPalette(int i){
		this.floorpal = (short)i;
	}
	
	public short getFirstWall(){
		return this.firstWall;
	}


	// TODO - is this wrong?  is it relative?
	public void setFirstWall(int wallId){
		this.firstWall = (short)wallId;
	}
	
	public short getWallCount(){
		return this.wallCount;
	}
	void setWallCount(int wallCount){
		this.wallCount = (short)wallCount;
	}
	
	public void setLotag(int lotag){
		this.lotag = (short)lotag;
	}

	public void setHitag(int hitag){
		this.hitag = (short)hitag;
	}

	public int getLotag(){
		return this.lotag;
	}

	/**
     * Calculates the height of a sector's floor or ceiling at a certain distance from the first wall,
	 * based on the sector floor/ceiling slope angle.o
	 *
	 * The slope angle in a value like `floorheinum` is rise/run, where 0 = flat and 4096 is 45 degrees.
     *
	 *                   (x=0,z=0) --------------------> (x=distance,z=0)
	 *                            .
	 *                               .
	 *                                  .
	 *                                     .
	 *                                        .
	 *                                           .
	 *                                              .  (x=distance, z=RETURN VALUE)
	 *
	 * @param slope the slope, in the same units as `floorheinum`, a.k.s. Sector.getFloorSlope()
	 * @param distanceFromFirstWall the horizontal distance from the first wall, in the coordinate scale used by x and y coordinates
	 *         Note:  use a negative distance to indicate a place "behind" the first wall of a sector
	 * @return  the height of the floor (starting from 0), in z units (which are a different scale than x and y units) at distance `distance`
	 * 		from the first wall of a sector
	 */
	public static int getSlopedZ(int slope, int distanceFromFirstWall){
	    int slope2 = slope / 256; // I have no idea why the slope value is bitshifted also
	    return distanceFromFirstWall * slope2;
	}

	// TODO good documentation, and put this function at the correct place
	public int getFloorZAt(int distanceFromFirstWall){
		return getSlopedZ(this.floorheinum, distanceFromFirstWall);
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
		
		System.out.println("{");
		
		
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
		System.out.println("}");
	}
}
