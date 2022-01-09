package trn.render

import trn.prefab.{ConnectorScanner, DukeConfig, TexturePack}
import trn.{BuildConstants, HardcodedConfig, Main, MapLoader, Wall, WallView, Map => DMap}

import scala.collection.JavaConverters._

/**
  * Operations for scaling or lining up Textures on Walls.
  * See also BuildConstants, ArtFileReader
  */
object TextureUtil {

  /**
    * Given the xpan/offset of a wall (`leftEdgeTexUnits`) calculate the offset of the right edge, which is also the
    * xpan of the "next wall"
    */
  private[render] def rightEdgeOffset(wallLengthWorldUnits: Int, scale: Double, texLengthPx: Int, leftEdgeTexUnits: Int): Int = {
    val wallLengthTexUnits = wallLengthWorldUnits / scale / BuildConstants.TexScalingFactorX
    (leftEdgeTexUnits + wallLengthTexUnits.toInt) % texLengthPx
  }

  private[render] def rightEdgeOffsetXFlip(wallLengthWorldUnits: Int, scale: Double, texLengthPx: Int, leftEdgeTexUnits: Int): Int = {

    val wallLengthTexUnits = wallLengthWorldUnits / scale / BuildConstants.TexScalingFactorX
    positiveMod(leftEdgeTexUnits - wallLengthTexUnits.toInt, texLengthPx)
  }

  /**
    * Calc the offset to line of the texture of this wall with the next one
    *
    * Given the "texture offset" of the right edge of a wall (i.e. the texture offset of the next wall),
    * figure out what the texture offset needs to be for [the left edge of] this wall in order to make the texture
    * hit the right edge at offset `rightEdgeTextureUnits`
    * @param wallLengthWorldUnits the length of the wall (obviously in world units)
    * @param rightEdgeTextureUnits the offset of the next wall(to the right), also the texture offset of the right edge of this wall
    *                               this is in unscaled texture units, basically the coordinate system used by the walls
    *                               `xpan` proprety.
    *
    * @return the offset, in texture unit (i.e. the coordinates used by the wall's `xpan` property)
    */
  private[render] def leftEdgeOffset(wallLengthWorldUnits: Int, scale: Double, texLengthPx: Int, rightEdgeTextureUnits: Int): Int = {
    val wallLengthTexPx = wallLengthWorldUnits / scale / BuildConstants.TexScalingFactorX
    positiveMod(rightEdgeTextureUnits - wallLengthTexPx.toInt, texLengthPx)
  }

  /**
    * Calculates offsets for textures when X is flipped.
    *
    * Given a walls xpan value (which is the offset of the right edge b/c x is flipped), calculate the "offset" of the
    * left wall, which will be used ass the offset for the next [xflipped] wall.
    */
  private[render] def leftEdgeOffsetXFlip(wallLengthWorldUnits: Int, scale: Double, texLengthPx: Int, rightEdgeTexUnits: Int): Int = {
    val wallLengthTexPx = wallLengthWorldUnits / scale / BuildConstants.TexScalingFactorX
    (rightEdgeTexUnits + wallLengthTexPx.toInt) % texLengthPx
  }

