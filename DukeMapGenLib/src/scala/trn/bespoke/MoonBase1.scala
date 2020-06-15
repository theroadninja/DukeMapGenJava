package trn.bespoke

import trn.{BuildConstants, HardcodedConfig, LineSegmentXY, Main, MapLoader, PlayerStart, PointXY, PointXYZ, Sector, WallView, Map => DMap}
import trn.prefab.{GameConfig, MapWriter, PasteOptions, PrefabPalette, SectorGroup}

object Space {
  val Door = 1 // sector group of split door with circles
}
/**
  * This creates a "moon base" themed map, aiming to recreat the levels in Duke3D Episode 2, especially L6, L7, L8.
  *
  * It is a "bespoke" generator, meaning it uses hardcoded assets and doesnt take in any input.
  *
  * Uses assets from space.map and moon1.map
  */
object MoonBase1 {

  def getSpaceMap: String = HardcodedConfig.getMapDataPath("SPACE.MAP")

  def getMoonMap: String = HardcodedConfig.getDosboxPath("MOON1.MAP")

  def run(gameCfg: GameConfig): Unit = {

    // TODO - compare space.map in proj folder and in workspace and fail fast if they are different (proj version
    // out of date)

    val spacePalette = MapLoader.loadPalette(getSpaceMap)
    val moonPalette = MapLoader.loadPalette(getMoonMap)

    val writer = MapWriter()

    val (center, _) = writer.sgBuilder.pasteSectorGroup2(moonPalette.getSectorGroup(1), PointXYZ.ZERO)

    val openConns = (0 until 4).map{_ =>
      val psg = writer.tryPasteConnectedTo(center, spacePalette.getSG(Space.Door), PasteOptions()).get
      val conn = psg.redwallConnectors.filterNot(_.isLinked(writer.outMap)).head
      (psg, conn)
    }

    //writer.randomShuffle()
    val startingAreas = Seq(2).map(moonPalette.getSG(_))
    val hallways = Seq(3, 4, 6, 7).map(moonPalette.getSG(_))

    val next = (0 until 3).map(_ => writer.randomElement(hallways)) :+ writer.randomElement(startingAreas)
    val next2: Seq[SectorGroup] = writer.randomShuffle(next).toSeq

    val openConns2 = openConns.zip(next2).map { case ((extantPsg, extantConn), newSg) =>
      val psg = writer.tryPasteConnectedTo(extantPsg, extantConn, newSg, PasteOptions()).get
      val conn = psg.redwallConnectors.filterNot(_.isLinked(writer.outMap)).headOption
      (psg, conn)
    }
    println(openConns2.map(_._2).count(_.isDefined))

    val setPieces = Seq(5).map(moonPalette.getSG(_))
    val setPieces2 = (0 until 3).map(_ => writer.randomElement(setPieces))

    openConns2.zip(writer.randomShuffle(setPieces2).toSeq).map { case ((extantPsg, extantConn), newSg) =>
      val psg = extantConn.map(conn => writer.tryPasteConnectedTo(extantPsg, conn, newSg, PasteOptions()).get)
    }



    writer.setAnyPlayerStart(force = true)
    writer.sgBuilder.clearMarkers()
    writer.checkSectorCount()
    Main.deployTest(writer.outMap)
  }
}
