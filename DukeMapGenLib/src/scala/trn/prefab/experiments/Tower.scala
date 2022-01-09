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

  def apply(sg: SectorGroup): Landing = {

    def wallNormal(sg: SectorGroup, conn: RedwallConnector): Int ={
      val wall = sg.map.getWallView(conn.getWallIds.get(0))
      wall.getVector.vectorRotatedCCW().toHeading
    }

    val up = sg.allRedwallConnectors.filter(_.getConnectorId == StairsUpConnectorId)
    require(up.size == 1 && up.head.getWallCount == 1)
    val stairsUpConn = up.head
    val directionOfStairs = wallNormal(sg, stairsUpConn)
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

    printTowerStairs(writer, 3, 48 * BuildConstants.ZStepHeight, 0, palette.getSG(3), palette.getSG(4), gameCfg)


    ExpUtil.finishAndWrite(writer, forcePlayerStart = false)
  }

  /**
    *
    * @param floorCount
    * @param deltaZ z distance from one level's floor to the next level's floor
    * @param bottomZ the z of the lowest level's floor
    */
  def printTowerStairs(writer: MapWriter, floorCount: Int, deltaZ: Int, bottomZ: Int, landingSg: SectorGroup, midLandingSg: SectorGroup, gameCfg: TexturePack): Unit ={
    require(floorCount > 0 && deltaZ > 0)
    require(deltaZ % (6 * BuildConstants.ZStepHeight) == 0, "DeltaZ must be a multiple of 6 * ZStepHeight")

    val landing = Landing(landingSg)
    val betweenFloorsSg = Landing(midLandingSg)

    val psg = writer.pasteSectorGroupAt(landing.sg, PointXYZ.ZERO)
    val sideWall = WallPrefab(gameCfg.tex(349)).withXScale(1.0) // TODO read from landing
    val stairFloor = HorizontalBrush(755).withRelative(true).withSmaller(true)
    val stairCeil = HorizontalBrush(437)
    addUpStairsToLanding(writer, landing, psg, betweenFloorsSg, SimpleStepBrush(sideWall, stairFloor, stairCeil))


  }

  def addUpStairsToLanding(
    writer: MapWriter,
    landing: Landing,
    pastedLanding: PastedSectorGroup,
    betweenFloorsSg: Landing,
    stepBrush: SimpleStepBrush, //sideWall: WallPrefab,
    // texPack: TexturePack,
  ): Unit = {
    val angle = SnapAngle.angleFromAtoB(betweenFloorsSg.directionToStairs, Heading.opposite(landing.directionToStairs))
    val stairCount = 8
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
    val anchor1 = Landing.connectorUpWallAnchor(writer, pastedLanding)

    val result = TowerStairPrinter.printSomeStairs(writer.outMap, anchor1, StairParams(stairCount), stepBrush)
    MiscPrinter.autoLinkRedWalls(writer.outMap, result.sectorIds.toSeq :+ Landing.connectorDown(otherLandingPsg).getSectorId.toInt)

    // second flight of stairs

    val result2 = TowerStairPrinter.printSomeStairs(
      writer.outMap,
      Landing.connectorUpWallAnchor(writer, otherLandingPsg),
      StairParams(stairCount),
      stepBrush
    )
  }


}
