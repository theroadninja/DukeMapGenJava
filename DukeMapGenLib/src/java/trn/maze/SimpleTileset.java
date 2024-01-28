package trn.maze;

public class SimpleTileset {
    public final int wallTexture;
    public final int floorTexture;
    public final int ceilingTexture;

    public SimpleTileset(int wall, int floor, int ceil) {
        this.wallTexture = wall;
        this.floorTexture = floor;
        this.ceilingTexture = ceil;
    }

    @Override
    public String toString() {
        return String.format("{ wall: %d, floor: %d, ceiling: %d", wallTexture, floorTexture, ceilingTexture);
    }
}
