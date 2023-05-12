package trn.prefab.experiments.tiling.pyth

import trn.math.SnapAngle
import trn.{BuildConstants, PointXY, RandomX, HardcodedConfig, ScalaMapLoader}
import trn.prefab.{MapWriter, GameConfig, SectorGroup, RedwallConnector, PrefabPalette, PastedSectorGroup}
import trn.prefab.experiments.tiling.{RenderedTile, PythTileType, BigTileEdge, TileEdge, PlanNode, TileFactory, TilePlan, TileMaker, PythagoreanTiling, SmallTileEdge, Tiling, TileNode, TilePlanner}


trait SpecialHallway {
  def sgId: Int

  def edgeInfoName: String

  def edgeInfo(tile: TileNode, edge: TileEdge, neighboor: TileNode): Option[String]

  final def matches(tile: TileNode, edge: TileEdge, neighboor: TileNode): Boolean = edgeInfo(tile, edge, neighboor).isDefined

  def matches2(edgeInfoA: Option[String], edgeInfoB: Option[String]): Boolean // TODO better name

  def makeEdge(writer: MapWriter, palette: PrefabPalette, tileA: RenderedTile, edgeA: Int, tileB: RenderedTile, edgeB: Int): Option[PastedSectorGroup]
}

class BackdropHallway(val sgId: Int, sgName1: String, sgName2: String) extends SpecialHallway {

  val edgeInfoName = "BACKDROP1"

  override def edgeInfo(tile: TileNode, edge: TileEdge, neighboor: TileNode): Option[String] = {
    if((tile.name == sgName1 && neighboor.name == sgName2) || tile.name == sgName2 && neighboor.name == sgName1){
      Some(edgeInfoName)
    }else{
      None
    }
  }

  override def matches2(edgeInfoA: Option[String], edgeInfoB: Option[String]): Boolean = {
    edgeInfoA == Some(edgeInfoName) || edgeInfoB == Some(edgeInfoName)
  }

  override def makeEdge(writer: MapWriter, palette: PrefabPalette, tileA: RenderedTile, edgeA: Int, tileB: RenderedTile, edgeB: Int): Option[PastedSectorGroup] = {
    val backdropPsg = if(tileA.tile.name == sgName2){ tileA.psg }else{ tileB.psg }
    val conn1 = backdropPsg.getRedwallConnector(BigTileEdge.EB)
    val windowSg = palette.getSG(14)
    val windowConn = windowSg.allRedwallConnectors.find(_.isMatch(conn1)).get
    val psg = writer.pasteAndLink(conn1, windowSg, windowConn, Seq.empty)
    Some(psg)
  }
}

class BigModularHallway(val sgId: Int, tilename: String) extends SpecialHallway {

  def edgeInfoName: String = "BIG5"

  override def edgeInfo(tile: TileNode, edge: TileEdge, neighboor: TileNode): Option[String] = {
    if(tile.name == tilename && neighboor.name == tilename) {
      Some(edgeInfoName)
    }else{
      None
    }
  }
  override def matches2(edgeInfoA: Option[String], edgeInfoB: Option[String]): Boolean = {
    edgeInfoA == Some(edgeInfoName) && edgeInfoB == Some(edgeInfoName)
  }

  override def makeEdge(writer: MapWriter, palette: PrefabPalette, tileA: RenderedTile, edgeA: Int, tileB: RenderedTile, edgeB: Int): Option[PastedSectorGroup] = {
    if (Seq(edgeA, edgeB).sorted == Seq(BigTileEdge.SB, BigTileEdge.NB).sorted) {
      val doorSg = palette.getSG(sgId)
      val northTile = if (edgeA == BigTileEdge.SB) {
        tileA
      } else {
        tileB
      }
      val northConn = northTile.psg.getRedwallConnector(BigTileEdge.SB)
      val doorConn = doorSg.allRedwallConnectors.find(conn => conn.isMatch(northConn)).get
      val psg = writer.pasteAndLink(northTile.psg.getRedwallConnector(BigTileEdge.SB), doorSg, doorConn, Seq.empty)
      Some(psg)
    } else if (Seq(edgeA, edgeB) == Seq(BigTileEdge.EB, BigTileEdge.WB).sorted) {
      val doorSg = palette.getSG(sgId).rotatedCW
      val westTile = if (edgeA == BigTileEdge.EB) {
        tileA
      } else {
        tileB
      }
      val eastConn = westTile.psg.getRedwallConnector(BigTileEdge.EB)
      val doorConn = doorSg.allRedwallConnectors.find(_.isMatch(eastConn)).get
      val psg = writer.pasteAndLink(westTile.psg.getRedwallConnector(BigTileEdge.EB), doorSg, doorConn, Seq.empty)
      Some(psg)
    } else {
      throw new Exception("this shouldnt happen")
    }

  }
}

