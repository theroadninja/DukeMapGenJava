package trn.prefab;

import trn.IdMap;
import trn.MapView;
import trn.PointXYZ;

public interface HasTags<T extends HasTags> {

    public T translateIds(IdMap idmap, PointXYZ delta, MapView map);
}
