package trn.prefab

import trn.{LineSegmentXY, MapUtil, PointXY, Wall, WallView, Map => DMap}

import scala.collection.JavaConverters._ // this is the good one
import trn.MapImplicits._

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

  /**
    * @return true if `xy` exists inside any sector of the group
    */
  def contains(xy: PointXY): Boolean = ???

  /**
    * TODO see also MapWriter.spaceAvailable
    *
    * @param other
    * @return
    */
  def intersectsWith(other: SectorGroupBase): Boolean = {
    val coarseIntersect = boundingBox.intersect(other.boundingBox).map(_.area).getOrElse(0) > 0
    lazy val fineIntersect = BoundingBox.nonZeroOverlap(fineBoundingBoxes, other.fineBoundingBoxes)
    lazy val polyIntersect = {
      val wallLoops1 = allSectorIds.toSeq.flatMap(getAllWallLoopsAsViews).map(_.map(_.getLineSegment))
      val wallLoops2 = other.allSectorIds.toSeq.flatMap(other.getAllWallLoopsAsViews).map(_.map(_.getLineSegment))
      Polygon.guessGroupsOverlap(wallLoops1, wallLoops2)
    }
    coarseIntersect && fineIntersect && polyIntersect
  }

  def getAllWallViews: Iterable[WallView] = {
    allSectorIds.flatMap(id => getAllWallLoopsAsViews(id).flatten)
  }

  def getAllWallLoopsAsViews(sectorId: Int): Seq[Seq[WallView]] = {
    map.getAllWallLoopsAsViews(sectorId).asScala.map(_.asScala.toSeq)
  }

  def getOuterWallLoop(sectorId: Int): Seq[WallView] = {
    map.getOuterWallLoop(sectorId)
    // val outerLoops = map.getAllWallLoopsAsViews(sectorId).asScala.filter(MapUtil.isOuterWallLoop)
    // require(outerLoops.size == 1, s"sector ${sectorId} has more than one outer wall loop")
    // outerLoops.head.asScala.toSeq
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