/**
  * TODO support:
  *  X- default connectors
  *  X- special connectors (doors)
  *  X- window connectors
  *  X- end caps (decorations added on to that part of the sector group if there is no connection, like the "armory"
  *     cabinets)
  */
class PythMap2(val bigWidth: Int, val smallWidth: Int, palette: PrefabPalette) extends TileFactory {

  val tiling: PythagoreanTiling = PythagoreanTiling(
    new PointXY(BuildConstants.MIN_X, 0),
    bigWidth,
    smallWidth
  )

  val Big1 = "BIG3"
  val BigModular = "BIG5"
  val Small1 = "BIG2"
  val SmallModular = "SMALL10"

  val BigStart = "BIGSTART" // 16
  val BigEnd = "BIGEND" // 17

  val BigModularDoor = 12

  // val Backdrop1 = ("BACKDROP1", 13) // 13 is the weird jungle area
  val Backdrop1 = ("BACKDROP1", 15)

  val bigModuleHallway = new BigModularHallway(12, BigModular)
  val backdropHallway = new BackdropHallway(14, BigModular, Backdrop1._1)

  val sectorGroupIds = Map(
    Big1 -> 3,
    Small1 -> 4,
    Backdrop1._1 -> Backdrop1._2,
  )

  override def chooseTile(random: RandomX, coord: (Int, Int), tileType: Int, planNode: PlanNode, edges: Seq[Int]): String = {
    tiling.shapeType(coord) match {
      case PythTileType.BigTile => {
        if(planNode.backdrop) {
          Backdrop1._1
        }else if(planNode.start) {
          BigStart
        }else if(planNode.end){
          BigEnd
        }else{
          BigModular
        }
      }
      case PythTileType.SmallTile => SmallModular
    }
  }

  override def makeTile(gameCfg: GameConfig, tile: TileNode): SectorGroup = {
    if(tile.name == BigModular) {
      makeBigModular(gameCfg, tile, palette.getSG(5))
    }else if(tile.name == SmallModular) {
      makeSmallModular(gameCfg, tile)
    }else if(tile.name == BigStart) {
      makeBigModular(gameCfg, tile, palette.getSG(16))
    }else if(tile.name == BigEnd) {
      makeBigModular(gameCfg, tile, palette.getSG(17))
    }else{
      palette.getSG(sectorGroupIds(tile.name))
    }
  }

  def makeBigModular(gameCfg: GameConfig, tile: TileNode, center: SectorGroup): SectorGroup = {
    val defaultHallways = Map(
      BigTileEdge.SB -> (palette.getSG(6), 9), // Map of edgeId -> (hallway SG, hallway conn Id)
      BigTileEdge.SS -> (palette.getSG(6), 9),
      BigTileEdge.EB -> (palette.getSG(7), 9),
      BigTileEdge.ES -> (palette.getSG(7), 9),
      BigTileEdge.NB -> (palette.getSG(8), 9),
      BigTileEdge.NS -> (palette.getSG(8), 9),
      BigTileEdge.WB -> (palette.getSG(9), 9),
      BigTileEdge.WS -> (palette.getSG(9), 9),
    )
    makeModular(gameCfg, tile, center, defaultHallways, palette.getSG(11))
  }


  def makeSmallModular(gameCfg: GameConfig, tile: TileNode): SectorGroup = {
    val defaultHallways = Map(
      SmallTileEdge.E -> (palette.getSG(7), 9), // Map of edgeId -> (hallway SG, hallway conn Id)
      SmallTileEdge.S -> (palette.getSG(6), 9),
      SmallTileEdge.W -> (palette.getSG(9), 9),
      SmallTileEdge.N -> (palette.getSG(8), 9),
    )
    makeModular(gameCfg, tile, palette.getSG(10), defaultHallways, palette.getSG(11))
  }

  def makeModular(
    gameCfg: GameConfig,
    tile: TileNode,
    centerSg: SectorGroup,
    edgeGroups: Map[Int, (SectorGroup, Int)],
    endCapSg: SectorGroup,
  ): SectorGroup = {

    val edgeGroups2 = tiling.allEdges(tile.shape).map { edgeId =>
      edgeId -> tile.edges.get(edgeId).map(_.info.getOrElse("DEFAULT")).getOrElse("ENDCAP")
    }.toMap

    edgeGroups2.foldLeft(centerSg){ case (center, (edgeId, edgeInfo)) =>
      edgeInfo match {
        case BigModular => {
          center // Dont change them yet -- the connector will be drawn later
        }
        case "DEFAULT" => {
          val (hallwaySg, conn2) = edgeGroups(edgeId)
          TileFactory.attachByConnId(gameCfg, center, hallwaySg, edgeId, Some(conn2))
        }
        case "ENDCAP" => {
          val snapAngle = rotationsToMatch(endCapSg, 9, center.getRedwallConnector(edgeId)).get
          val endCapSg2 = snapAngle * endCapSg
          center.withGroupAttached(gameCfg, center.getRedwallConnectorsById(edgeId).head, endCapSg2, endCapSg2.getRedwallConnector(9))
          // center
        }
        case backdropHallway.edgeInfoName => {
          // dont add any connectors, a special hallway will be drawn later
          center
        }
      }
    }

  }

