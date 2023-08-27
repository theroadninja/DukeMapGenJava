package trn.prefab.experiments
import trn.{BuildConstants, PointXY, FuncUtils, HardcodedConfig, ScalaMapLoader, Map => DMap}
import trn.prefab.{CompassWriter, BoundingBox, MapWriter, PrefabUtils, MaxCopyHint, DukeConfig, SectorGroup, RedwallConnector, Heading, PrefabPalette, Matrix2D, EntropyProvider, PastedSectorGroup, SpriteLogicException, Marker}
import trn.FuncImplicits._
import trn.duke.TextureList
import trn.prefab.grid2d.{Side, GridPiece, SimpleGridPiece, SectorGroupPiece}

import scala.collection.JavaConverters._

object GridBuilderInput {
  // seems the most playable
  def defaultInput: GridBuilderInput = GridBuilderInput(6, 6, 2)

  // def defaultInput: GridBuilderInput = GridBuilderInput(8, 8, 2)
}
/**
  * @param maxCellsX maximum width of grid in number of cells
  * @param maxCellsY maximum height of grid in number of cells
  */
case class GridBuilderInput(
  maxCellsX: Int,
  maxCellsY: Int,

  /** minimum manhattan distance between start and end */
  startEndDist: Int,
)


object Cell2D {
  def apply(t: (Int, Int)): Cell2D = Cell2D(t._1, t._2)
}

case class Cell2D(x: Int, y: Int){
  lazy val asTuple: (Int, Int) = (x, y)
  lazy val toSeq: Seq[Int] = Seq(x, y)

  def moveTowards(heading: Int): Cell2D = heading match {
    case Heading.E => Cell2D(x + 1, y)
    case Heading.W => Cell2D(x - 1, y)
    case Heading.N => Cell2D(x, y - 1)
    case Heading.S => Cell2D(x, y + 1)
    case _ => throw new RuntimeException(s"invalid heading ${heading}")
  }

  def manhattanDist(other: Cell2D): Int = Math.abs(x - other.x) + Math.abs(y - other.y)
}