  /**
    * Line up the textures (left-right) to the first wall in the list.  X direction only.
    * Like pressing '.' in Build.
    *
    * @param wallIds ids of walls whose textures should be aligned; walls(0) sets the starting offset and scale.
    * @param map map containing the walls to edit
    * @param gameCfg a TexturePack object to get the texture width from
    */
  def rightAlignX(wallIds: Seq[Int], map: DMap, gameCfg: TexturePack): Unit = if(wallIds.nonEmpty){
    // panning is never negative!  always a valid coord on the texture! (so max xpan determined by tex width)
    // the panning is where the texture starts, to increasing it moves the texture left...
    val firstWall = map.getWallView(wallIds.head)
    val scale = firstWall.getScaleX
    val tex = firstWall.tex
    val xflip = firstWall.stat.xflip
    require(!wallIds.map(map.getWall).exists(w => w.getTex != tex), "all walls must have the same texture")
    // val texWorldWidthPx = gameCfg.textureWidth(tex) * BuildConstants.TexScalingFactorX * scale

    // if xflip is true, need to change the whole algorithm!
    if(xflip){
      var offset = firstWall.xPan
      for(wallId <- wallIds.tail){
        val wall = map.getWall(wallId)
        val view = map.getWallView(wallId)
        wall.setXScale(scale, view.length)
        wall.setXFlip(xflip)

        offset = rightEdgeOffsetXFlip(view.length.toInt, scale, gameCfg.textureWidth(tex), offset)
        wall.setXPanning(offset)
      }
    }else{
      var offset = firstWall.xPan
      for(wallId <- wallIds){
        val wall = map.getWall(wallId)
        val view = map.getWallView(wallId)
        wall.setXScale(scale, view.length)
        wall.setXFlip(xflip)
        wall.setXPanning(offset)
        // offset = TextureUtil.calcOffset(offset, scale, gameCfg.textureWidth(tex), view.length())
        offset = rightEdgeOffset(view.length.toInt, scale, gameCfg.textureWidth(tex), offset)
      }
    }
  }


  /**
    * Starts at the last wall, and aligns the textures going right to left.
    * @param wallIds
    * @param map
    * @param gameCfg
    */
  def leftAlignX(wallIds: Seq[Int], map: DMap, gameCfg: TexturePack): Unit = if(wallIds.nonEmpty){

    val reversed = wallIds.reverse
    val firstWall = map.getWallView(reversed.head)
    val scale = firstWall.getScaleX
    val tex = firstWall.tex
    val xflip = firstWall.stat.xflip
    require(!wallIds.map(map.getWall).exists(w => w.getTex != tex), "all walls must have the same texture")
    val texWorldWidthPx = gameCfg.textureWidth(tex) * BuildConstants.TexScalingFactorX * scale
    // println(s"scale=${scale} tex width worldpx=${texWorldWidthPx}")

    if(xflip){
      var offset = leftEdgeOffsetXFlip(firstWall.length.toInt, scale, gameCfg.textureWidth(tex), firstWall.xPan)
      // println(s"first offset: ${offset}")
      for(wallId <- reversed.tail){
        val wall = map.getWall(wallId)
        val view = map.getWallView(wallId)

        wall.setXPanning(offset)
        wall.setXScale(scale, view.length)
        wall.setXFlip(xflip)
        offset = leftEdgeOffsetXFlip(view.length.toInt, scale, gameCfg.textureWidth(tex), offset)
      }
    }else{
      var offset = firstWall.xPan
      for(wallId <- reversed.tail){
        val wall = map.getWall(wallId)

        val view = map.getWallView(wallId)
        offset = leftEdgeOffset(view.length.toInt, scale, gameCfg.textureWidth(tex), offset)
        wall.setXPanning(offset)
        wall.setXScale(scale, view.length)
        wall.setXFlip(xflip)
      }

    }
  }

