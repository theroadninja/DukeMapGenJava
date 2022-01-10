package trn.render

import trn.{BuildConstants, PointXYZ, Wall, Map => DMap}
import trn.PointImplicits._
import trn.math.{RotatesCW, SnapAngle}
import trn.prefab.{DukeConfig, Heading, MapWriter, PastedSectorGroup, ReadOnlySectorGroup, RedwallConnector, SectorGroup, TexturePack}

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

/** helper class for both the level/floor landings and the midlevel landing */
case class Landing(sg: SectorGroup, directionToStairs: Int) extends RotatesCW[Landing] {
  override def rotatedCW: Landing = Landing(sg.rotatedCW, Heading.rotateCW(directionToStairs))
  def connectorDown: RedwallConnector = Landing.connectorDown(sg)
  def connectorUp: RedwallConnector = Landing.connectorUp(sg)
  def connectorGoingDownAnchor: PointXYZ = Landing.connectorDownAnchor(sg)
}

object Landing {
  val StairsUpConnectorId = 1
  val StairsDownConnectorId = 2

  def connectorDown(sg: ReadOnlySectorGroup): RedwallConnector = {
    sg.getRedwallConnector(StairsDownConnectorId)
  }

  def connectorUp(sg: ReadOnlySectorGroup): RedwallConnector = {
    sg.getRedwallConnector(StairsUpConnectorId)
  }

  /** outside point, anchor for moving the sector group around */
  def connectorUpAnchor(sg: ReadOnlySectorGroup): PointXYZ = {
    val wv = sg.getWallView(connectorUp(sg).getWallIds.get(0))
    // wv.p2.withZ(connectorUpFloorZ(sg))
    wv.p2.withZ(wv.getSectorFloorZ)
  }

  /** used to draw the stairs */
  def connectorUpWallAnchor(writer: MapWriter, sg: ReadOnlySectorGroup): WallSectorAnchor = {
    val conn = connectorUp(sg)
    WallSectorAnchor(writer.getWallAnchor(conn), conn.getSectorId)
  }

  /** outside point of the connector wall (second point) */
  def connectorDownAnchor(sg: ReadOnlySectorGroup): PointXYZ = {
    val wv = sg.getWallView(connectorDown(sg).getWallIds.get(0))
    wv.p1.withZ(wv.getSectorFloorZ)
  }

  /** for redwall conns with exactly one wall, gives you the nomral (the vector perpendicular to the wall, pointing
    * out of the sector */
  def wallNormal(sg: ReadOnlySectorGroup, conn: RedwallConnector): Int ={
    require(conn.getWallCount == 1)
    val wall = sg.getWallView(conn.getWallIds.get(0))
    wall.getVector.vectorRotatedCCW().toHeading
  }

  def directionToStairs(sg: ReadOnlySectorGroup): Int = {
    wallNormal(sg, connectorUp(sg))
  }

  def apply(sg: SectorGroup): Landing = {
    val up = sg.allRedwallConnectors.filter(_.getConnectorId == StairsUpConnectorId)
    require(up.size == 1 && up.head.getWallCount == 1)

    val directionOfStairs = directionToStairs(sg)
    val stairsDown = sg.allRedwallConnectors.filter{conn =>
      conn.getWallCount == 1 && conn.getConnectorId == StairsDownConnectorId && directionOfStairs == wallNormal(sg, conn)
    }
    require(stairsDown.size == 1)
    Landing(sg, directionOfStairs)
  }
}

/**
  * Creating a new Stair Printer for the Tower experiment because my first attempt sucked.
  */
object TowerStairPrinter {

