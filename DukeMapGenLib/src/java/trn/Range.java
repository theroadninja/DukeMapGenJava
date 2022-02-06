package trn;

/**
 * Defines a range of integers.  Made this because I don't want Map.java to depend on Apache Commons just for it's
 * Range class.
 */
public class Range {

    /** starting number, inclusive */
    final int start;

    /** ending number, exclusive */
    final int end;

    public Range(int start, int end){
        if(start > end){
            throw new IllegalArgumentException("start must be >= end");
        }
        this.start = start;
        this.end = end;
    }

    /**
     * create a range that includes both numbers.  They can be specified in either order.
     */
    public static Range inclusive(int start, int end){
        if(start < end){
            return new Range(start, end + 1);
        }else{
            return new Range(end, start + 1);
        }
    }

    /**
     * @return true if `i` is in the range
     */
    public boolean contains(int i){
        return start <= i && i < end;
    }

    public boolean containsAny(int... i){
        if(i.length < 1){
            throw new IllegalArgumentException();
        }
        for(int ii: i){
            if(this.contains(ii)){
                return true;
            }
        }
        return false;
    }

    @Override
    public String toString(){
        return String.format("[%s,%s)", this.start, this.end);
    }
}
