package trn.render

import trn.{BuildConstants, Wall, Map => DMap}
import trn.PointImplicits._
import trn.prefab.{DukeConfig, RedwallConnector}

import scala.collection.mutable
import scala.collection.JavaConverters._

/** A wall anchor that also has the sector id */
case class WallSectorAnchor(anchor: WallAnchor, sectorId: Int)

case class PrintedStep(anchor: WallSectorAnchor, leftWallId: Int, rightWallId: Int, sectorId: Int)

case class PrintStairResult(sectorIds: Set[Int])

/**
  *
  * @param stepCount how many steps are there - TODO does this include the last one or not?
  * @param stepLength the horizontal length from one step to the next step (NOT the distance between the walls)
  */
case class StairParams(stepCount: Int, stepLength: Int = StairParams.NormalStairLength)

object StairParams {
  val NormalStairLength: Int = 256

  val NormalStepHeight: Int = BuildConstants.ZStepHeight * 3
}

case class SimpleStepBrush(sideWall: WallPrefab, floor: HorizontalBrush, ceil: HorizontalBrush)


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
    TextureUtil.setWallXScale(walls, 1.0)
    val leftWall = walls(1)
    val rightWall = walls(3)

    val floorZ = start.floorZ - stepHeight
    val ceilZ = start.ceilZ - stepHeight
    val newSectorId = MiscPrinter.createAndPaintSector(map, walls, floorZ, ceilZ, stairFloor, stairCeil)
    MiscPrinter.autoLinkRedWalls(map, startAnchor.sectorId, newSectorId)
    val anchor = WallSectorAnchor(WallAnchor(p3, p2, floorZ, ceilZ), newSectorId)

    val printedWalls = map.getWallLoop(map.getSector(newSectorId).getFirstWall).asScala
    PrintedStep(anchor, printedWalls(1), printedWalls(3), newSectorId)
  }


  def printSomeStairs(
    map: DMap,
    start: WallSectorAnchor,
    stairParams: StairParams,
    stepBrush: SimpleStepBrush,
    // sideWall: WallPrefab,
    // stairFloor: HorizontalBrush,
    // stairCeil: HorizontalBrush
  ): PrintStairResult = {
    val sideWall = stepBrush.sideWall
    val stairFloor = stepBrush.floor
    val stairCeil = stepBrush.ceil
    require(sideWall.tex.isDefined)

    // TODO - add an option to make the top and/or bottom stair shorter by one ZStepHeight
    //        could this double as the feature to make the first/last step flush with the landing?

    val stairLength = stairParams.stepLength
    val stepHeight = StairParams.NormalStepHeight
    val stepCount = stairParams.stepCount

    // TODO fix wall XRepeat

    // val leftWalls = mutable.ArrayBuffer[Int]()

    val printedSteps = mutable.ArrayBuffer[PrintedStep]()
    var anchor: WallSectorAnchor = start
    for(_ <- 0 until stepCount){

      val ps = printStair(map, anchor, sideWall, stairLength, stepHeight, stairFloor, stairCeil)
      anchor = ps.anchor
      // leftWalls.append(ps.leftWallId)
      printedSteps.append(ps)
    }
    val leftWalls = printedSteps.map(_.leftWallId)
    val rightWalls = printedSteps.map(_.rightWallId).reverse


    // TODO: not enough; only lines up the X repeats, and misses the last one
    // TextureUtil.lineUpTextures(leftWalls, 1.0, sideWall.tex.get.widthPx)
    val gameCfg = DukeConfig.loadHardCodedVersion() // TODO pass into this function
    TextureUtil.alignXL2R(leftWalls, map, gameCfg)
    TextureUtil.alignYL2R(leftWalls, map)

    TextureUtil.alignXR2L(rightWalls, map, gameCfg)
    TextureUtil.alignYR2L(rightWalls, map)
    // rightWalls.map(map.getWall).foreach(_.setPal(2))


    PrintStairResult(printedSteps.map(_.sectorId).toSet)
  }
}
