package trn;

public class SpriteStat {

    private final int stat;

    public SpriteStat(int stat){
        this.stat = stat;
    }

    boolean get(int bitval){
        return bitval == (this.stat & bitval);
    }

    public boolean isBlocking(){
        return get(1);  // bit 1 is the blocking flag (there is no bit zero...)
    }
}
