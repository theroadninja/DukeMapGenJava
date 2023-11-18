package trn.prefab.experiments.dijkdrop

import trn.{PointXYZ, RandomX, HardcodedConfig, ScalaMapLoader, Sprite, Map => DMap}
import trn.prefab.{CompassWriter, MapWriter, DukeConfig, GameConfig, SectorGroup, RedwallConnector, PrefabPalette, SpriteLogicException, Marker}
import trn.prefab.experiments.ExpUtil
import trn.prefab.experiments.dijkdrop.PipePalette.{LEFT_CAP_STRAIGHT, STRAIGHT2, RIGHT_CAP_STRAIGHT, DIAG, RIGHT_CAP_DIAG, MAX_TUNNEL_CONN_ID, LEFT_CAP_DIAG, STRAIGHT1}

object PipePalette {
  /**
    * tunnel redwall connectors must have a two digit id because:
    * - those digits become half of the 4 digit fall connector id
    * - the ids used to connect these pipes are >= 100
    */
  val MAX_TUNNEL_CONN_ID = 99

  val LEFT_CAP_STRAIGHT = "8"
  val LEFT_CAP_DIAG = "9"

  val STRAIGHT1 = "1"
  val STRAIGHT2 = "2"
  val DIAG = "4"

  val RIGHT_CAP_STRAIGHT = "6"
  val RIGHT_CAP_DIAG = "7"

}

class PipePalette(palette: PrefabPalette){

  def getSG(section: String): SectorGroup = {
    section match {
      case LEFT_CAP_STRAIGHT => palette.getSG(8)
      case LEFT_CAP_DIAG => palette.getSG(9)
      case STRAIGHT1 => palette.getSG(1)
      case STRAIGHT2 => palette.getSG(2)
      case DIAG => palette.getSG(4)
      case RIGHT_CAP_STRAIGHT => palette.getSG(6)
      case RIGHT_CAP_DIAG => palette.getSG(7)
    }
  }


  def eastConn(sg: SectorGroup): RedwallConnector = {
    sg.allRedwallConnectors.filterNot(_.isLinked(sg.map)).filter(c => c.getConnectorId > MAX_TUNNEL_CONN_ID && c.getConnectorId % 2 == 1).head
  }

  def westConn(sg: SectorGroup): RedwallConnector = {
    sg.allRedwallConnectors.filterNot(_.isLinked(sg.map)).filter(c => c.getConnectorId > MAX_TUNNEL_CONN_ID && c.getConnectorId % 2 == 0).head
  }

  def combine(gameCfg: GameConfig, leftSG: SectorGroup, rightSG: SectorGroup): SectorGroup = {
    val leftConn = eastConn(leftSG)
    val rightConn = westConn(rightSG)
    require(leftConn.getWallCount == 3)

    (leftConn.getConnectorId, rightConn.getConnectorId) match {
      case (a, b) if Math.abs(a-b) == 1 => leftSG.withGroupAttached(gameCfg, leftConn, rightSG, rightConn)
      case (a, b) if a == 101 && b == 200 => {
        val adapter = palette.getSG(3)
        val adapterConn = adapter.getRedwallConnector(100)
        require(adapterConn.getWallCount == 3)
        val sg = leftSG.withGroupAttached(gameCfg, leftConn, adapter, adapterConn)
        sg.withGroupAttached(gameCfg, eastConn(sg), rightSG, rightConn)
      }
      case (201, 100) => {
        val adapter = palette.getSG(5)
        val sg = leftSG.withGroupAttached(gameCfg, leftConn, adapter, adapter.getRedwallConnector(200))
        sg.withGroupAttached(gameCfg, eastConn(sg), rightSG, rightConn)
      }
    }


  }

}

/**
  * This is just one room in DijkDrop but it is complicated enough to warrant its own testing.
  */
object PipeRoom {

  val OtherOtherFilename = "dijk/dijkpipe.map"

  // for testing
  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)

    // TODO map contents to a case class
    val input3: DMap = ScalaMapLoader.loadMap(HardcodedConfig.EDUKE32PATH + OtherOtherFilename)
    val result = tryRun(gameCfg, input3)
    ExpUtil.write(result)
  }

  // for testing
  def tryRun(gameCfg: GameConfig, input: DMap): DMap = {
    val writer = MapWriter(gameCfg)
    try {
      val random = RandomX()
      run(gameCfg, random, input, writer)
    } catch {
      case e: SpriteLogicException => {
        e.printStackTrace()
        ExpUtil.finish(writer, removeMarkers = false, errorOnWarnings = false)
        writer.outMap
      }
    }
  }

  // for testing
  def run(gameCfg: GameConfig, random: RandomX, input: DMap, writer: MapWriter): DMap = {
    val sgZZ = makePipeRoom(gameCfg, random, ScalaMapLoader.paletteFromMap(gameCfg, input))
    writer.pasteSectorGroupAt(sgZZ, PointXYZ.ZERO)
    ExpUtil.finish(writer, removeMarkers=false)
    writer.outMap
  }

  def makePipeRoom(gameCfg: GameConfig, random: RandomX, input: PrefabPalette): SectorGroup = {
    val palette = new PipePalette(input)

    // val pipes = Seq(LEFT_CAP_DIAG, STRAIGHT1, STRAIGHT2, DIAG, RIGHT_CAP_DIAG)
    val start = random.randomElement(Seq(LEFT_CAP_DIAG, LEFT_CAP_STRAIGHT))
    val end = random.randomElement(Seq(RIGHT_CAP_DIAG, RIGHT_CAP_STRAIGHT))
    val middle = random.shuffle(Seq(STRAIGHT1, STRAIGHT1, DIAG, DIAG, STRAIGHT2)).toSeq.take(4)
    val pipes = start +: middle :+ end


    val sgZZ = pipes.map(palette.getSG).reduce { (left: SectorGroup, right: SectorGroup) => palette.combine(gameCfg, left, right) }

    // I gave all the pipe conns in this map id 1
    val sprites = sgZZ.allSprites.filter { s =>
      Marker.isMarker(s, Marker.Lotags.REDWALL_MARKER) && s.getHiTag == 1
    }

    sprites.zipWithIndex.foreach { case (marker, index) => marker.setHiTag(index + 1) }
    sgZZ.copy() // MUST rescan redwall connectors, so they have the new ids
  }
}
