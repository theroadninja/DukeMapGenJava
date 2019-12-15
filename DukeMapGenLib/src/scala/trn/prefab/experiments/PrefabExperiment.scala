package trn.prefab.experiments
import trn.{DukeConstants, Main, MapLoader, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, Map => DMap}

trait PrefabExperiment {
  def Filename: String

  def run(mapLoader: MapLoader): DMap
}
