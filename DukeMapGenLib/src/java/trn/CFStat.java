package trn;

/**
 * Low level representation of ceiling/floor flags.
 *
 * Useful reference to map this to the editor keys:
 *     http://infosuite.duke4.net/index.php?page=references_build_keys
 *     https://wiki.eduke32.com/wiki/Build/Mapster32_Keyboard_Commands
 */
public class CFStat {

    public static final int PARALLAXING = 1;
    public static final int SLOPED = 2;
    public static final int SWAPXY = 4;
    public static final int DOUBLE_SMOOSHINESS = 8;
    public static final int XFLIP = 16;
    public static final int YFLIP = 32;
    public static final int ALIGN_TEX_FIRSTWALL = 64;
    // rest of the bits are reserved

    private final int stat;

    public CFStat(int stat){
        this.stat = stat;
    }

    /**
     * @param bitval the VALUE of 1 in the bit to read, e.g.:
     *               bit 0: 1
     *               bit 1: 2
     *               bit 2: 4
     *               bit 3: 8
     *               ...
     * @return True IFF that bit is set
     */
    boolean get(int bitval){
        return bitval == (this.stat & bitval);
    }

    public boolean parallaxing(){ return get(PARALLAXING); }
    public boolean sloped(){ return get(SLOPED); }

    /**
     * One of the options when pressing 'F' on floor/ceiling texture?
     * @return
     */
    public boolean swapxy(){ return get(SWAPXY); }

    /**
     * Double the scaling of texture (pressing 'E' in the build editor).
     * A.k.a. "texture expansion."
     * @return True if texture is scaled to fit twice as much in the same area
     */
    public boolean doubleSmooshiness(){ return get(DOUBLE_SMOOSHINESS); }

    public boolean xflip(){ return get(XFLIP); }
    public boolean yflip(){ return get(YFLIP); }

    /**
     * Texture is aligned to first wall instead of global (pressing 'R' in the build editor).
     * Use this feature to prevent your floors/ceilings from becoming mis-aligned on translation.
     * @return True if the floor texture is aligned to the first wall
     */
    public boolean alighTexFirstwall(){ return get(ALIGN_TEX_FIRSTWALL); }

    @Override
    public String toString(){
        return ""+stat;
    }
}
