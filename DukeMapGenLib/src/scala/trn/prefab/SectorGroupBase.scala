package trn.prefab

import trn.{Wall, Map => DMap}
import scala.collection.JavaConverters._ // this is the good one

/**
  * For code that is shared between SectorGroups and PastedSectorGroups.
  */
trait SectorGroupBase {
  def map: DMap

  protected def wallSeq(): Seq[Wall]
  protected def allSectorIds: Set[Int]

  /**
    * @return a singe large bounding box that covers that entire sector group
    */
  def boundingBox: BoundingBox = { // TODO make val, but after PastedSectorGroup migrated to scala
    val walls = wallSeq
    SectorGroupBase.toBoundingBox(walls)
    // val w = walls(0)
    // walls.foldLeft(BoundingBox(w.getX, w.getY, w.getX, w.getY)){ (acc, wall) =>
    //   acc.add(wall.getX, wall.getY)
    // }
  }

  private var boundingBoxes: Option[Seq[BoundingBox]] = None // TODO - make this val after PastedSectorGroup to scala
  def fineBoundingBoxes: Seq[BoundingBox] = {
    if(!boundingBoxes.isDefined){
      val wallGroups = allSectorIds.map(map.getSector).map { sector =>
        map.getAllSectorWallIds(sector).asScala.map(map.getWall(_))
      }.toSeq
      // TODO - this is very inefficient.  should at least see if bounding boxes are inside each other!
      val bbs = wallGroups.map(SectorGroupBase.toBoundingBox(_))
      //boundingBoxes = Some(bbs)
      boundingBoxes = Some(bbs.foldLeft(Seq.empty[BoundingBox])(BoundingBox.merge))
    }
    boundingBoxes.get
  }

}

object SectorGroupBase {
  def toBoundingBox(walls: Seq[Wall]): BoundingBox = {
    val w = walls(0)
    walls.foldLeft(BoundingBox(w.getX, w.getY, w.getX, w.getY)){ (acc, wall) =>
      acc.add(wall.getX, wall.getY)
    }
  }
}