object GridParams2D {
  // Note:  the resulting bounding box will not match `bb` perfectly
  def fillMap(bb: BoundingBox, alignXY: PointXY, sideLength: Int): GridParams2D = {
    val width = bb.xMax - alignXY.x
    val height = bb.yMax - alignXY.y
    val cellCount = Math.min(width / sideLength,  height / sideLength)
    GridParams2D(alignXY, cellCount, cellCount, sideLength)
  }
}
case class GridParams2D(
  val originMapCoords: PointXY,   // the (x,y) map coordinates where cell (0,0) on this grid will be
  val cellCountX: Int,
  val cellCountY: Int,
  val sideLength: Int,
) {
  val width = cellCountX * sideLength
  val height = cellCountY * sideLength

  /** @returns the bounding box in map coordinates */
  val boundingBox: BoundingBox = {
    val p2 = originMapCoords.add(new PointXY(width, height))
    BoundingBox(Seq(originMapCoords, p2))
  }

  def withMaxCellCounts(maxCellsX: Int, maxCellsY: Int): GridParams2D = this.copy(
    cellCountX = Math.min(cellCountX, maxCellsX),
    cellCountY = Math.min(cellCountY, maxCellsY),
  )

  def toMapCoords(cellx: Int, celly: Int): PointXY = {
    new PointXY(cellx * sideLength, celly * sideLength).add(originMapCoords)
  }

  /** @returns the cell indexes of the corners */
  lazy val cornerCells: Seq[(Int, Int)] = Seq((0, 0), (0, cellCountY-1), (cellCountX-1, 0), (cellCountX-1, cellCountY-1))

  def isCorner(cell: Cell2D): Boolean = cornerCells.contains(cell.asTuple)

  lazy val borderCells: Seq[(Int, Int)] = {
    val topBottom = (0 until cellCountX).flatMap(x => Seq((x, 0),(x, cellCountY-1)))
    val leftRight = (1 until cellCountY-1).flatMap(y => Seq((0, y), (cellCountX-1, y)))
    topBottom ++ leftRight
  }

  lazy val allCells: Seq[(Int, Int)] = (0 until cellCountX).flatMap { x => (0 until cellCountY).map(y => (x, y)) }

  /**
    * @returns the headings (direction) of the outside of the map if the cell is on the edge, or an empty list.
    *   e.x. the top left edge will return Seq(N, W)
    * @param cellx
    * @param celly
    */
  def adjacentEdgeHeadings(cellx: Int, celly: Int): Seq[Int] = {
    val avoid = if(cellx == 0){
      Seq(Heading.W)
    }else if(cellx == cellCountX - 1){
      Seq(Heading.E)
    }else{
      Seq.empty
    }
    val avoid2 = if(celly == 0){
      Seq(Heading.N)
    }else if(celly == cellCountY - 1){
      Seq(Heading.S)
    }else{
      Seq.empty
    }
    avoid ++ avoid2
  }

  /** @returns the bounding box of the cell in map coordinates */
  def cellBoundingBox(cellx: Int, celly: Int): BoundingBox = {
    val bb = BoundingBox(cellx, celly, cellx + 1, celly + 1).transform(Matrix2D.scale(sideLength, sideLength))
    bb.transform(Matrix2D.translate(originMapCoords.x, originMapCoords.y))
  }

  /**
    * This is a sort of exclusive intersection test -- intersection only counts if area is non zero
    * @param bb a bounding box, in map coords
    */
  def cellsIntersectedBy(bb: BoundingBox): Seq[(Int, Int)] = {
    require(bb.w > 0 && bb.h > 0) // this function doest make sense for a zero-area bounding box
    val topLeft = {
      val p = bb.topLeft.subtractedBy(originMapCoords)
      new PointXY(Math.max(p.x, 0), Math.max(p.y, 0))
    }
    val bottomRight = {
      val p = new PointXY(bb.xMax, bb.yMax).subtractedBy(originMapCoords)
      new PointXY(Math.min(p.x, boundingBox.xMax), Math.min(p.y, boundingBox.yMax))
    }
    if(topLeft.x >= width || topLeft.y >= height || bottomRight.x <= 0 || bottomRight.y <= 0){
      return Seq.empty
    }else{
      val startX = topLeft.x / sideLength
      val startY = topLeft.y / sideLength
      val stopX = (bottomRight.x - 1) / sideLength
      val stopY = (bottomRight.y - 1) / sideLength
      require(startX <= stopX && startY <= stopY)
      (startX to stopX).flatMap { x =>
        (startY to stopY).map ( y => (x, y) )
      }
    }
  }

}

class Grid2D(val params: GridParams2D) {
  val grid = scala.collection.mutable.Map[(Int, Int), PastedSectorGroup]()

  def put(cell: (Int, Int), psg: PastedSectorGroup): Unit = {
    grid.put(cell, psg)
  }
  def get(cell: (Int, Int)): Option[PastedSectorGroup] = grid.get(cell)

  def eachCell(f: (Int, Int) => Unit): Unit = (0 until params.cellCountX).foreach { x =>
    (0 until params.cellCountY).foreach { y =>
      f(x, y)
    }
  }

  def allPsg: Iterable[PastedSectorGroup] = grid.values

  def pasteToCell(writer: MapWriter, cellx: Int, celly: Int, sg: SectorGroup): Option[PastedSectorGroup] = {
    val sg2 = Some(sg) // TODO clean up
    sg2.map { sg3 =>
      val cellBox = params.cellBoundingBox(cellx, celly)
      val target = SquareTileMain.alignToBox(sg3, cellBox)
      val psg = writer.pasteSectorGroupAt(sg3, target.withZ(0))
      grid.put((cellx, celly), psg)
      psg
    }
  }
}

object MaxCopyTracker2 {
  def countAll(groups: Iterable[SectorGroup]): MaxCopyTracker2 = {
    val tracker = new MaxCopyTracker2()
    groups.foreach(tracker.record(_, 1))
    tracker
  }
}

/**
  * Tracks counts of sector groups used, for the Max Copy hint.
  * Does not work with anonymous sector groups
  *
  * TODO - replace with the other stand alone one
  */
class MaxCopyTracker2 {
  // map of sector group id -> count
  val copies = scala.collection.mutable.Map[Int, Int]()

  def count(groupId: Int): Int = copies.get(groupId).getOrElse(0)

