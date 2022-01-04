package trn.render

import trn.{BuildConstants, Wall, WallView}

// See also BuildConstants
object TextureUtil {

  /**
    * calculates where the texture will get cut off if it is tiled horizontally along a wall
    * @param startOffset x coordinate on texture to start (0 for left edge)
    * @param scale  scaling of the texture (e.g. 2 for double the size)
    * @param texWidthPx the raw width of the texture in pixels (texture coordinates)
    * @param wallLength the length of the wall in map coordinates
    * @return
    */
  def calcOffset(startOffset: Int, scale: Double, texWidthPx: Int, wallLength: Double): Int = {
    require(startOffset >= 0)
    val texWorldWidthPx = texWidthPx * BuildConstants.TexScalingFactorX * scale
    val wallEnd = startOffset * BuildConstants.TexScalingFactorX + wallLength
    if(wallEnd < texWorldWidthPx){
      wallEnd.toInt / BuildConstants.TexScalingFactorX
    }else{
      (wallEnd.toInt % texWorldWidthPx.toInt) / BuildConstants.TexScalingFactorX
    }
  }

  /**
    * Lines up all of the textures in the wall loop, similar to pressig '.' in build.
    *
    * Caveats:
    *   - always starts with the first wall in the `wallLoop` seq, and always with offset 0
    *   - always modifies all walls; doesnt check what texture they have
    *   - assumes the seq represents a loop
    *   - doesnt handle floor/ceiling differences
    */
  def lineUpTextures(wallLoop: Seq[Wall], scale: Double, texWidth: Int): Unit = {
    // as far as I can tell, xPanning works like you would expect, and in texture coordinates
    var offset = 0
    for(i <- 0 until wallLoop.size){
      val j = (i + 1) % wallLoop.size
      val wallLength = wallLoop(i).getLocation.distanceTo(wallLoop(j).getLocation)
      wallLoop(i).setXScale(scale, wallLength)
      wallLoop(i).setXPanning(offset)
      val newOffset = TextureUtil.calcOffset(offset, scale, texWidth, wallLength)

      offset = newOffset
    }
  }

  /**
    * Lines up textures along a segment of walls
    *
    * TODO testing:
    * - differnt floor and ceiling heights
    * - walls aligned to floor and ceiling
    * - walls with holes (same and different textures above and below)
    * - slopes, especially when the wall doesnt line up to the slope start
    */
  def lineUpWallLengths(walls: Seq[WallView]): Unit = {
    // TODO separate functions to line up X and Y??

    ???
  }

  // prototype for setting xrepeat en masse
  // wrote this while writing StarPrinter
  def setWallXScale(wallLoop: Seq[Wall], scale: Double = 1.0): Unit = {
    for(i <- 0 until wallLoop.size){
      val j = (i + 1) % wallLoop.size
      val wallLength = wallLoop(i).getLocation.distanceTo(wallLoop(j).getLocation)
      // val xrepeat = wallLength / (128.0 * scale)
      // wallLoop(i).setXRepeat(Math.round(xrepeat.toInt))
      wallLoop(i).setXScale(scale, wallLength)
    }
  }

  // prototype for setting xrepeat en masse
  // wrote this while writing StarPrinter
  def setWallXRepeatCount(wallLoop: Seq[Wall], repeatCount: Int, texWidth: Int): Unit = {
    // TODO - generalize this to a function that will give you each wall and its next point...
    // like you pass it a f(wall, point2) => B
    for(i <- 0 until wallLoop.size){
      val j = (i + 1) % wallLoop.size
      // val wallLength = wallLoop(i).getLocation.distanceTo(wallLoop(j).getLocation)
      val xrepeat = repeatCount * texWidth / 8
      wallLoop(i).setXRepeat(xrepeat)
    }
  }
}