  /**
    * Align vertically all wall textures to the texture of the first wall
    * @param wallIds the ids of the walls to align (probably dont have to be contiguous)
    * @param map the map containing the walls
    */
  def rightAlignY(wallIds: Seq[Int], map: DMap): Unit = if(wallIds.nonEmpty){

    /**
      * Return the vertial(Z) coordinate, in build space (as opposed to texture space) where the texture starts
      */
    def getZ(wall: WallView): Int = {  // the anchor point where the texture starts
      val alignBottom = wall.stat.alignBottom
      if(wall.isRedwall){
        // now, "align bottom" actuall means align top
        if(alignBottom){ wall.getSectorCeilZ} else {
          val otherSector = map.getSector(wall.getWall.getOtherSector)
          // WARNING: the top part of the wall will still be wrong!
          otherSector.getFloorZ
        }
      }else{
        if(alignBottom){ wall.getSectorFloorZ} else { wall.getSectorCeilZ }
      }
    }

    def texToBuildCoords(ycoord: Int, yrepeat: Int): Int = {
      // TODO my notes (docs/YRepeat) say 2048 here, but the correct value seems to be 1024
      ycoord * 1024 / yrepeat // NOTE this z coord is not absolute; its relative to the place in buildspace where texture_y=0
    }
    def buildToTexCoords(zcoord: Int, yrepeat: Int): Int = {
      zcoord * yrepeat / 1024
    }

    val firstWall = map.getWallView(wallIds.head)
    if(firstWall.isRedwall){
      // TODO It could work if the wall has alignBottom == True ...
      throw new RuntimeException("this method doesnt work if the first wall is a red wall")
    }
    val yflip = firstWall.stat.yflip
    val yrepeat = firstWall.getYRepeat

    // The top of the texture (remember positive z goes into the earth)
    // (if top aligned) TOP_OF_WALL = TEXTURE_TOP + z_OFFSET
    // (bottom aligned) BOT_OF_WALL = TEXTURE_TOP + z_OFFSET
    // NEW_OFFSET = z_NEW_WALL - TEXTURE_TOP
    val texTop = getZ(firstWall) - texToBuildCoords(firstWall.getWall.getYPanning, yrepeat)

    // first set alignBottom = True on all red walls.
    //
    // when a wall has a "hole" in it, ALIGN_BOTTOM actually causes the top of the texture to be at
    // the top of the upper wall (same as a non-redwall when align bottom is false).  This is necessary because
    // a redwall with a hole in it, when align bottom is false, will align the top of the texture to the top of
    // the lower wall, and the bottom of the texture to the higher wall, which is impossible to line up
    wallIds.tail.map(map.getWall).filter(_.isRedWall).foreach{w => w.setAlignBottom(true)}

    for(wallId <- wallIds.tail){
      val wallView = map.getWallView(wallId)
      val newOffset = getZ(wallView) - texTop
      val ypan = positiveMod(buildToTexCoords(newOffset, yrepeat), BuildConstants.YPanMax)
      // println(s"setting y offset build coords = ${newOffset}  tex coords = ${ypan}")
      val wall = map.getWall(wallId)
      wall.setYPanning(ypan)
      wall.setYRepeat(yrepeat)
      wall.setYFlip(yflip)
    }
  }

  def leftAlignY(wallIds: Seq[Int], map: DMap): Unit = rightAlignY(wallIds.reverse, map)

  /**
    * Computes i % j but if i is negative, still returns the distance from the "smaller" j to i
    * (i.e. steps up the value of i until it is positive and then does the mod)
    * @param i  the i in i%j
    * @param j  the j in i%j
    * @return  i%j but handling negative numbers a certain way
    */
  private[TextureUtil] def positiveMod(i: Int, j: Int): Int = {
    var x = i
    while(x < 0){
      x += j
    }
    x % j
  }

  /**
    * Lines up all of the textures in the wall loop, similar to pressig '.' in build.
    *
    * TODO this is an older version of this logic and doesnt work very well
    *
    * Caveats:
    *   - always starts with the first wall in the `wallLoop` seq, and always with offset 0
    *   - always modifies all walls; doesnt check what texture they have
    *   - assumes the seq represents a loop
    *   - doesnt handle floor/ceiling differences
    */
  @deprecated
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

  // prototype for setting xrepeat en masse
  // wrote this while writing StarPrinter
  /**
    * Prototype for setting xrepeat en masse.  Wrote this while writing first version of StairPrinter.
    *
    * You can just call WallView.setXScale though
    *
    * @param wallLoop
    * @param scale
    */
  @deprecated
  def setWallXScale(wallLoop: Seq[Wall], scale: Double = 1.0): Unit = {
    for(i <- 0 until wallLoop.size){
      val j = (i + 1) % wallLoop.size
      val wallLength = wallLoop(i).getLocation.distanceTo(wallLoop(j).getLocation)
      // val xrepeat = wallLength / (128.0 * scale)
      // wallLoop(i).setXRepeat(Math.round(xrepeat.toInt))
      wallLoop(i).setXScale(scale, wallLength)
    }
  }