  def canPaste(sg: SectorGroup): Boolean = (sg.groupIdOpt, sg.hints.maxCopies) match {
    case (Some(groupId), Some(maxCopies)) => count(groupId) < maxCopies
    case _ => true
  }

  def canPasteTile(piece: SectorGroupPiece): Boolean = canPaste(piece.sg)

  def recordId(groupId: Int, n: Int = 1): Unit = copies.put(groupId, Math.max(0, count(groupId) + n))

  /** record the fact that this piece was just added */
  def record(sg: SectorGroup, n: Int = 1): Unit = (sg.groupIdOpt, sg.hints.maxCopies) match {
    case (Some(groupId), Some(_)) => recordId(groupId, n)
    case _ => {}
  }

  override def toString: String = s"{CopyTracker ${copies} }"

}


object SquareTileMain {
  def main(args: Array[String]): Unit = {
    val mapLoader = new ScalaMapLoader(HardcodedConfig.EDUKE32PATH)
    val map = new SquareTileMain(Filename).run(mapLoader)
    ExpUtil.deployMap(map)
  }

  val Filename = "cptest3.map"

  val MapWidth = DMap.MAX_X - DMap.MIN_X
  val MapHeight = DMap.MAX_Y - DMap.MIN_Y

  val DefaultCellSize = 5 * 1025

  // the smallest meaningful grid is 2x2, so we need to divide by at least 2.
  // however, grid alignment could waste up to a full grid cell, so add +1
  // and use 4 instead of 3 because 4 is a power of 2, and because a 3x3 grid in the minimum grid to have
  // a cell surrounded by cells on all sides
  val MaxCellSize = MapWidth / 4 // smallest meaningful grid is 2x2, divide by 3 to account for space lost by alignment
  val MinCellSize = 32 // should be the size of the smallest grid in the Build Editor

  /**
    * A sector group that is "compatible" with this builder fits completely inside the boundary box defined by
    * its ordinal connectors, OR has an ordinal connector in only one direction.
    *
    * @param sg
    * @return
    */
  def isCompatible(sg: SectorGroup, cellSideLength: Int): Boolean = {
    // 1. filter to all groups with opposing ordinal connectors (use the pair that is farthest apart)
    // 2. and the bounding box of the group must be inside the connectors

    // TODO - need to measure the farthest connectors, to make sure sg isn't sticking past
    sg.boundingBox.fitsInside(cellSideLength, cellSideLength)
  }

  /** find the side length of the most common square-shaped bounding box that is drawn based on assuming the ordinal
    * connectors are on the outside.
    */
  def guessCellSize(groups: Iterable[SectorGroupPiece]): Option[Int] = {
    val s = FuncUtils.histo(groups.flatMap(_.cellSize)).maxByOption(_._2).map(_._1)
    s.foreach{ size =>
      SpriteLogicException.throwIf(size < MinCellSize, s"grid cell size must be >= ${MinCellSize}")
      SpriteLogicException.throwIf(size > MaxCellSize, s"grid cell size must be <= ${MaxCellSize}")
    }
    s
  }

  // figure out where to put the origin so that it will line up with the "stay" sector groups.
  // it uses the leftmost and topmost ordinal connector.
  def guessAlignment(gridArea: BoundingBox, conns: Traversable[RedwallConnector], cellSize: Int): PointXY = {
    require(cellSize >= MinCellSize)
    val minX = gridArea.xMin
    val minY = gridArea.yMin
    if(conns.exists(conn => !gridArea.contains(conn.getAnchorPoint.asXY))){
      throw new IllegalArgumentException("one of the given connectors is outside the grid area")
    }
    val ewConns = conns.filter(c => c.isEast || c.isWest).map(_.getAnchorPoint.x)
    val nsConns = conns.filter(c => c.isNorth || c.isSouth).map(_.getAnchorPoint.y)
    val xalign = ewConns.maxByOption(_ * -1).getOrElse(minX)
    val yalign = nsConns.maxByOption(_ * -1).getOrElse(minY)
    require(xalign >= minX && yalign >= minY)

    // handle mod differently due the negative coords
    def modX(x: Int): Int = ((0 - minX) + x) % cellSize + minX
    def modY(y: Int): Int = ((0 - minY) + y) % cellSize + minY

    new PointXY(modX(xalign), modY(yalign))
  }

