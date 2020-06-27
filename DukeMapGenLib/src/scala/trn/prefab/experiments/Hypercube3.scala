package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapLoader, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, Map => DMap}
import trn.MapImplicits._
import trn.duke.{PaletteList, TextureList}
import trn.prefab.hypercube.GridManager
import trn.prefab.hypercube.GridManager.Cell

import scala.collection.JavaConverters._
import trn.MapImplicits._

// object Axis {
//
//   val X = 0
//   val Y = 1
//   val Z = 2
//   val W = 3
//   val All = Seq(X, Y, Z, W)
//
//   private val FromLotag = Map(
//     SectorGroupHints.HypercubeGridX -> X,
//     SectorGroupHints.HypercubeGridY -> Y,
//     SectorGroupHints.HypercubeGridZ -> Z,
//     SectorGroupHints.HypercubeGridW -> W,
//   )
//
//   def fromLotag(lotag: Int): Option[Int] = FromLotag.get(lotag)
//
// }

case class HallPlacement(newSg: SectorGroup, c0: RedwallConnector, c1: RedwallConnector){

}

object RoomConnection {

  //def matches(psgConn: RedwallConnector, newSg: SectorGroup): S
  def conflict(a: Option[PartialCell], b: Option[PartialCell]): Boolean = (a, b) match {
    case (Some(a), Some(b)) if (a.conflicts(b)) => true
    case _ => false
  }
}

case class RoomConnection(
  c0: RedwallConnector,
  c0gridHints: Option[PartialCell],
  c1: RedwallConnector,
  c1cell: Option[PartialCell]
){

  private def inPlaceMatch(a: RedwallConnector, b: RedwallConnector): Boolean = {
    a.isMatch(b) && a.getTransformTo(b).asXY == PointXY.ZERO
  }

  private def matches3(sg: SectorGroup, map: DMap): Option[HallPlacement] = {
    val match1 = sg.allRedwallConnectors.filter(cSg0 => inPlaceMatch(c0, cSg0))
    val pair = match1.flatMap { sgConn0 =>
      val otherMatches = sg.allRedwallConnectors.filter(sgConn1 => inPlaceMatch(sgConn1, c1))
      otherMatches.filter(_ != sgConn0).headOption.map(sgConn1 => (sgConn0, sgConn1))
    }.headOption
    pair.map{ case(a, b) =>
        HallPlacement(sg, a, b)
    }

  }

  private def matches2(sg: SectorGroup, map: DMap): Seq[HallPlacement] = {
    val match1 = sg.allRedwallConnectors.filter{newC0 =>

      // c0cell - hints for the room
      val c0Hints = HyperUtils.gridHintsForConn(c0, map) // hints for connector c0 (not its room)
      val newGH = HyperUtils.gridHintsForConn(newC0, sg) // hints for the hallway connection
      c0.isMatch(newC0) && !(RoomConnection.conflict(c0gridHints, newGH) && RoomConnection.conflict(c0Hints, newGH))

      // TODO:  we have two connectors, and the overall room
    }
    val translatedSg = match1.map { sgConn1 =>
      val translate = sgConn1.getTransformTo(c0)
      val sg2 = sg.translated(translate) // TODO - not doing Z :(
      sg2
    }
    translatedSg.flatMap(sg => matches3(sg, map))
  }

  // TODO - use blueprints instead of actual connectors
  def findMatches(sg: SectorGroup, map: DMap, allowRotation: Boolean = true): Seq[HallPlacement] = {
    val rotations = if(allowRotation){
      val sg2 = sg.rotateCW
      val sg3 = sg2.rotateCW
      val sg4 = sg3.rotateCW
      Seq(sg, sg2, sg3, sg4)
    }else{
      Seq(sg)
    }
    rotations.flatMap(matches2(_, map))
  }
}

object GridPosition {
  type Cell = (Int, Int, Int, Int)
}


object Hyper3MapBuilder {

  val HyperGridLotags = Seq(
    SectorGroupHints.HypercubeGridX,
    SectorGroupHints.HypercubeGridY,
    SectorGroupHints.HypercubeGridZ,
    SectorGroupHints.HypercubeGridW
  )

  def isHyperGrid(s: Sprite): Boolean = {
    SectorGroupHints.isHint(s)
  }

  def cellCoords(sg: SectorGroup): Seq[PartialCell] = {
    HypercubeGridHint.calculateCells(sg.hints.hypercubeGridHints)
  }

  def toCell(cell: PartialCell): Cell = {
    require(cell.axisToCoord.size == 4)
    (cell.x.get, cell.y.get, cell.z.get, cell.w.get)
  }
}

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

