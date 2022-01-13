package trn.prefab;

import trn.Map;
import trn.Sprite;

import java.util.List;

// TODO !!! This should be implemented ONLY by SectorGroup, and SgMapBuilder  (NOT PastedSectorGroups)
public interface ISectorGroup {
    /**
     * @returns the underlying map that stores the data in this sector group.
     */
    Map getMap();

    List<Sprite> findSprites(int picnum, int lotag, int sectorId);

    // List<Sprite> findSprites(ISpriteFilter... filters);
}
