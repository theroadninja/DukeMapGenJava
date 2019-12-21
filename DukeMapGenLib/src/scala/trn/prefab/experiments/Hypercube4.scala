package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapLoader, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, Map => DMap}
import trn.MapImplicits._
import trn.duke.{PaletteList, TextureList}
import trn.prefab.hypercube.GridManager
import trn.prefab.hypercube.GridManager.Cell

import scala.collection.JavaConverters._
import trn.MapImplicits._


/** TODO - this is copied from Hypercube3 */
object HyperUtils {

  /** we look at only the hings in the same sector as the connector */
  def gridHintsForConn(conn: RedwallConnector, map: DMap): Option[PartialCell] = {
    //sgBuilder.getMapTODO.getSector(conn.getSectorId)
    //conn.getSectorId
    val sprites = map.allSprites.filter(s => s.getSectorId == conn.getSectorId && HypercubeGridHint.isGridHint(s))
    val hints = sprites.map(s => HypercubeGridHint(s.getLotag, s.getHiTag))
    val cells = HypercubeGridHint.calculateCells(hints)
    SpriteLogicException.throwIf(cells.size > 1, s"Connector in sector with grid hints resolving to >1 grid cells: ${cells}")
    cells.headOption
  }

  def gridHintsForConn(conn: RedwallConnector, sg: SectorGroup): Option[PartialCell] = gridHintsForConn(conn, sg.getMap)
}

class Hyper4MapBuilder(val outMap: DMap, palette: PrefabPalette, gridManager: trn.prefab.hypercube.GridManager)
  extends MapBuilder // with AnywhereBuilder
{
  val writer = MapWriter(this)

  // TODO - put this somewhere more generic
  val grid = scala.collection.mutable.Map[Cell, PastedSectorGroup]()

  // val sgPacker: SectorGroupPacker = new SimpleSectorGroupPacker(
  //   new PointXY(DMap.MIN_X, 0),
  //   new PointXY(DMap.MAX_X, DMap.MAX_Y),
  //   512)

  def tryPlaceInGrid(sg: SectorGroup, primaryLocation: Cell, otherLocations: Seq[Cell] = Seq.empty): Option[PastedSectorGroup] = { // TODO - only for debugging
    val allLocations = primaryLocation +: otherLocations
    if(allLocations.filter(loc => grid.contains(loc)).nonEmpty){
      // something is already there
      None
    }else{
      val p = gridManager.toCoordinates(primaryLocation)
      val psg = pasteSectorGroupAt(sg, p, anchorOnly = true)
      allLocations.foreach { cell =>
        require(!grid.contains(cell))
        grid.put(cell, psg)
      }
      Some(psg)
    }
  }

  def fillFloor(sg: SectorGroup, z: Int, w: Int): Unit = {
    (0 until 3).foreach { x =>
      (0 until 3).foreach { y =>
        val cell = (x, y, z, w)
        tryPlaceInGrid(sg, cell)
      }
    }
  }

  def connectRooms(n1: Cell, n2: Cell, hallways: Seq[SectorGroup]): Unit = {
    println(s"connecting ${n1} and ${n2}")
    if(n1._4 != n2._4 || grid(n1) == grid(n2)){ // dont allow crossing W, or connecting group to itself
      return
    }

    def matchesW(c: RedwallConnector, psg: PastedSectorGroup, currentW: Int): Boolean = {
      HyperUtils.gridHintsForConn(c, psg.getMap).flatMap(_.w.map(w => w == currentW)).getOrElse(true)
    }

    val n1conns = grid(n1).redwallConnectors.filter(c => matchesW(c, grid(n1), n1._4))
    val n2conns = grid(n2).redwallConnectors.filter(c => matchesW(c, grid(n2), n2._4))

    // TODO - filter on W
    val x = hallways.flatMap { hallway =>
      JigsawPlacer.findPlacements(
        hallway,
        n1conns, //grid(n1).redwallConnectors,
        n2conns, //grid(n2).redwallConnectors,
        outMap,
        allowRotation = true,
        zMatch = JigsawPlacer.ZMatchOverlap
      )
    }
    x.headOption.foreach { placement =>
      writer.pasteAndLink2(placement.newSg, PointXYZ.ZERO, placement.conns)
    }

  }

  def connectRooms(hallways: Seq[SectorGroup], strict: Boolean): Unit = {
    trn.prefab.hypercube.GridManager.eachNeighboor(grid.toMap) { case (n1: Cell, n2: Cell) => {
      if(strict){
        connectRooms(n1, n2, hallways)
      }else{
        try{
          connectRooms(n1, n2, hallways)
        }catch{
          case _ => {}
        }

      }

    } }
  }
}

