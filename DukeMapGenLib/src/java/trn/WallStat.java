package trn;

/**
 * https://wiki.eduke32.com/wiki/Cstat(wall)
 */
public class WallStat {

    /**
     * Removes bits from an int.
     * AFAIK, java does not support unsigned ints, which is a pain in the ass for bit operations.  I cant find any
     * good information on how to do this stuff, so I'm rigging this together.  This function lets me sort of
     * pretend that ints are unsigned.
     *
     * @param value  the value you want to manipulate
     * @param bitsToRemove an integer with the bits you want to remove (i.e. `4` to remove the third least significant bit)
     * @return the value of `value` except is has zeros where bitsToRemove had ones
     */
    public static int removeBits(int value, int bitsToRemove){
        return value & ~bitsToRemove;
    }

    public static final int BLOCKABLE = 1;
    public static final int BOTTOM_SWAPPED = 2;
    public static final int ALIGN_BOTTOM = 4;
    public static final int XFLIP = 8;
    public static final int MASK_2SIDE = 16; // this is a normal masking wall? (pressing M in build) NOTE: for masked walls, overpicnum is used!
    public static final int MASK_1SIDe = 32; // combine 16 and 32 to disable transparency  (pressing 1 in build)
    public static final int HITSCAN = 64; // wall can be hit by weapons
    public static final int TRANSPARENT = 128;
    public static final int YFLIP = 256;
    public static final int MORE_TRANSPARENT = 512; // combine with 128?

    private final int stat;

    public WallStat(int stat){
        this.stat = stat;
    }

    boolean get(int bitval){
        return bitval == (this.stat & bitval);
    }

    /** @returns the value as a short, as the build engine uses it */
    short cstat(){
        return (short)stat;
    }

    public boolean blockPlayer(){
        return get(BLOCKABLE);
    }

    public boolean alignBottom(){
        return get(ALIGN_BOTTOM);
    }

    public boolean hitscan(){
        return get(HITSCAN);
    }

    public boolean xflip(){
        return get(XFLIP);
    }

    public boolean yflip(){
        return get(YFLIP);
    }

    public WallStat withValueChanged(int whichBit, boolean value){
        if(value){
            return new WallStat(this.stat | whichBit);
        }else{
            return new WallStat(removeBits(this.stat, whichBit));
        }
    }

    public WallStat withAlignBottom(boolean value){
        return withValueChanged(ALIGN_BOTTOM, value);
    }

}
