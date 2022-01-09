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
        return new FVectorXY(-y, x); // remember the math is weird because y+ is south
    }

    public FVectorXY rotatedCCW(){
        return new FVectorXY(y, -x);
    }

    public FVectorXY add(FVectorXY p){
        return new FVectorXY(x + p.x, y + p.y);
    }

    public FVectorXY subtractedBy(FVectorXY other){
        return new FVectorXY(this.x - other.x, this.y - other.y);
    }

    public FVectorXY multipliedBy(double f){
        return new FVectorXY(x * f, y * f);
    }

    public double dotProduct(PointXY other){
        return this.x * other.x + this.y * other.y;
    }

    public double dotProduct(FVectorXY other){
        return this.x * other.x + this.y * other.y;
    }

    @Override
    public String toString(){
        return "{ FVectorXY x=" + this.x + " y=" + y + " }";
    }


}
