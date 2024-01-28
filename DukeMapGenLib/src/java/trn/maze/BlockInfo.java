package trn.maze;

public class BlockInfo {
    SimpleTileset tileset;
    public Integer floorZ = null;

    public BlockInfo() {
    }

    public BlockInfo(SimpleTileset s) {
        this.tileset = s;
    }

    public SimpleTileset getTileset() {
        return this.tileset;
    }
}
