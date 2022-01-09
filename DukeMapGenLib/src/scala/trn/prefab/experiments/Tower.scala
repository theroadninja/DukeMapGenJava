package trn.prefab.experiments

import trn.{BuildConstants, HardcodedConfig, MapLoader, PointXY, PointXYZ, Map => DMap}
import trn.prefab.{DukeConfig, GameConfig, Heading, MapWriter, PastedSectorGroup, RedwallConnector, SectorGroup}
import trn.render.{HorizontalBrush, MiscPrinter, StairParams, Texture, TextureUtil, TowerStairPrinter, WallAnchor, WallPrefab, WallSectorAnchor}
import trn.PointImplicits._
import trn.math.{RotatesCW, SnapAngle}

import scala.collection.JavaConverters._

/**
  * Requirements:
  * - redwall conns must have 1 wall each.
  *
  * @param sg
  */
case class Landing(sg: SectorGroup, directionToStairs: Int) extends RotatesCW[Landing] {

  override def rotatedCW: Landing = Landing(sg.rotatedCW, Heading.rotateCW(directionToStairs))
}

object Landing {
  val StairsUpConnectorId = 1
  val StairsDownConnectorId = 2

  def connectorGoingDown(psg: PastedSectorGroup): RedwallConnector = {
    psg.getRedwallConnectorsById(1).get(0)
  }

  def apply(sg: SectorGroup): Landing = {

    def wallNormal(sg: SectorGroup, conn: RedwallConnector): Int ={
      val wall = sg.map.getWallView(conn.getWallIds.get(0))
      wall.getVector.vectorRotatedCCW().toHeading
    }

    val up = sg.allRedwallConnectors.filter(_.getConnectorId == StairsUpConnectorId)
    require(up.size == 1 && up.head.getWallCount == 1)
    val stairsUpConn = up.head

    //val w2 = sg.map.getWallView(stairsUpConn.getWallIds.get(0))
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

    val landing = Landing(palette.getSG(3))
    val betweenFloorsSg = Landing(palette.getSG(4))



    // TODO should have hardcoded stairs in case the level doesnt specify any
    val psg = writer.pasteSectorGroupAt(landing.sg, PointXYZ.ZERO)


    val angle = SnapAngle.angleFromAtoB(betweenFloorsSg.directionToStairs, SnapAngle(2).rotateHeading(landing.directionToStairs))
    val otherLandingPsg = writer.pasteSectorGroupAt(angle.rotate(betweenFloorsSg).sg, new PointXYZ(0, -1024 * 4, 0))

    // how to I pasted them to face each other?


    val conn1 = psg.getRedwallConnectorsById(1).get(0)
    val startSector = conn1.getSectorId
    val anchor1 = WallSectorAnchor(writer.getWallAnchor(conn1), startSector)

    val sideWall = WallPrefab(gameCfg.tex(349)).withXScale(1.0) // TODO read from landing
    val stairFloor = HorizontalBrush(755).withRelative(true).withSmaller(true)
    val stairCeil = HorizontalBrush(437)

    TowerStairPrinter.printSomeStairs(writer.outMap, anchor1, StairParams(8), sideWall, stairFloor, stairCeil)

    ExpUtil.finishAndWrite(writer, forcePlayerStart = false)
  }


}
