package trn.prefab.experiments

import trn.{BuildConstants, HardcodedConfig, MapLoader, PointXY, PointXYZ, Map => DMap}
import trn.prefab.{DukeConfig, GameConfig, Heading, MapWriter, PastedSectorGroup, ReadOnlySectorGroup, RedwallConnector, SectorGroup, TexturePack}
import trn.render.{HorizontalBrush, MiscPrinter, SimpleStepBrush, StairParams, Texture, TextureUtil, TowerStairPrinter, WallAnchor, WallPrefab, WallSectorAnchor}
import trn.PointImplicits._
import trn.math.{RotatesCW, SnapAngle}

import scala.collection.JavaConverters._

/**
  * TODO need an abstraction that works for psg OR sg (although the rotation might be a problem)
  *
  *
  * Requirements:
  * - redwall conns must have 1 wall each.
  *
  * @param sg
  */
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

  private def wallNormal(sg: ReadOnlySectorGroup, conn: RedwallConnector): Int ={
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


object Tower {

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    val writer = MapWriter(gameCfg)
    try {
      run(gameCfg, writer)
    } catch {
      case e => {
        writer.setAnyPlayerStart(true)
        ExpUtil.deployMap(writer.outMap, "error.map")
        // Main.deployTest(writer.outMap, "error.map", HardcodedConfig.getEduke32Path("error.map"))
        throw e
      }
    }
  }

  def run(gameCfg: GameConfig, writer: MapWriter): Unit = {
    val palette = MapLoader.loadPalette(HardcodedConfig.getEduke32Path("tower1.map"))
    // TODO should have hardcoded stairs in case the level doesnt specify any

    // TODO should we add a "drift" feature where each floor is offset just a tiny bit so its obvious in the map editor?
    val floorCount = 3
    printTowerStairs(writer, PointXYZ.ZERO, floorCount, 48 * BuildConstants.ZStepHeight, palette.getSG(3), palette.getSG(4), gameCfg)


    ExpUtil.finishAndWrite(writer, forcePlayerStart = false)
  }

  def printTowerStairs(
    writer: MapWriter,
    locationBottom: PointXYZ, // location of the first landing -- TODO not sure about x and y coords
    floorCount: Int,
    deltaZ: Int,
    landingSg: SectorGroup,
    midLandingSg: SectorGroup,
    gameCfg: TexturePack
  ): Unit ={
    require(floorCount > 1 && deltaZ > 0)
    require(deltaZ % (6 * BuildConstants.ZStepHeight) == 0, "DeltaZ must be a multiple of 6 * ZStepHeight")

    val landing = Landing(landingSg)
    val betweenFloorsSg = Landing(midLandingSg) // TODO would be better to render instead of using a prefab (and have a customizable step in between)

    val locations = (0 until floorCount).map(i => locationBottom + new PointXYZ(0, 0, -i * deltaZ))
    println(locations)

    val landings = locations.map { location =>
      writer.pasteSectorGroupAt(landing.sg, location)
    }


    val pairs = landings.sliding(2, 1)

    //val sideWall = WallPrefab(gameCfg.tex(349)).withXScale(1.0) // TODO read from landing
    val sideWall = WallPrefab(gameCfg.tex(396)).withXScale(1.0) // TODO TMP
    val stairFloor = HorizontalBrush(755).withRelative(true).withSmaller(true)
    val stairCeil = HorizontalBrush(437)

    pairs.foreach { pair =>
      val lower = pair(0) // TODO there is a more elegant way to do this
      val upper = pair(1)
      addStairs(writer, landing, lower, upper, betweenFloorsSg, SimpleStepBrush(sideWall, stairFloor, stairCeil))
    }
    // addStairs(writer, landing, landings(0), landings(1), betweenFloorsSg, SimpleStepBrush(sideWall, stairFloor, stairCeil))


  }

  def addStairs(writer: MapWriter,
    landing: Landing, // TODO get rid of this
    lowerLanding: PastedSectorGroup, upperLanding: PastedSectorGroup,
    betweenFloorsSg: Landing,
    stepBrush: SimpleStepBrush): Unit = {

    require(Landing.connectorUpAnchor(lowerLanding).z > Landing.connectorUpAnchor(upperLanding).z) // z+ goes into earth
    val height = Landing.connectorUpAnchor(lowerLanding).z - Landing.connectorUpAnchor(upperLanding).z

    // TODO: render the landing so that we can use the optional step in between to deal with other heights!
    require(height > 0 && height % (6 * BuildConstants.ZStepHeight) == 0)

    val stepCount = height / 2 / StairParams.NormalStepHeight

    addUpStairsToLanding(writer, lowerLanding, betweenFloorsSg, upperLanding, stepCount, stepBrush)
  }

  def addUpStairsToLanding(
    writer: MapWriter,
    pastedLanding: PastedSectorGroup,
    betweenFloorsSg: Landing,
    upperPastedLanding: PastedSectorGroup,
    stepCount: Int,
    stepBrush: SimpleStepBrush,
    // texPack: TexturePack,
  ): Unit = {
    val angle = SnapAngle.angleFromAtoB(betweenFloorsSg.directionToStairs, Heading.opposite(Landing.directionToStairs(pastedLanding)))
    // val stairCount = 8
    val stairCount = stepCount - 1 // the last one is the next floor
    val distance = StairParams.NormalStairLength * stairCount

    val anchorPsg = Landing.connectorUpAnchor(pastedLanding)
    val otherLandingPsg = writer.pasteSectorGroupAtCustomAnchor(
      angle.rotate(betweenFloorsSg).sg,
      anchorPsg + new PointXYZ(0, -distance, -(stairCount + 1) * StairParams.NormalStepHeight),
      betweenFloorsSg.connectorGoingDownAnchor,
    )

    // TODO figure out how to paint the bottom step!
    // TODO also figure out how to line up the textures on the landing (if they are the same tex)

    // the actual stairs
    // val anchor1 = Landing.connectorUpWallAnchor(writer, pastedLanding)
    val result = TowerStairPrinter.printSomeStairs(
      writer.outMap,
      Landing.connectorUpWallAnchor(writer, pastedLanding),
      StairParams(stairCount),
      stepBrush,
    )
    MiscPrinter.autoLinkRedWalls(writer.outMap, result.sectorIds.toSeq :+ Landing.connectorDown(otherLandingPsg).getSectorId.toInt)

    // second flight of stairs
    val result2 = TowerStairPrinter.printSomeStairs(
      writer.outMap,
      Landing.connectorUpWallAnchor(writer, otherLandingPsg),
      StairParams(stairCount),
      stepBrush,
    )

    // TODO autoLinkRedWalls should return the number of walls linked, so we can fail fast
    MiscPrinter.autoLinkRedWalls(writer.outMap, result2.sectorIds.toSeq :+ Landing.connectorDown(upperPastedLanding).getSectorId.toInt)
  }


}
