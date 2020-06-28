package trn.prefab

import trn.PointXY

/**
  * Packs unrelated and floating sector groups (like underwater areas) into a region
  * of the map so you don't have to think about where they are placed.
  */
trait SectorGroupPacker {

  /**
    * Finds and reserves (this class is mutable) a place for the given sector group.
    * The reason this takes a sector group is in case future versions will do something
    * more intelligent with concave groups.
    * @param sg  the sector group to find a home for.
    * @return  the location in the map where the top left point of the sector groups bounding box should go
    */
  def reserveArea(sg: SectorGroup): PointXY

}


object SimpleSectorGroupPacker {
  def apply(topLeft: PointXY, bottomRight: PointXY, margin: Int = 1024): SimpleSectorGroupPacker = {
    new SimpleSectorGroupPacker(topLeft, bottomRight, margin)
  }
}

/**
  * Packer that just places things in a row.
  * @param margin - amount of space to put between groups
  */
class SimpleSectorGroupPacker(val topLeft: PointXY, val bottomRight: PointXY, val margin: Int)
  extends SectorGroupPacker
{
  require(margin >= 0)
  require(bottomRight.x > topLeft.x)

  private def availWidth: Int = bottomRight.x - currentLeft
  private def availHeight: Int = bottomRight.y - topLeft.y

  var currentLeft = topLeft.x

  def canFit(bb: BoundingBox): Boolean = {
    return bb.w <= availWidth && bb.h <= availHeight

  }

  override def reserveArea(sg: SectorGroup): PointXY = reserveArea(sg.boundingBox)

  def reserveArea(bb: BoundingBox): PointXY = {
    if(! canFit(bb)) throw new Exception("Sector packer cannot find room for sector group")

    val r = new PointXY(currentLeft, topLeft.y)
    currentLeft += bb.w + margin
    r
  }

}