  def guessGridParams(gridArea: BoundingBox, palette: PrefabPalette, stays: Traversable[PastedSectorGroup]): GridParams2D = {
    val cellSize = SquareTileMain.guessCellSize(palette.allSectorGroups().asScala.map(sg => new SectorGroupPiece(sg))).getOrElse{
      println("unable to guess cell size")
      SquareTileMain.DefaultCellSize
    }
    println(s"Using cell size ${cellSize} (${cellSize/1024} * 1024)")
    val alignXY = SquareTileMain.guessAlignment(gridArea, stays.flatMap(_.unlinkedRedwallConnectors), cellSize)
    GridParams2D.fillMap(gridArea, alignXY, cellSize)
  }

  // TODO - make these fully private
  private[experiments] def snapH(sg: SectorGroup, dest: BoundingBox): Int = {
    (CompassWriter.east(sg).isDefined, CompassWriter.west(sg).isDefined) match {
      case (_, true) => dest.xMin  // align left
      case (true, false) => dest.xMin + (dest.w - sg.boundingBox.w) // align right
      case (false, false) => (dest.xMin + dest.w/2) - sg.boundingBox.w/2 // align center
    }
  }

  private[experiments] def snapV(sg: SectorGroup, dest: BoundingBox): Int = {
    (CompassWriter.north(sg).isDefined, CompassWriter.south(sg).isDefined) match {
      case (true, _) => dest.yMin // align top
      case (false, true) => dest.yMin + (dest.h - sg.boundingBox.h) // align bottom
      case (false, false) => (dest.yMin + dest.h/2) - sg.boundingBox.h/2 // align center
    }
  }

  /**
    *
    * @param sg
    * @param bb
    * @return the absolute (not relative to BBox) point to place the sector group so it will be properly aligned to
    *         the cell.
    */
  def alignToBox(sg: SectorGroup, bb: BoundingBox): PointXY = {
    require(sg.boundingBox.fitsInsideBox(bb))
    new PointXY(snapH(sg, bb), snapV(sg, bb))
  }
}


object TilePainter {
  def describeAvailConnectors(
    grid: scala.collection.immutable.Map[Cell2D, GridPiece],
    cell: Cell2D,
    topLeftBorder: Cell2D, //exclusive
    bottomRightBorder: Cell2D // exclusive
  ): GridPiece = {
    def readBorder(heading: Int): Option[Int] = heading match {
      case Heading.E => if(bottomRightBorder.x == cell.x + 1){ Some(Side.Blocked) }else{ None }
      case Heading.S => if(bottomRightBorder.y == cell.y + 1){ Some(Side.Blocked) }else{ None }
      case Heading.W => if(topLeftBorder.x == cell.x - 1){ Some(Side.Blocked) }else{ None }
      case Heading.N => if(topLeftBorder.y == cell.y - 1){ Some(Side.Blocked) }else{ None }
      case _ => ???
    }

    def readSide(heading: Int): Int = {
      readBorder(heading).orElse{
        val ncell = cell.moveTowards(heading)
        grid.get(ncell).map { neighboorTile =>
          neighboorTile.side(GridUtil.heading(ncell, cell).get)
        }
      }.getOrElse(Side.Unknown)
    }
    SimpleGridPiece(
      readSide(Heading.E),
      readSide(Heading.S),
      readSide(Heading.W),
      readSide(Heading.N),
    )
  }

  def describeAvailConnectors(
    grid: scala.collection.immutable.Map[Cell2D, GridPiece],
    cell: Cell2D,
    params: GridParams2D,
  ): GridPiece = describeAvailConnectors(grid, cell, Cell2D(-1, -1), Cell2D(params.cellCountX, params.cellCountY) )

  def singleSituation(grid: Map[Cell2D, GridPiece], cell: Cell2D, matchTile: GridPiece): Boolean = matchTile.sidesWithConnectors == 1 && {
    val h = Heading.all.asScala.find(h => matchTile.side(h) == Side.Conn).get
    require(Heading.all.contains(h))
    grid.get(cell.moveTowards(h)).get.gridPieceType == GridPiece.Single
  }

