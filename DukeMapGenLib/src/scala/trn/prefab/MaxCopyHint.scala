package trn.prefab

import trn.FuncUtils

import scala.collection.JavaConverters._

object MaxCopyHint {

  def fromPalette(palette: PrefabPalette): MaxCopyHint = apply(palette.allSectorGroups.asScala)

  def apply(sectorGroups: Iterable[SectorGroup]): MaxCopyHint = {
    val maxCopies = sectorGroups.map(sg => sg.groupIdOpt -> sg.hints.maxCopies).collect {
      case (Some(groupId), Some(maxCopies)) => groupId -> maxCopies
    }.toMap
    new MaxCopyHint(maxCopies)
  }

}

// NOTE: right now this does not work for anonymous sector groups
/**
  * This object exists at the scope of the entire input map; it tracks the maximum number of copies for every sector
  * group.
  * @param maxCopies
  */
class MaxCopyHint(maxCopies: Map[Int, Int]) {

  def assertMaxCopies(groups: Iterable[SectorGroup]): Unit = {
    countGroups(groups).foreach { case (groupId, copies) =>
      maxCopies.get(groupId).foreach { maxCopy =>
        if(copies > maxCopy){
          val msg = s"Error: sector group ${groupId} should have at most ${maxCopy} copies but there are ${copies}"
          throw new RuntimeException(msg)
        }
      }
    }
  }

  /**
    * counts the number of sector groups that have the same id.  Note: normally when counting groups you will probably
    * be dealing with PastedSectorGroups; this is only useful when you are collecting sector groups somewhere before
    * pasting them all at once.
    */
  def countGroups(groups: Iterable[SectorGroup]): Map[Int, Int] = groups.flatMap(_.groupIdOpt).groupBy(i => i).map{
    case (k, items) => k -> items.size
  }
}
