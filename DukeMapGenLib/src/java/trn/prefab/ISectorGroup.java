package trn.prefab;

import trn.Map;
import trn.Sprite;

import java.util.List;

public interface ISectorGroup {
    /**
     * @returns the underlying map that stores the data in this sector group.
     *
     * this one is also used by the elevator connectors
     */
    Map getMap();

    // this is used by connectors, which I guess don't want to care about SectorGroup vs PastedSectorGroup
    List<Sprite> findSprites(int picnum, int lotag, int sectorId);

}