  /**
    * this results in a grid full of subgraphs that are disconnected from each other.
    */
  def paintRandomTiles(
    writer: MapWriter,
    gridParams: GridParams2D,
    tiles: Iterable[SectorGroupPiece],
    startingGrid: Map[Cell2D, GridPiece],
    copyTracker: MaxCopyTracker2
  ): scala.collection.immutable.Map[Cell2D, GridPiece] = {
    // val topLeft = Cell2D(-1, -1)
    // val bottomRight = Cell2D(gridParams.cellCountX, gridParams.cellCountY)

    val grid = scala.collection.mutable.Map[Cell2D, GridPiece](startingGrid.toSeq: _*)

    gridParams.allCells.map(Cell2D(_)).foreach { cell =>
      // val matchTile = describeAvailConnectors(grid.toMap, cell, topLeft, bottomRight)
      val matchTile = describeAvailConnectors(grid.toMap, cell, gridParams)

      if(grid.get(cell).isEmpty){

        val tiles1 = tiles.filter(copyTracker.canPasteTile)
        val tiles2 = writer.randomShuffle(tiles1.filter(t => t.sidesWithConnectors >= matchTile.sidesWithConnectors && t.sidesWithConnectors <= matchTile.maxConnectors))
        val tiles3 = if(gridParams.isCorner(cell)){ tiles2.filter(_.gridPieceType == GridPiece.Corner)}else{ tiles2 }
        val tiles4 = if(singleSituation(grid.toMap, cell, matchTile)){
          tiles3.filter(_.gridPieceType != GridPiece.Single)
        }else{tiles3}
        val tile = tiles4.find(t => writer.canFitSectors(t.sg) && t.couldMatch(matchTile))

        // TODO if you are looking for how to auto rotate when pasting & linking, see `Placement`
        tile.flatMap(_.rotateToMatch(matchTile)).foreach { t =>
          grid.put(cell, t)
          copyTracker.record(t.getSg.get)
        }
      }
    }
    grid.toMap
  }


  def paintStrategy1(
    input: GridBuilderInput,
    maxCopyHint: MaxCopyHint,
    writer: MapWriter,
    gridParams: GridParams2D, allTiles: Iterable[SectorGroupPiece]): scala.collection.immutable.Map[Cell2D, GridPiece] = {

    val copyTracker = new MaxCopyTracker2()

    // 1. choose start and end
    // start and end cannot share an axis
    // manhattan distance must be greater than 4  (total size / 4)
    val playerStart = Cell2D(writer.randomElement(gridParams.allCells))
    val playerStartMatch = describeAvailConnectors(Map.empty, playerStart, gridParams)
    val startTile = writer.randomElement(
      allTiles.filter(_.sg.hasPlayerStart).filter(copyTracker.canPasteTile).flatMap(t => t.rotateToMatch(playerStartMatch))
    )
    copyTracker.record(startTile.getSg.get)

    val endPos = Cell2D(writer.randomElement(gridParams.allCells.filter{
      case (x, y) => (x != playerStart.x && y != playerStart.y && playerStart.manhattanDist(Cell2D(x,y)) > input.startEndDist)
    }))
    val endMatch = describeAvailConnectors(Map.empty, endPos, gridParams)
    val endTile = writer.randomElement(
      allTiles.filter(_.sg.hasEndGame).filter(copyTracker.canPasteTile).flatMap(t => t.rotateToMatch(endMatch))
    )
    copyTracker.record(endTile.getSg.get)

    // 2. fill in the rest
    val startGrid = Map(playerStart -> startTile, endPos -> endTile)
    val tiles = allTiles.filterNot(_.sg.hasEndGame)

    val pass1 = TilePainter.paintRandomTiles(writer, gridParams, tiles, startGrid, copyTracker)
    val grid = scala.collection.mutable.Map(pass1.toSeq: _*)
    maxCopyHint.assertMaxCopies(grid.values.map(_.getSg.get))

    // 3. connect the separate pieces
    val components = TilePainter.connectedComponents(grid.toMap)// .filterNot(_.size == 1)
    val results = mergeComponents(components, grid,  tiles, writer, copyTracker, playerStart)
    require(results.size == 1)


    // TODO:  4. fill in gaps with singles!


    // TODO - known bug
    // maxCopyHint.assertMaxCopies(grid.values.map(_.getSg.get))

    println(copyTracker)
    println(s"player start: ${playerStart}")
    println(s"end: ${endPos}")

    grid.toMap
  }

