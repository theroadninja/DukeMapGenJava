package trn.prefab.experiments

import trn.{BuildConstants, HardcodedConfig, MapLoader, PointXY, PointXYZ, ScalaMapLoader, Map => DMap}
import trn.prefab.{DukeConfig, GameConfig, Heading, MapWriter, PastedSectorGroup, PrefabPalette, ReadOnlySectorGroup, RedwallConnector, SectorGroup, TexturePack}
import trn.render.{HorizontalBrush, Landing, MiscPrinter, SimpleStepBrush, StairParams, Texture, TextureUtil, TowerStairPrinter, WallAnchor, WallPrefab, WallSectorAnchor}
import trn.PointImplicits._
import trn.math.{RotatesCW, SnapAngle}

import scala.collection.JavaConverters._



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
    val palette = ScalaMapLoader.loadPalette(HardcodedConfig.getEduke32Path("tower1.map"))
    // TODO should have hardcoded stairs in case the level doesnt specify any

    // TODO should we add a "drift" feature where each floor is offset just a tiny bit so its obvious in the map editor?
    val floorCount = 3

    // val levelHeight = 48 * BuildConstants.ZStepHeight
    val levelHeight = 66 * BuildConstants.ZStepHeight

    printTowerStairs(writer, palette, PointXYZ.ZERO, floorCount, levelHeight, palette.getSG(3), palette.getSG(4), gameCfg)


    ExpUtil.finishAndWrite(writer, forcePlayerStart = false)
  }

  def printTowerStairs(
    writer: MapWriter,
    palette: PrefabPalette,
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

    val sideWall = WallPrefab(gameCfg.tex(349)).withXScale(1.0) // TODO read from landing
    // val sideWall = WallPrefab(gameCfg.tex(396)).withXScale(1.0) // TODO TMP
    val stairFloor = HorizontalBrush(755).withRelative(true).withSmaller(true)
    val stairCeil = HorizontalBrush(437)

    pairs.foreach { pair =>
      val lower = pair(0) // TODO there is a more elegant way to do this
      val upper = pair(1)
      val stepLength = 320
      TowerStairPrinter.addStairsBetweenFloors(writer, lower, upper, betweenFloorsSg, stepLength, SimpleStepBrush(sideWall, stairFloor, stairCeil), gameCfg)
    }

    // printing the levels

    addLevelToLanding(writer, palette.getSG(1), landings(0))
    addLevelToLanding(writer, palette.getSG(2), landings(1))
    addLevelToLanding(writer, palette.getSG(1), landings(2))



  }

  def addLevelToLanding(writer: MapWriter, level: SectorGroup, landing: PastedSectorGroup): Unit = {
    val conn = landing.getRedwallConnector(3)
    val landingWall = writer.outMap.getWallView(conn.getWallIds.get(0))
    // val landingSector = conn.getSectorId

    val heading1 = Landing.wallNormal(landing, conn)

    // val level1 = palette.getSG(1)
    val levelConn = level.getRedwallConnector(3)

    val levelRotated = SnapAngle.angleFromAtoB(Landing.wallNormal(level, levelConn), Heading.opposite(heading1)).rotate(level)

    writer.pasteAndLink(conn, levelRotated, levelRotated.getRedwallConnector(3), Seq.empty)

  }


}
