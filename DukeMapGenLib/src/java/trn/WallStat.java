package trn;

/**
 * https://wiki.eduke32.com/wiki/Cstat(wall)
 */
public class WallStat {

    public static final int BLOCKABLE = 1;
    public static final int BOTTOM_SWAPPED = 2;
    public static final int ALIGN_BOTTOM = 4;
    public static final int XFLIP = 8;
    public static final int MASK_2SIDE = 16;
    public static final int MASK_1SIDe = 32; // combine 16 and 32 to disable transparency
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

    public boolean blockPlayer(){
        return get(BLOCKABLE);
    }
}