  def forceConnection(
    rand: EntropyProvider,
    cell1: Cell2D,
    cell2: Cell2D,
    grid: scala.collection.mutable.Map[Cell2D, GridPiece],
    tiles: Iterable[SectorGroupPiece],
    copyTracker: MaxCopyTracker2,
  ): Unit = {
    require(GridUtil.isAdj(cell1, cell2))

    val existing1 = grid(cell1)
    val existing2 = grid(cell2)
    copyTracker.record(existing1.getSg.get, -1)
    copyTracker.record(existing2.getSg.get, -1)
    val (match1, match2) = GridPiece.connectedMatchPiecees(existing1, cell1, existing2, cell2).get

    val matches1 = tiles.filter(copyTracker.canPasteTile).flatMap(_.rotateToMatch(match1))
    require(matches1.nonEmpty, s"cant find any tile to match: ${match1} # tiles = ${tiles.size}")
    val tile1 = rand.randomElement(matches1)
    copyTracker.record(tile1.getSg.get, -1)

    val matches2 = tiles.filter(copyTracker.canPasteTile).flatMap(_.rotateToMatch(match2))
    require(matches2.nonEmpty, s"cant find any tile to match: ${match2}")
    val tile2 = rand.randomElement(matches2)
    copyTracker.record(tile2.getSg.get, -1)

    // immutable version: grid + (cell1 -> tile1) + (cell2 -> tile2)
    grid.put(cell1, tile1)
    grid.put(cell2, tile2)
  }

  def mergeComponents(
    components: Seq[Set[Cell2D]],
    grid: scala.collection.mutable.Map[Cell2D, GridPiece],
    tiles: Iterable[SectorGroupPiece],
    writer: MapWriter,
    copyTracker: MaxCopyTracker2,
    playerStart: Cell2D
  ): Seq[Set[Cell2D]] = {
    require(components.size > 0)
    if(components.size == 1){
      components
    }else{
      val first::tail = writer.randomShuffle(components)
      var second: Option[Set[Cell2D]] = None
      var adjacent: Option[Set[(Cell2D, Cell2D)]] = None

      val tail2 = scala.collection.mutable.ArrayBuffer[Set[Cell2D]]()

      for(i <- 0 until tail.size){ // basically a partitionFirst
        if(second.isEmpty){
          val adj = allAdjacent(first, tail(i)).filterNot{
            case (cell1, cell2) => Seq(cell1, cell2).exists{ cell =>
              grid(cell).getSg.map(_.hasEndGame).getOrElse(false) || cell == playerStart
            }
          }
          if(adj.size > 0){
            second = Some(tail(i))
            adjacent = Some(adj)
          }else{
            tail2 += tail(i)
          }
        }else{
          tail2 += tail(i)
        }
      }

      // /B\
      // C+C
      // \C/
      val newComponents: Seq[Set[Cell2D]] = if(second.isEmpty){
        println("WARNING:  UNABLE TO MERGE COMPONENTS")
        tail2 ++ Seq.empty // just keep going
      }else{
        // pick one of the adjacent ones at random, and change the tiles
        val (cell1, cell2) = writer.randomElement(adjacent.get)
        forceConnection(writer, cell1, cell2, grid, tiles, copyTracker)
        val merged: Set[Cell2D] = first ++ second.get
        val newComponents: Seq[Set[Cell2D]] = Seq(merged) ++ tail2
        newComponents
      }
      require(newComponents.size == components.size - 1)
      val newComponents2 = Seq(newComponents: _*) // having problems with ArrayBuffer, need to force to immutable
      mergeComponents(newComponents2, grid, tiles, writer, copyTracker, playerStart)
    }
  }

  /** find all the pairs of cells that are adjacent to each other */
  def allAdjacent(set1: Iterable[Cell2D], set2: Iterable[Cell2D]): Set[(Cell2D, Cell2D)] = {
    set1.flatMap { left =>
      set2.flatMap { right =>
        if(GridUtil.isAdj(left.toSeq, right.toSeq)){
          Some((left, right))
        }else{
          None
        }
      }
    }.toSet
  }

