package trn;

public class SpriteStat {

    public static int ALIGN_NORMAL = 0;
    public static int ALIGN_WALL = 16;
    public static int ALIGN_FLOOR = 32;

    // bit 0:  blocking
    // bit 1: t for translucent, 0 for normal
    // bit 2:  x flipped if set
    // bit 3:  y flipped if set
    // bits [54]:
    //         00 = normal
    //         01 = flat standing up
    //         10 = flat on the ground
    // bit 6: one-sided if set (only for wall or floor aligned
    // bit 7:  something about centering (0 means centered at floor?)
    // bit 8: hitscan blocking
    // bit 15: invisible if set

    private final int stat;

    public SpriteStat(int stat){
        this.stat = stat;
    }

    boolean get(int bitval){
        return bitval == (this.stat & bitval);
    }

    public boolean isBlocking(){
        return get(1);  // bit value 1 is the blocking flag
    }

    public boolean isTranslucent(){
        return get(2);
    }

    public boolean isXFlipped(){
        return get(4);
    }

    public boolean isYFlipped(){
        return get(8);
    }

    public int getAlignment(){
        return this.stat & (16|32);
    }

    public boolean isOneSided(){
        return get(64);
    }

    // so if this is set, the sprite looks like its wading through water?
    public boolean isCenteredOnFloor(){
        return get(128);
    }

    public boolean isHitscanBlocking(){
        return get(256);
    }

    public boolean isInvisible(){
        return get(32768);
    }
}