  def main(args: Array[String]): Unit = {

    // Texture 858 is 128x128
    // wall lengths in align2 are 1024

    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile, HardcodedConfig.getAtomicHeightsFile)
    val testMap = MapLoader.loadMap(HardcodedConfig.getEduke32Path("aligntest.map"))
    // val testMap = MapLoader.loadMap(HardcodedConfig.getEduke32Path("aligntest2.map"))
    val walls = testMap.getAllWallViews.asScala.filter(w => w.tex == 858)

    val walls2 = ConnectorScanner.sortContinuousWalls(walls)

    val wallIds = walls2.map(_.getWallId)

    println(s"first wall is ${wallIds.head}")
    testMap.getWall(wallIds(0)).setPal(1)
    testMap.getWall(wallIds.last).setPal(2)

    TextureUtil.rightAlignX(walls2.map(_.getWallId), testMap, gameCfg)
    // TextureUtil.leftAlignX(walls2.map(_.getWallId), testMap, gameCfg)
    // TextureUtil.rightAlignY(walls2.map(_.getWallId), testMap)
    TextureUtil.leftAlignY(walls2.map(_.getWallId), testMap)

    Main.deployTest(testMap, "output.map", HardcodedConfig.getEduke32Path("output.map"))
  }

  // TODO I wrote this to capture the math here, but I don't think I want this.
  // // prototype for setting xrepeat en masse
  // // wrote this while writing StarPrinter
  // def setWallXRepeatCount(wallLoop: Seq[Wall], repeatCount: Int, texWidth: Int): Unit = {
  //   // TODO - generalize this to a function that will give you each wall and its next point...
  //   // like you pass it a f(wall, point2) => B
  //   for(i <- 0 until wallLoop.size){
  //     val j = (i + 1) % wallLoop.size
  //     // val wallLength = wallLoop(i).getLocation.distanceTo(wallLoop(j).getLocation)
  //     val xrepeat = repeatCount * texWidth / 8
  //     wallLoop(i).setXRepeat(xrepeat)
  //   }
  // }

  // old attempt to write this function, which made everything super complicated by doing the math in the world
  // coordinate system (the newer version covnerts everything to the texture coordinate system)
  // def leftEdgeOffset(wallLengthWorldUnits: Int, scale: Double, texLengthPx: Int, texLengthWorldUnits: Int, rightEdgeTextureUnits: Int): Int = {
  //   // TODO i bet this all could be simpler if we just scaled wall length down to texture units!

  //   val ridgeEdgeWorldUnits = (rightEdgeTextureUnits * BuildConstants.TexScalingFactorX * scale)
  //   val leftEdgeWorldUnits = ridgeEdgeWorldUnits - wallLengthWorldUnits

  //   // This logic is strange, but the max xpan of 256 does NOT equal a single length of the texture, and calculating
  //   // a percentage like this is easier for me to understand.
  //   val leftEdgePercentage = positiveMod(leftEdgeWorldUnits.toInt, texLengthWorldUnits).toDouble / texLengthWorldUnits.toDouble
  //   val leftOffset = leftEdgePercentage * texLengthPx
  //   leftOffset.toInt
  // }

  /**
    * WARNING:  this is wrong (doesnt handle scaling correctly); use rightEdgeOffset instead
    *
    * calculates where the texture will get cut off if it is tiled horizontally along a wall (thinking left to right)
    * @param startOffset x coordinate on texture to start (0 for left edge)
    * @param scale  scaling of the texture (e.g. 2 for double the size)
    * @param texWidthPx the raw width of the texture in pixels (texture coordinates)
    * @param wallLength the length of the wall in map coordinates
    * @return
    */
  @deprecated
  private[render] def calcOffset(startOffset: Int, scale: Double, texWidthPx: Int, wallLength: Double): Int = {
    require(startOffset >= 0)
    val texWorldWidthPx = texWidthPx * BuildConstants.TexScalingFactorX * scale
    val wallEnd = startOffset * BuildConstants.TexScalingFactorX + wallLength
    if(wallEnd < texWorldWidthPx){
      wallEnd.toInt / BuildConstants.TexScalingFactorX
    }else{
      (wallEnd.toInt % texWorldWidthPx.toInt) / BuildConstants.TexScalingFactorX
    }
  }
}