  /**
    * takes one or more unconnected graphs, splits them into connected components (sub graphs) and returns them
    * as a set
    * @param grid the graph, where each cell coordinate is a node, and edges are defined by matching GridPiece conns.
    * @return set of connected components (a connected component is itself a set of Cell2D's)
    */
  def connectedComponents(grid: Map[Cell2D, GridPiece]): Seq[Set[Cell2D]] = {
    if(grid.isEmpty){
      return Seq.empty
    }
    def pop[T](set: scala.collection.mutable.Set[T]): T = {
      val t = set.head
      set -= t
      t
    }

    val closedList = scala.collection.mutable.Set[Cell2D]()
    val openList = scala.collection.mutable.Set[Cell2D]()
    openList += grid.keys.head

    while(openList.size > 0){
      val current = pop(openList)
      if(!closedList.contains(current)){
        closedList.add(current)
        val neighboors = GridUtil.neighboors(current).map(n => (n -> grid.get(n))).collect {
          case (loc, Some(piece)) => (loc, piece)
        }
        openList ++= neighboors.collect {
          case (nloc, n) if(GridPiece.connected(grid(current), current, n, nloc)) => nloc
        }.filterNot(closedList.contains)
      }
    }
    Seq(closedList.toSet) ++ connectedComponents(grid -- closedList)
  }

}

/**
  * Places Sector Groups in a 2D Grid of square tiles, and attempts to align ordinal connectors to grid cell walls.
  *
  * // TODO: these aren't really tiles, because we don't yet support having connectors on every outside wall.
  * // TODO:  make a version of this that accepts "Hallways" (so the grid has padding)
  *
  * @param Filename
  */
class SquareTileMain(
  val Filename: String,
  gridBuilderInput: GridBuilderInput = GridBuilderInput.defaultInput
) {


  // TODO - things like which tile number is end sprite, or which SE lotags cant be rotated, should be part of
  // some kind of "game options" to make it easier to support other games in the future.
  def run(mapLoader: ScalaMapLoader): trn.Map = {
    val sourceMap = mapLoader.load(Filename)
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true)
    val maxCopyHint = MaxCopyHint.fromPalette(palette)
    val writer = MapWriter(DukeConfig.loadHardCodedVersion())

    val stays = writer.pasteStays(palette)

    val gridParams = SquareTileMain.guessGridParams(BuildConstants.MapBounds, palette, stays).withMaxCellCounts(
      gridBuilderInput.maxCellsX,
      gridBuilderInput.maxCellsY
    )

    // mark off all locations already occupied by a "stay" sector group
    stays.foreach { staySg =>
      gridParams.cellsIntersectedBy(staySg.boundingBox).foreach { cell =>
        // TODO - need to map this into tiles!
        // grid.put(cell, staySg)
      }
    }

    // figure out which sector groups will fit in the grid
    val availableTiles = palette.allSectorGroups().asScala.filter(SquareTileMain.isCompatible(_, gridParams.sideLength))
      .map(new SectorGroupPiece(_)).filterNot(_.gridPieceType == GridPiece.Orphan)

    require(availableTiles.find(_.sg.sectorGroupId == 24).get.sg.hints.maxCopies.get == 1)

    val grid = new Grid2D(gridParams)

    val tilesToPaint = TilePainter.paintStrategy1(gridBuilderInput, maxCopyHint, writer, gridParams, availableTiles)

    tilesToPaint.foreach { case (cell, tile) =>
      grid.pasteToCell(writer, cell.x, cell.y, tile.getSg.get)
    }

    grid.allPsg.foreach { psg =>
      val enemyTags = psg.allSprites.filter(s => Marker.isMarker(s) && s.getLotag == PrefabUtils.MarkerSpriteLoTags.ENEMY)
      if(enemyTags.nonEmpty){
        println("printing enemy")
        val s = writer.randomElement(enemyTags)
        // TODO - textures should be part of a "GameInput" object that lists textures for enemies
        s.setTexture(TextureList.Enemies.LIZTROOP)
        s.setLotag(0)
        s.setHiTag(0)
      }
    }

    writer.sgBuilder.autoLinkRedwalls()
    writer.setAnyPlayerStart(force = true)
    writer.sgBuilder.clearMarkers()
    writer.checkSectorCount()
    writer.outMap
  }

}
