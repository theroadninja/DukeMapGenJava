package trn.prefab

import trn.{Wall, Map => DMap}

/**
  * For code that is shared between SectorGroups and PastedSectorGroups.
  */
trait SectorGroupBase {
  def map: DMap

  protected def wallSeq(): Seq[Wall]

  def boundingBox: BoundingBox = { // TODO make val, but after PastedSectorGroup migrated to scala
    val walls = wallSeq
    val w = walls(0)
    walls.foldLeft(BoundingBox(w.getX, w.getY, w.getX, w.getY)){ (acc, wall) =>
      acc.add(wall.getX, wall.getY)
    }
  }
}
