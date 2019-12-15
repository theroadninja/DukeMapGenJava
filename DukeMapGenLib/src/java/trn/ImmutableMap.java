package trn;

/**
 * Read-only version of a Build Engine Map object.
 */
public class ImmutableMap implements WallContainer {

    private final Map map;

    public ImmutableMap(Map map){
        this.map = map;
    }

    @Override
    public Wall getWall(int i){
        return map.getWall(i);
    }

    public Sector getSector(int sectorId){
        return map.getSector(sectorId).copy();
    }
}
