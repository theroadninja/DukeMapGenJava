package trn;

import java.util.*;

// this is public because someone might need the source sector ids
public class CopyState {
    public IdMap idmap = new IdMap();

    List<Integer> wallsToUpdate = new LinkedList<Integer>();
    List<Integer> sectorsToUpdate = new LinkedList<Integer>();

    /** unique hi tags already present in the destination map before copying the sector group */
    //Set<Integer> usedTagsBeforeCopy = new HashSet<Integer>();

    public Set<Short> sourceSectorIds(){
        return idmap.sectorIdMap.keySet();
    }

    public Set<Short> destSectorIds(){
        Set<Short> ids = new TreeSet<Short>();
        ids.addAll(idmap.sectorIdMap.values());
        return ids;
    }
}