/**
  * This is really another attempt at hyper 3.
  */
object Hypercube4 extends PrefabExperiment {
  override val Filename = "hyper4.map"

  override def run(mapLoader: MapLoader): DMap = {
    val sourceMap = mapLoader.load(Filename)
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true)
    val result = run(palette)
    println(s"Sector count: ${result.getSectorCount}")
    result
  }

  def run(palette: PrefabPalette): DMap = {
    val gridCellDist = 1024 * 8 // distance between the center of each cell
    val gridCellDistZ = 1024 * 4 // distance between the center of each cell
    val sideGridLength = 3 // number of rooms on the side of the grid (e.g. 3 for a 3x3x3x3 grid)

    val gridManager = trn.prefab.hypercube.GridManager.apply(gridCellDist, sideGridLength, Some(gridCellDistZ))
    val builder = new Hyper4MapBuilder(DMap.createNew(), palette, gridManager)
    val writer = builder.writer

    val LowFloor = 0
    val MidFloor = 1
    val HighFloor = 2

    val BluW = 0

    val sg = palette.getSectorGroup(1)
    builder.tryPlaceInGrid(sg, (1, 1, MidFloor, 0), Seq((1, 1, MidFloor, 1), (1, 1, MidFloor, 2)))

    // lift
    // builder.tryPlaceInGrid(palette.getSG(13), (1, 0, MidFloor, 0), Seq((1, 0, HighFloor, 0)))

    //stairs
    builder.tryPlaceInGrid(palette.getSG(14), (2, 0, LowFloor, 0), Seq((2, 0, MidFloor, 0)))
    builder.tryPlaceInGrid(palette.getSG(14).rotate180, (0, 2, MidFloor, 0), Seq((0, 2, HighFloor, 0)))

    // top floor slanted stuff
    builder.tryPlaceInGrid(palette.getSG(15), (0, 1, HighFloor, 0))
    builder.tryPlaceInGrid(palette.getSG(15).rotate180, (2, 1, HighFloor, 0))

    // top floor center
    builder.tryPlaceInGrid(palette.getSG(16), (1, 1, HighFloor, 0))

    def placeOnEdge(sg: SectorGroup, floor: Int, w: Int): Option[PastedSectorGroup] = {
      val availableSpots = gridManager.allCells.filter(c => gridManager.isEdgeXY(c) && !builder.grid.contains(c))
      val spot = writer.randomElement(availableSpots.filter(c => c._3 == floor && c._4 == w))
      var sg2 = sg
      sg2.hints.hypercubeEdge.xyEdgeAngle.foreach { currentHeading =>
        val count = gridManager.edgeRotationCount(spot, currentHeading)
        (0 until count).foreach { _ =>
          sg2 = sg2.rotateCW
        }

      }
      builder.tryPlaceInGrid(sg2, spot)

    }

    val WizardRoom = 17
    val Bridge = 19
    // top floor wizard tower
    val wizardTower = palette.getSG(WizardRoom)
    require(wizardTower.hints.hypercubeEdge.xyEdgeOnly)
    require(wizardTower.hints.hypercubeEdge.xyEdgeAngle.isDefined)
    require(placeOnEdge(wizardTower, HighFloor, BluW).isDefined)

    // TODO - this can be MidFloor or LowFloor
    // builder.tryPlaceInGrid(palette.getSG(19), (2, 1, MidFloor, BluW))
    //println(s"required angle: ${palette.getSG(19).hints.hypercubeEdge.xyEdgeAngle.get}")
    require(placeOnEdge(palette.getSG(Bridge), MidFloor, BluW).isDefined)



    // builder.fillFloor(palette.getSG(18), LowFloor, 0)
    builder.fillFloor(palette.getSG(10), MidFloor, 0)
    builder.fillFloor(palette.getSG(10), HighFloor, 0)

    val redRoom = MapWriter.painted2(palette.getSG(10), PaletteList.BLUE_TO_RED)
    builder.fillFloor(redRoom, MidFloor, 1)

    val greenRoom = MapWriter.painted2(palette.getSG(10), PaletteList.BLUE_TO_GREEN)
    builder.fillFloor(greenRoom, MidFloor, 2)

    val hallways = palette.anonSectorGroups().asScala.toSeq
    builder.connectRooms(hallways, true)

    //
    // -- standard stuff below --
    writer.sgBuilder.autoLinkRedwalls()
    writer.builder.setAnyPlayerStart()
    writer.sgBuilder.clearMarkers()
    writer.outMap
  }
}