  override def edgeInfo(tile: TileNode, edge: TileEdge, neighboor: TileNode): Option[String] = {


    if(bigModuleHallway.matches(tile, edge, neighboor)) {
      bigModuleHallway.edgeInfo(tile, edge, neighboor)
    }else if(backdropHallway.matches(tile, edge, neighboor)){
    // }else if((tile.name == BigModular && neighboor.name == Backdrop1._1) || tile.name == Backdrop1._1 && neighboor.name == BigModular){
      Some(backdropHallway.edgeInfoName)
    }else{
      None
    }
  }

  override def makeEdge(writer: MapWriter, tileA: RenderedTile, edgeA: Int, tileB: RenderedTile, edgeB: Int): Option[PastedSectorGroup] = {
    val edgeInfoA = tileA.tile.edges(edgeA).info
    val edgeInfoB = tileB.tile.edges(edgeB).info

    if(bigModuleHallway.matches2(edgeInfoA, edgeInfoB)) {
      bigModuleHallway.makeEdge(writer, palette, tileA, edgeA, tileB, edgeB)
    }else if(backdropHallway.matches2(edgeInfoA, edgeInfoB)){
      backdropHallway.makeEdge(writer, palette, tileA, edgeA, tileB, edgeB)
    }else{
      None
    }
  }

  /**
    * given a sector group `rotateMe` with connector identified by `rotateConnId`, figure out how many 90 degree
    * rotations are needed for the connector with id `rotateConnId` to match `target`
    * @param rotateMe
    * @param rotateConnId
    * @param target
    * @return
    */
  def rotationsToMatch(rotateMe: SectorGroup, rotateConnId: Int, target: RedwallConnector): Option[SnapAngle] = {
    SnapAngle.rotateUntil(rotateMe){ sg =>
      sg.getRedwallConnector(rotateConnId).isMatch(target)
    }
  }
  override def getTileMaker(gameCfg: GameConfig, name: String, tileType: Int): TileMaker = ???

}

/**
  * For the main() see TilingMain.scala.
  */
object PythMap2 {
  val InputMap: String = "tiles/pyth2.map"

  // trying to automatically encode many different combos of connections
  def testMap(tiling: Tiling): TilePlan = {
    val coords = Seq(
      (3, -1),
      (3, 0),
      (3, 1),
      (1, 2), (2, 2), (5, 2),
      (2, 3), (3, 3), (5, 3),
      (0, 4), (1, 4), (2, 4), (3, 4), (4, 4), (5, 4),
      (1, 5), (2, 5), (3, 5), (4, 5),
      (0, 6), (2, 6), (3, 6), (5, 6),
      (0, 7), (5, 7),
      (0, 8), (2, 8), (4, 8),
      (1, 9), (2, 9),
      (1, 10)
    )
    val addWindows = true

    val plan = TilePlanner.fromHardcoded(
      tiling, coords
    )

    // replace the bottom big area with a start area
    plan.put((0, 8), PlanNode(start=true))
    plan.put((5, 2), PlanNode(end=true))

    if(addWindows){
      // TODO want the connectors to the window nodes to be automatic
      plan.put((-1, 4), PlanNode(backdrop=true))
      plan.putEdge((-1, 4), (0, 4))
    }
    plan
  }


  /**
    * Reads the input map to determine the small and big widths of the pythagorean tiling, expecting to find:
    * - sector group 1, with exactly 1 sector, with four axis-aligned walls in a square shape:  wall length = big width
    * - sector group 2, with exactly 1 sector, with four axis-aligned walls in a square shape:  wall length = small width
    *
    * small width must be < big width
    *
    * @param palette a "prefab palette" loaded from the input map
    * @return (bigWidth, smallWidth)
    */
  def readWidths(palette: PrefabPalette): (Int, Int) = {

    def readWidth(sg: SectorGroup): Int = {
      val walls = sg.getAllWallViews.toSeq
      require(walls.size == 4)
      val walls2 = walls.filter(_.isAxisAligned)
      require(walls2.filter(_.getLineSegment.getManhattanLength == walls2(0).getLineSegment.getManhattanLength).size == 4)
      walls2(0).getLineSegment.getManhattanLength.toInt
    }
    val big = readWidth(palette.getSG(1))
    val small = readWidth(palette.getSG(2))
    require(big > small)
    (big, small)
  }

  def apply(): PythMap2 = {
    val palette: PrefabPalette = ScalaMapLoader.loadPalette(HardcodedConfig.getEduke32Path(InputMap))

    val (big, small) = readWidths(palette)
    new PythMap2(big, small, palette)
  }
}
