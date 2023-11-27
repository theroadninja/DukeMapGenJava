package trn.prefab.experiments.dijkdrop

import trn.{HardcodedConfig, RandomX, ScalaMapLoader, PointXYZ, Map => DMap}
import trn.prefab.{MapWriter, DukeConfig, GameConfig, SectorGroup, PrefabPalette, Item, SpriteLogicException, Marker}
import trn.prefab.experiments.ExpUtil

object RandomMoonRoom {

  val Filename = "dijk/dijkmoon.map"

  // for testing
  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)

    // TODO map contents to a case class
    val input3: DMap = ScalaMapLoader.loadMap(HardcodedConfig.EDUKE32PATH + Filename)
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
    val sgZZ = makeRoom(gameCfg, random, ScalaMapLoader.paletteFromMap(gameCfg, input))
    writer.pasteSectorGroupAt(sgZZ, PointXYZ.ZERO)
    ExpUtil.finish(writer, removeMarkers = false)
    writer.outMap
  }

  def attachEdge(gameCfg: GameConfig, sgMain: SectorGroup, mainConnId: Int, edgeSg: SectorGroup,
    attachConnector: Int // <-- the coon on edgeSg
  ): SectorGroup = {

    sgMain.withGroupAttachedAutoRotate(gameCfg, sgMain.getRedwallConnector(mainConnId), edgeSg) { edge =>
      edge.getRedwallConnector(attachConnector)
    }

  }

  def darken(sg: SectorGroup): SectorGroup = {
    val cp = sg.copy()
    // val textures = Seq(183, 258, 181, 346) // textures to darken
    val keepLight = Seq(150, 221, 463, 217, 219, 369, 128)
    val dark: Short = 16

    cp.allSectorIds.foreach { sectorId =>
      val sector = cp.map.getSector(sectorId)
      if(! keepLight.contains(sector.getFloorTex)){
        sector.setFloorShade(16)
      }
      if(! keepLight.contains(sector.getCeilingTexture)){
        sector.setCeilingShade(16)
      }
    }
    cp.allWalls.foreach { wall =>
      if(! keepLight.contains(wall.getTex)){
        wall.setShade(dark)
      }
    }
    cp
  }

  def makeRoom(gameCfg: GameConfig, random: RandomX, input: PrefabPalette): SectorGroup = {
    val sgMain = input.getSG(1)

    val decorEdgeSG = Seq(2, 3, 4, 5, 6, 7, 8) // corners
    val darkEdges = Seq(3, 4, 8) // subset of decorEdgeSG which are "dark"

    val innerWallDecorSG = Seq(32, 33, 34, 35, 36)
    val innerWallDecorConns = Seq(200, 201, 202, 203)
    val StandardChildConnector = 100

    val edgeId = random.randomElement(decorEdgeSG)
    val edge = input.getSG(edgeId)
    var sg = sgMain

    // the ids of the corner connectors in the main sector group
    val cornerConns = Seq(100, 101, 102, 103)
    sg = cornerConns.foldLeft(sg) { (accumulator, connId) =>
      attachEdge(gameCfg, accumulator, connId, edge, StandardChildConnector)
    }

    innerWallDecorConns.foreach { connId =>
      val attachMe = input.getSG(random.randomElement(innerWallDecorSG))
      sg = attachEdge(gameCfg, sg, connId, attachMe, StandardChildConnector)
    }

    val mainItems: Int = sg.allSprites.count(s => Marker.isMarker(s, Marker.Lotags.ITEM))
    val stuff = Seq(Item.Rpg, Item.Devastator, Item.AtomicHealth, Item.ShrinkRay, Item.FreezeRay, Item.Armor)
    for(_ <- 0 until mainItems){
      sg = sg.withItem2(random.randomElement(stuff))
    }

    if(darkEdges.contains(edgeId)){
      sg = darken(sg)
    }
    sg
  }
}
