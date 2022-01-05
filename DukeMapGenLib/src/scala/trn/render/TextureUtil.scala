package trn.render

import trn.prefab.{ConnectorScanner, DukeConfig, GameConfig, MapWriter}
import trn.{BuildConstants, HardcodedConfig, Main, MapLoader, Wall, WallStat, WallView, Map => DMap}

import scala.collection.JavaConverters._

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
    * Line up the textures (left-right) to the first wall in the list.  X direction only.
    * Like pressing '.' in Build.
    *
    * @param wallIds ids of walls whose textures should be aligned; walls(0) sets the starting offset and scale.
    * @param map map containing the walls to edit
    * @param gameCfg a GameConfig object to get the texture width from
    */
  def rightAlignX(wallIds: Seq[Int], map: DMap, gameCfg: GameConfig): Unit = if(wallIds.nonEmpty){
    // panning is never negative!  always a valid coord on the texture! (so max xpan determined by tex width)
    // the panning is where the texture starts, to increasing it moves the texture left...
    val firstWall = map.getWallView(wallIds.head)
    val scale = firstWall.getScaleX
    val tex = firstWall.tex
    val xflip = firstWall.stat.xflip
    val yflip = firstWall.stat.yflip

    // TODO if xflip is true, need to change the whole algorithm!

    if(xflip){

      var offset = firstWall.xPan
      for(wallId <- wallIds.tail){
        val wall = map.getWall(wallId)
        require(wall.getTex == tex, "all walls must have the same texture")

        wall.setXFlip(xflip)
        wall.setYFlip(yflip)
        // println(s"setting wall ${wallId} xpan to ${offset}")

        val view = map.getWallView(wallId)
        wall.setXScale(scale, view.length)

        val texWorldWidthPx = gameCfg.textureWidth(tex) * BuildConstants.TexScalingFactorX * scale
        offset = (offset * BuildConstants.TexScalingFactorX) - view.length.toInt
        if(offset < 0){
          offset += texWorldWidthPx.toInt
        }
        offset = offset / BuildConstants.TexScalingFactorX
        println(s"setting offset ${offset}")
        wall.setXPanning(offset)
      }



    }else{
      var offset = firstWall.xPan
      for(wallId <- wallIds){
        val wall = map.getWall(wallId)
        require(wall.getTex == tex, "all walls must have the same texture")

        wall.setXFlip(xflip)
        wall.setYFlip(yflip)
        // println(s"setting wall ${wallId} xpan to ${offset}")

        val view = map.getWallView(wallId)
        wall.setXScale(scale, view.length)
        wall.setXPanning(offset)
        offset = TextureUtil.calcOffset(offset, scale, gameCfg.textureWidth(tex), view.length())
      }
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
    * Lines up textures along a segment of walls so that the left sides match the right sides.
    *
    * TODO testing:
    * - differnt floor and ceiling heights
    * - walls aligned to floor and ceiling
    * - walls with holes (same and different textures above and below)
    * - slopes, especially when the wall doesnt line up to the slope start
    *
    * @param startWallId id of the wall to align based on (doesnt have to be first wall)
    */
  def alignWallSectionX(startWallId: Int, wallIds: Seq[Int], map: DMap): Unit = {
    // TODO separate functions to line up X and Y??

    // TODO also copy the flip bits

    // TODO split up and call alignRight() and alignLeft() as needed

    // walls(0).wall

    ???
  }


  def alignWallSectionY(wallIds: Seq[Int], map: DMap): Unit = {

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

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    val testMap = MapLoader.loadMap(HardcodedConfig.getEduke32Path("aligntest.map"))

    val walls = testMap.getAllWallViews.asScala.filter(w => w.tex == 858)

    val walls2 = ConnectorScanner.sortContinuousWalls(walls)

    val wallIds = walls2.map(_.getWallId)

    println(s"first wall is ${wallIds.head}")
    testMap.getWall(wallIds(0)).setPal(1)
    testMap.getWall(wallIds.last).setPal(2)

    TextureUtil.rightAlignX(walls2.map(_.getWallId), testMap, gameCfg)

    Main.deployTest(testMap, "output.map", HardcodedConfig.getEduke32Path("output.map"))
  }
}