class Hyper3MapBuilder(val outMap: DMap, palette: PrefabPalette, gridManager: trn.prefab.hypercube.GridManager)
  extends MapBuilder with AnywhereBuilder with HardcodedGameConfigProvider// TODO move AnywhereBuilder functionality to MapWriter
{
  val writer = MapWriter(this)

  override def pasteSectorGroup(sg: SectorGroup, translate: PointXYZ): PastedSectorGroup = writer.pasteSectorGroup(sg, translate)

  val grid = scala.collection.mutable.Map[Cell, PastedSectorGroup]()

  // TODO - in a generic builder, where is this location?  up to the algorithm?
  val sgPacker: SectorGroupPacker = new SimpleSectorGroupPacker(
    new PointXY(DMap.MIN_X, 0),
    new PointXY(DMap.MAX_X, DMap.MAX_Y),
    512)

  def placeInGrid(sg: SectorGroup): Unit = {
    val cellCoords = Hyper3MapBuilder.cellCoords(sg)

    val xy = HypercubeGridHint.topLeft(cellCoords)
    val (x, y) = xy.getOrElse(throw new RuntimeException("TODO: support pieces without TopLeft defined"))
    val z = HypercubeGridHint.getLowestFloor(cellCoords)
    val p = gridManager.toCoordinates(x, y, z)

    // TODO - this is not handling W ...

    val psg = writer.pasteSectorGroupAt(sg, p)
    cellCoords.foreach { coord =>
      require(coord.axisToCoord.size == 4)
      val cell = Hyper3MapBuilder.toCell(coord)
      require(!grid.contains(cell))
      grid.put(cell, psg)
    }

  }


  def placeInGrid(sg: SectorGroup, cell: Cell): PastedSectorGroup = { // TODO - only for debugging
    val p = gridManager.toCoordinates(cell)
    val psg = writer.pasteSectorGroupAt(sg, p)
    require(!grid.contains(cell))
    grid.put(cell, psg)
    psg
  }

  // TODO - this is a hack
  def markOccupied(psg: PastedSectorGroup, cell: Cell): Unit = {
    grid.put(cell, psg)
  }

  def tryPlaceInGrid(sg: SectorGroup, cell: Cell): Option[PastedSectorGroup] = {
    if(grid.contains(cell)){
      None
    }else{
      val psg = writer.pasteSectorGroupAt(sg, gridManager.toCoordinates(cell))
      grid.put(cell, psg)
      Some(psg)
    }
  }
  def fillFloor(sg: SectorGroup, z: Int, w: Int): Unit = {
    (0 until 3).foreach { x =>
      (0 until 3).foreach { y =>
        val cell = (x, y, z, w)
        tryPlaceInGrid(sg, cell)
        //if(!grid.contains(cell)){
        //  val psg = pasteSectorGroupAt(sg, gridManager.toCoordinates(cell))
        //  grid.put(cell, psg)
        //}
      }
    }
  }


  def getConnectionBlueprints(cell1: Cell, cell2: Cell, map: DMap): Seq[RoomConnection] = {
    val psg1: PastedSectorGroup = grid(cell1)
    val psg2 = grid(cell2)
    if(psg1 == psg2){
      Seq()
    }else{
      lazy val defaultHints = PartialCell(cell1).union(PartialCell(cell2))
      psg1.redwallConnectors.filter(!_.isLinked(map)).flatMap { c1 =>
        psg2.redwallConnectors.filter(!_.isLinked(map)).flatMap { c2 =>
          val c1hints = gridHintsForConn(c1).orElse(Some(defaultHints))
          val c2hints = gridHintsForConn(c2).orElse(Some(defaultHints))
          if(RoomConnection.conflict(c1hints, c2hints)){
            None
          }else{
            Some(RoomConnection(c1, c1hints, c2, c2hints))
          }
        }
      }
    }

  }

  /** we look at only the hings in the same sector as the connector */
  def gridHintsForConn(conn: RedwallConnector): Option[PartialCell] = {
    HyperUtils.gridHintsForConn(conn, sgBuilder.getMapTODO)
  }


  def connectRooms(n1: Cell, n2: Cell): Unit = {
    val roomConns = getConnectionBlueprints(n1, n2, sgBuilder.getMapTODO)

    // TODO - need to exclude sector groups with hints...
    val hallways = palette.allSectorGroups().asScala.filter(_.hints.hypercubeGridHints.isEmpty)

    hallways.flatMap { hallway =>
      roomConns.flatMap { roomConn =>
        roomConn.findMatches(hallway, sgBuilder.getMapTODO).map(placement => (roomConn, placement))
      }
    }.headOption.foreach { case (roomConn, placement) =>
      val (psg, idmap) = writer.pasteSectorGroup2(placement.newSg, PointXYZ.ZERO, Seq.empty) // its already been translated
    //cant use the conns in the room...
    val newC0 = placement.c0.translateIds(idmap, PointXYZ.ZERO, outMap)
      val newC1 = placement.c1.translateIds(idmap, PointXYZ.ZERO, outMap)

      sgBuilder.linkConnectors(roomConn.c0, newC0)
      sgBuilder.linkConnectors(roomConn.c1, newC1)

    }
  }

  def connectRooms(): Unit = {
    trn.prefab.hypercube.GridManager.eachNeighboor(grid.toMap) { case (n1: Cell, n2: Cell) => {
      connectRooms(n1, n2)

    } }
  }

}


