package trn;

/**
 * For those times when you really want to use floating point types, like interpolation.
 *
 * NOTE: this is probably going to be mostly used as a vector
 */
public final class FVectorXY {
    public static FVectorXY ZERO = new FVectorXY(0, 0);

    public final double x;
    public final double y;

    public FVectorXY(double x, double y){
        this.x = x;
        this.y = y;
    }

    public PointXY toPointXY(){
        return new PointXY((int)Math.round(x), (int)Math.round(y));
    }

    public double length(){
        return Math.sqrt(x*x + y*y);
    }

    public FVectorXY normalized() {
        double d = length();
        if(d == 0.0){
            throw new RuntimeException("cannot normalize a zero length vector");
        }
        return new FVectorXY(x/d, y/d);
    }

    public FVectorXY scaled(double scale) {
        return new FVectorXY(x * scale, y * scale);
    }

    public FVectorXY rotatedCW(){
        return new FVectorXY(-y, x);
    }

    public FVectorXY rotatedCCW(){
        return new FVectorXY(y, -x);
    }


}
