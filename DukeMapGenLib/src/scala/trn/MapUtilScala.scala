package trn

import trn.{Map => DMap}
import trn.MapImplicits._
import trn.prefab.GameConfig
import scala.collection.JavaConverters._

import scala.collection.mutable.ArrayBuffer

/**
  * For functions that belong in MapUtil but I don't feel like writing in Java.
  */
object MapUtilScala {

  def usedUniqueTags(cfg: GameConfig, map: DMap): Set[Int] = {
    // TODO
    Set.empty
    //val tags = map.allSprites.flatMap(s => cfg.uniqueTags(s)) ++ map.allWalls.flatMap(w => cfg.uniqueTags(w))
    //tags.toSet
  }

  def usedUniqueTagsAsJava(cfg: GameConfig, map: DMap): java.util.Set[Integer] = {
    usedUniqueTags(cfg, map).map(Integer.valueOf).asJava
  }


}
