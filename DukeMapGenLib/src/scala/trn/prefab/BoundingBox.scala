package trn.prefab

import trn.PointXY

case class BoundingBox(xMin: Int, yMin: Int, xMax: Int, yMax: Int) {
  val w = xMax - xMin
  val h = yMax - yMin
  if(w < 0 || h < 0) throw new IllegalArgumentException

  def add(x: Int, y: Int): BoundingBox = {
    BoundingBox(
      math.min(xMin, x),
      math.min(yMin, y),
      math.max(xMax, x),
      math.max(yMax, y)
    )
  }

  /**
    * @param width
    * @param height
    * @return true if this bounding box could fix inside a rectangle of the given width and height
    */
  def fitsInside(width: Int, height: Int): Boolean = {
    if(width < 0 || height < 0) throw new IllegalArgumentException("height and width must be >= 0")
    w <= width && h <= height
  }

  /**
    * @param dest coordinate to move bounding box to
    * @return bounding box with top left at the given coordinate
    */
  def translate(translation: PointXY): BoundingBox = {
    BoundingBox(
      xMin + translation.x,
      yMin + translation.y,
      xMax + translation.x,
      yMax + translation.y,
    )
  }

  def topLeft: PointXY = {
    new PointXY(xMin, yMin)
  }

  /**
    *  returns the vector that would move the top left of this bounding box to that point
    */
  def getTranslateTo(point: PointXY): PointXY = {
    val p1 = topLeft
    p1.translateTo(point)
  }

}