  /**
    *
    * Just prints a single step.
    *
    *
    * I think the --- are the side walls:
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

    val floorZ = start.floorZ - stepHeight
    val ceilZ = start.ceilZ - stepHeight
    val newSectorId = MiscPrinter.createAndPaintSector(map, walls, floorZ, ceilZ, stairFloor, stairCeil)
    MiscPrinter.autoLinkRedWalls(map, startAnchor.sectorId, newSectorId)
    val anchor = WallSectorAnchor(WallAnchor(p3, p2, floorZ, ceilZ), newSectorId)

    val printedWalls = map.getWallLoop(map.getSector(newSectorId).getFirstWall).asScala
    PrintedStep(anchor, printedWalls(1), printedWalls(3), newSectorId)
  }


  /**
    * Prints stair of multiple steps.
    * @param map
    * @param start
    * @param stairParams
    * @param stepBrush
    * @return
    */
  def printSomeStairs(
    map: DMap,
    start: WallSectorAnchor,
    stairParams: StairParams,
    stepBrush: SimpleStepBrush,
    gameCfg: TexturePack,
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


    TextureUtil.alignXL2R(leftWalls, map, gameCfg)
    TextureUtil.alignYL2R(leftWalls, map, gameCfg)

    TextureUtil.alignXR2L(rightWalls, map, gameCfg)
    TextureUtil.alignYR2L(rightWalls, map, gameCfg)


    PrintStairResult(printedSteps.map(_.sectorId).toSet)
  }

  /** given two landings, print stairs between them (two staircases with a midlevel landing to reverse direction */
  def addStairsBetweenFloors(writer: MapWriter,
    lowerLanding: PastedSectorGroup,
    upperLanding: PastedSectorGroup,
    betweenFloorsSg: Landing,
    stepLength: Int,
    stepBrush: SimpleStepBrush,
    texPack: TexturePack,
  ): Unit = {

    require(Landing.connectorUpAnchor(lowerLanding).z > Landing.connectorUpAnchor(upperLanding).z) // z+ goes into earth
    val height = Landing.connectorUpAnchor(lowerLanding).z - Landing.connectorUpAnchor(upperLanding).z
    require(height > 0 && height % (6 * BuildConstants.ZStepHeight) == 0)
    val stepCount = height / 2 / StairParams.NormalStepHeight

    val stairCount = stepCount - 1 // the last one is the next floor
    val stairParams = StairParams(stairCount, stepLength)

    // TODO figure out how to paint the bottom step!
    // TODO also figure out how to line up the textures on the landing (if they are the same tex)
    // TODO random landing gets a fire extinguisher + hole

    // 1. midway landing
    val pastedLanding = lowerLanding
    val distance = stairParams.stepLength * stairCount
    val angle = SnapAngle.angleFromAtoB(betweenFloorsSg.directionToStairs, Heading.opposite(Landing.directionToStairs(pastedLanding)))
    val anchorPsg = Landing.connectorUpAnchor(pastedLanding)
    val otherLandingPsg = writer.pasteSectorGroupAtCustomAnchor(
      angle.rotate(betweenFloorsSg).sg,
      anchorPsg + new PointXYZ(0, -distance, -(stairCount + 1) * StairParams.NormalStepHeight),
      betweenFloorsSg.connectorGoingDownAnchor,
    )

    // 2. lower flight of stairs
    val result = TowerStairPrinter.printSomeStairs(
      writer.outMap,
      Landing.connectorUpWallAnchor(writer, pastedLanding),
      stairParams,
      stepBrush,
      texPack,
    )
    MiscPrinter.autoLinkRedWalls(writer.outMap, result.sectorIds.toSeq :+ Landing.connectorDown(otherLandingPsg).getSectorId.toInt)

    // 3. second flight of stairs
    val result2 = TowerStairPrinter.printSomeStairs(
      writer.outMap,
      Landing.connectorUpWallAnchor(writer, otherLandingPsg),
      stairParams,
      stepBrush,
      texPack,
    )
    // TODO autoLinkRedWalls should return the number of walls linked, so we can fail fast
    MiscPrinter.autoLinkRedWalls(writer.outMap, result2.sectorIds.toSeq :+ Landing.connectorDown(upperLanding).getSectorId.toInt)
  }
}
