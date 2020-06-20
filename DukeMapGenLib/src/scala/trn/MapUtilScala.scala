package trn

import trn.{Map => DMap}
import trn.MapImplicits._
import trn.prefab.{GameConfig, SpriteLogicException}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer
import trn.FuncImplicits._

/**
  * For functions that belong in MapUtil but I don't feel like writing in Java.
  *
  * TODO: this is in trn package, but depends on GameConfig which is in trn.prefab
  */
object MapUtilScala {

  def usedUniqueTags(cfg: GameConfig, map: DMap): Set[Int] = {
    require(Option(cfg).isDefined && Option(map).isDefined)
    val tags = map.allSprites.flatMap(s => cfg.uniqueTags(s)) ++ map.allWalls.flatMap(w => cfg.uniqueTags(w))
    tags.toSet
  }

  def usedUniqueTagsAsJava(cfg: GameConfig, map: DMap): java.util.Set[Integer] = {
    usedUniqueTags(cfg, map).map(Integer.valueOf).asJava
  }

  /**
    * Starting at index `start`, scans forward (increasing integer values) until if finds an integer i such that
    * (i, i+1, i+2, ..., i+gapSize-1) is NOT in the set `tags`
    * @param tags set of tags that already exist
    * @param start index to start (inclusive)
    * @param gapSize size of the "gap" that we can find (consecutive ints that are not in `tags`)
    * @return starting index of the gap
    */
  private[trn] def findGap(tags: Set[Int], start: Int, gapSize: Int): Int = {
    require(start >= 0 && gapSize > 0)
    // in scala, break statements are discouraged to promote ugly code
    var i = start
    while(i + gapSize <= BuildConstants.MaxTagValue){
      var j = 0
      var jbreak = false
      while(j < gapSize && !jbreak){
        if(tags.contains(i+j)){
          jbreak = true
        }else{
          j += 1
        }
      } // jwhile
      if(! jbreak){
        return i
      }else{
        i += j + 1
      }
    } //iwhile
    throw new SpriteLogicException("ran out of unique tag values")
  }

  /**
    * Counts integers, starting at `start`, until it has found `count` integers that are not in start, and
    * returns them.
    * @param tags
    * @param start
    * @param count
    * @return
    */
  private[trn] def findSingleGaps(tags: Set[Int], start: Int, count: Int): Seq[Int] = {
    require(start >= 0)
    val results = mutable.ArrayBuffer[Int]()
    var i = start
    while(results.size < count){
      if(!tags.contains(i)){
        results.append(i)
      }
      i += 1
    }
    results
  }


  def getUniqueTagCopyMap(
    cfg: GameConfig,
    sourceMap: DMap,
    destMap: DMap,
    copyState: CopyState
  ): java.util.Map[Integer, Integer] = {
    val sourceSectorIds: Set[Int] = copyState.sourceSectorIds.asScala.map(_.toInt).toSet
    val sourceSprites: Set[Sprite] = sourceMap.allSprites.filter(s => sourceSectorIds.contains(s.getSectorId)).toSet
    val sourceWalls: Set[Wall] = sourceSectorIds.flatMap(sourceMap.allWallsInSector)
    val usedUniqueDestTags = usedUniqueTags(cfg, destMap)

    val results = getUniqueTagCopyMap(cfg, sourceSprites, sourceWalls, usedUniqueDestTags)
    results.map{case (k, v) => (Integer.valueOf(k) -> Integer.valueOf(v))}.asJava
  }

  /**
    * Creates a map of unique tag values in the source map, to their new values in the destination map.
    *
    * Note: this is stable/deterministic because it sorts
    */
  def getUniqueTagCopyMap(
    cfg: GameConfig,
    sourceSprites: Set[Sprite],
    sourceWalls: Set[Wall],
    usedUniqueDestTags: Set[Int],
  ): scala.collection.immutable.Map[Int, Int] = {

    // dont want to start at 0, because that might conflic with SEs that haven't been set up yet
    val MinTag = 1

    // it is possible that the ranges of multiswitches/two-way trains overlap each other (which is probably invalid)
    // and we dont want to deal with them. so compress them and deal with all of them as a block.  We shouldnt need
    // to worry about someone only partially implementing a multiswitch because groupedUniqueTags() is expected to
    // return all possible tags for a multiswitch/two-way train, even if they are not all used.
    val sourceTagGroups: Set[Int] = sourceSprites.flatMap(s => cfg.groupedUniqueTags(s))
    val groupDestIds: Seq[Int] = if(sourceTagGroups.size > 0){
      val start = findGap(usedUniqueDestTags, MinTag, sourceTagGroups.size)
      (start until start + sourceTagGroups.size)
    }else{
      Seq.empty
    }
    val x = sourceTagGroups.toSeq.sorted.zip(groupDestIds).toMap

    val sourceTagsSingles = (sourceSprites.flatMap(cfg.uniqueTags) ++ sourceWalls.flatMap(cfg.uniqueTags)) -- sourceTagGroups
    //val start = x.values.toSeq.maxOption.map(i => i + 1).getOrElse(0) // No, there could be smaller gaps earlier
    val start = MinTag
    val singleDestIds = findSingleGaps(usedUniqueDestTags ++ groupDestIds, start, sourceTagsSingles.size)
    val y = sourceTagsSingles.toSeq.sorted.zip(singleDestIds).toMap
    (x ++ y)
  }


}