object Hypercube3 extends PrefabExperiment {
  override val Filename = "infinity.map"

  override def run(mapLoader: MapLoader): DMap = {
    val sourceMap = mapLoader.load(Filename)
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true);
    run(palette)
  }

  def run(palette: PrefabPalette): DMap = {
    // TODO - how do we learn these?
    val gridCellDist = 1024 * 8  // distance between the center of each cell
    val gridCellDistZ = 1024 * 6  // distance between the center of each cell
    val oneDimRoomCount = 3 // number of rooms on the side of the grid (e.g. 3 for a 3x3x3x3 grid)

    val gridManager = trn.prefab.hypercube.GridManager.apply(gridCellDist, oneDimRoomCount, Some(gridCellDistZ))
    val builder = new Hyper3MapBuilder(DMap.createNew(), palette, gridManager)
    val writer = builder.writer

    val sg = palette.getSectorGroup(1)
    builder.placeInGrid(sg)

    //
    // -- standard stuff below --
    writer.sgBuilder.autoLinkRedwalls()
    writer.setAnyPlayerStart()
    writer.sgBuilder.clearMarkers()
    writer.outMap
  }

  def runOld(palette: PrefabPalette): DMap = {

    // TODO - how do we learn these?
    val gridCellDist = 1024 * 8  // distance between the center of each cell
    val gridCellDistZ = 1024 * 6  // distance between the center of each cell
    val oneDimRoomCount = 3 // number of rooms on the side of the grid (e.g. 3 for a 3x3x3x3 grid)

    val gridManager = trn.prefab.hypercube.GridManager.apply(gridCellDist, oneDimRoomCount, Some(gridCellDistZ))
    val builder = new Hyper3MapBuilder(DMap.createNew(), palette, gridManager)
    val writer = builder.writer

    //builder.placeAnywhere(palette.getSectorGroup(1))


    val sg = palette.getSectorGroup(1)
    builder.placeInGrid(sg)
    //builder.placeInGrid(palette.getSectorGroup(2))
    builder.placeInGrid(palette.getSectorGroup(3))

    builder.placeInGrid(palette.getSectorGroup(4), (2, 1, 1, 0))
    builder.placeInGrid(palette.getSectorGroup(5))
    // builder.connectRooms((2, 1, 1, 0), (2, 2, 1, 0))

    //builder.placeInGrid(palette.getSG(7), (2, 0, 0, 0))
    //builder.placeInGrid(palette.getSG(7)) // pit stairs

    builder.placeInGrid(palette.getSectorGroup(6), (0, 0, 1, 0))
    val placedMeasure = builder.placeInGrid(palette.getSectorGroup(9), (1, 0, 0, 0))
    builder.placeInGrid(palette.getSectorGroup(6), (2, 0, 1, 0))

    builder.markOccupied(placedMeasure, (1, 0, 1, 0))
    //builder.markOccupied(placedMeasure, (1, 0, 2, 0))
    //builder.connectRooms((0, 0, 1, 0), (1, 0, 1, 0))

    //
    // Fills below here
    //

    // TODO - need to assign hints to these things!
    //builder.fillFloor(palette.getSectorGroup(6), 1, 0)

    // this throws: builder.fillFloor(palette.getSectorGroup(6), 1, 1)
    val sg6W = palette.getSG(6).withModifiedSectors{s =>
      if(s.getFloorTexture == 898){
        s.setFloorPalette(PaletteList.BLUE_TO_RED)
      }
    }

    // TODO - have a strict mode where both construction and hint sprites Throw if they have unexpected lotags

    builder.connectRooms() // TODO - make this work

    val cells1 = HypercubeGridHint.calculateCells(palette.getSectorGroup(1).hints.hypercubeGridHints)
    println(cells1)

    // -- standard stuff below --
    writer.sgBuilder.autoLinkRedwalls()
    writer.setAnyPlayerStart()
    writer.sgBuilder.clearMarkers()
    writer.outMap
  }

}
