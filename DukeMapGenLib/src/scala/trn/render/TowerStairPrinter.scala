package trn.render

import trn.{BuildConstants, Wall, Map => DMap}
import trn.PointImplicits._

import scala.collection.mutable

/** A wall anchor that also has the sector id */
case class WallSectorAnchor(anchor: WallAnchor, sectorId: Int)


case class PrintedStep(anchor: WallSectorAnchor, leftWall: Wall, rightWall: Wall)


/**
  * Creating a new Stair Printer for the Tower experiment because my first attempt sucked.
  */
object TowerStairPrinter {

  /**
    *  p1 ----> p2
    *  /\       |
    *  |        |
    *  |        |
    *  p0 <--- p3
    *
    */
  def printStair(
    map: DMap,
    startAnchor: WallSectorAnchor,
    sideWall: WallPrefab,
    stairLength: Int,
    stepHeight: Int,
    stairFloor: HorizontalBrush,
    stairCeil: HorizontalBrush,
  ): PrintedStep = {
    val start = startAnchor.anchor
    val v0 = start.vector
    val v1 = toI(v0.toF.rotatedCW.normalized * stairLength) // TODO need to change the length
    val p2 = start.p1 + v1
    val p3 = start.p0 + v1
    val walls = Seq(start.p0, start.p1, p2, p3).map(sideWall.create)
    val leftWall = walls(1)
    val rightWall = walls(3)

    val floorZ = start.floorZ - stepHeight
    val ceilZ = start.ceilZ - stepHeight
    val newSectorId = MiscPrinter.createAndPaintSector(map, walls, floorZ, ceilZ, stairFloor, stairCeil)
    MiscPrinter.autoLinkRedWalls(map, startAnchor.sectorId, newSectorId)
    val anchor = WallSectorAnchor(WallAnchor(p3, p2, floorZ, ceilZ), newSectorId)

    PrintedStep(anchor, leftWall, rightWall)
  }


  def printSomeStairs(map: DMap, start: WallSectorAnchor, sideWall: WallPrefab, stairFloor: HorizontalBrush, stairCeil: HorizontalBrush): Unit = {
    require(sideWall.tex.isDefined)

    val stairLength = 256
    val stepHeight = BuildConstants.ZStepHeight * 3
    val stepCount = 8

    // TODO fix wall XRepeat

    val leftWalls = mutable.ArrayBuffer[Wall]()
    var anchor: WallSectorAnchor = start
    for(_ <- 0 until stepCount){

      val ps = printStair(map, anchor, sideWall, stairLength, stepHeight, stairFloor, stairCeil)
      anchor = ps.anchor
      //ps.leftWall.setTexture(755)
      leftWalls.append(ps.leftWall)
    }

    // TODO: not enough; only lines up the X repeats, and misses the last one
    TextureUtil.lineUpTextures(leftWalls, 1.0, sideWall.tex.get.widthPx)


  }
}
