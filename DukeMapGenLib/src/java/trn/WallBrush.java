package trn;

/**
 * NOTE:  this might not be necessary once I claw WallBrush out of the Wall and Map objects...in which case
 * a "WallBrush" might not need to be in this layer at all (the java lib).
 */
public interface WallBrush {

    void writeToWall(Wall wall);
}
