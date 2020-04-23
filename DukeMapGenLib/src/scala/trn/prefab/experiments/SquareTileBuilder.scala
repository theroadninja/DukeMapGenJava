package trn.prefab.experiments
import trn.{FuncUtils, MapLoader, MapUtil, PointXY, Map => DMap}
import trn.prefab.{BoundingBox, Heading, MapWriter, Matrix2D, PastedSectorGroup, PrefabPalette, RedwallConnector, SectorGroup, SpriteLogicException}
import trn.FuncImplicits._
import trn.prefab.grid2d.{GridPiece, SectorGroupPiece, Side, SimpleGridPiece}

import scala.collection.JavaConverters._
import scala.collection.immutable

/**
  * @param maxCellsX maximum width of grid in number of cells
  * @param maxCellsY maximum height of grid in number of cells
  */
case class GridBuilderInput(
  maxCellsX: Int = 8,
  maxCellsY: Int = 8,
)


object Cell2D {
  def apply(t: (Int, Int)): Cell2D = Cell2D(t._1, t._2)
}

case class Cell2D(x: Int, y: Int){
  lazy val asTuple: (Int, Int) = (x, y)

  def moveTowards(heading: Int): Cell2D = heading match {
    case Heading.E => Cell2D(x + 1, y)
    case Heading.W => Cell2D(x - 1, y)
    case Heading.N => Cell2D(x, y - 1)
    case Heading.S => Cell2D(x, y + 1)
    case _ => throw new RuntimeException(s"invalid heading ${heading}")
  }
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

  def corners: Seq[(Int, Int)] = params.cornerCells
}


object SquareTileBuilder {

  val TestFile1 = GridExperiment.Filename

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
    val cellSize = SquareTileBuilder.guessCellSize(palette.allSectorGroups().asScala.map(sg => new SectorGroupPiece(sg))).getOrElse{
      println("unable to guess cell size")
      SquareTileBuilder.DefaultCellSize
    }
    println(s"Using cell size ${cellSize} (${cellSize/1024} * 1024)")
    val alignXY = SquareTileBuilder.guessAlignment(gridArea, stays.flatMap(_.unlinkedRedwallConnectors), cellSize)
    GridParams2D.fillMap(gridArea, alignXY, cellSize)
  }

  // TODO - make these fully private
  private[experiments] def snapH(sg: SectorGroup, dest: BoundingBox): Int = {
    (MapWriter.east(sg).isDefined, MapWriter.west(sg).isDefined) match {
      case (_, true) => dest.xMin  // align left
      case (true, false) => dest.xMin + (dest.w - sg.boundingBox.w) // align right
      case (false, false) => (dest.xMin + dest.w/2) - sg.boundingBox.w/2 // align center
    }
  }

  private[experiments] def snapV(sg: SectorGroup, dest: BoundingBox): Int = {
    (MapWriter.north(sg).isDefined, MapWriter.south(sg).isDefined) match {
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
  def describeAvailConnectors(grid: scala.collection.immutable.Map[Cell2D, GridPiece], cell: Cell2D): GridPiece = {
    def readSide(heading: Int): Int = {
      val ncell = cell.moveTowards(heading)
      grid.get(ncell).map { neighboorTile =>
        neighboorTile.side(GridUtil.heading(ncell, cell).get)
      }.getOrElse(Side.Unknown)
    }
    SimpleGridPiece(
      readSide(Heading.E),
      readSide(Heading.S),
      readSide(Heading.W),
      readSide(Heading.N),
    )
  }

  def paintTiles(writer: MapWriter, gridParams: GridParams2D, tiles: Iterable[SectorGroupPiece]): scala.collection.immutable.Map[Cell2D, GridPiece] = {
    val cornerTiles = tiles.filter(_.gridPieceType == GridPiece.Corner)

    val grid = scala.collection.mutable.Map[Cell2D, GridPiece]()

    gridParams.cornerCells.map(Cell2D(_)).foreach { case cell =>
      val tile = writer.randomElement(cornerTiles)
      if(grid.get(cell).isEmpty && (writer.canFitSectors(tile.sg))){
        val matchTile = GridPiece.withBlockedSides(gridParams.adjacentEdgeHeadings(cell.x, cell.y))
        tile.rotateToMatch(matchTile).map { sg2 => grid.put(cell, sg2) }
      }
    }

    // return grid.toMap

    val borderTiles = tiles.filter(_.sidesWithConnectors < 4).filterNot(_.gridPieceType == GridPiece.Orphan)
    val border = gridParams.borderCells.toSet.diff(gridParams.cornerCells.toSet).map(Cell2D(_))
    border.foreach { case cell =>
      val tile = writer.randomElement(borderTiles)
      if(grid.get(cell).isEmpty && writer.canFitSectors(tile.sg)){

        val matchTile = GridPiece.withBlockedSides(gridParams.adjacentEdgeHeadings(cell.x, cell.y))
        tile.rotateToMatch(matchTile).map { sg2 => grid.put(cell, sg2) }
      }
    }

    val inner = gridParams.allCells.diff(gridParams.borderCells)
    inner.foreach { case (cellx, celly) =>
      val neighboorConns = GridUtil.neighboors(cellx, celly).flatMap { n=>
        val neighboorCell = Cell2D(n)
        grid.get(neighboorCell) match {
          case Some(tile) => {
            GridUtil.heading(neighboorCell.x, neighboorCell.y, cellx, celly).flatMap{ h =>
              if(tile.side(h) == Side.Conn){
                Some(neighboorCell, 1)
              }else{
                None
              }
            }
          }
          case None => None
        }
      }
      require(neighboorConns.size >= 0 && neighboorConns.size < 5)

      val matchTile = TilePainter.describeAvailConnectors(grid.toMap, Cell2D(cellx, celly))

      val tiles2 = writer.randomShuffle(tiles.filter(_.sidesWithConnectors >= neighboorConns.size))
      if(grid.get(Cell2D(cellx, celly)).isEmpty){
        val tile = tiles2.find{p: SectorGroupPiece => writer.canFitSectors(p.sg) && p.couldMatch(matchTile)}
        tile.flatMap(_.rotateToMatch(matchTile)).foreach { t =>
          grid.put(Cell2D(cellx, celly), t)
        }
      }
    }

    grid.toMap
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
class SquareTileBuilder(
  val Filename: String,
  gridBuilderInput: GridBuilderInput = GridBuilderInput(),
) extends PrefabExperiment {

  // TODO - things like which tile number is end sprite, or which SE lotags cant be rotated, should be part of
  // some kind of "game options" to make it easier to support other games in the future.
  override def run(mapLoader: MapLoader): trn.Map = {
    val sourceMap = mapLoader.load(Filename)
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true)
    val writer = MapWriter()

    val stays = writer.pasteStays(palette)

    val gridParams = SquareTileBuilder.guessGridParams(MapWriter.MapBounds, palette, stays).withMaxCellCounts(
      gridBuilderInput.maxCellsX,
      gridBuilderInput.maxCellsY
    )
    // val gridParams2 = gridParams.copy(
    //   cellCountX = Math.min(gridParams.cellCountX, gridBuilderInput.maxCellsX),
    //   cellCountY = Math.min(gridParams.cellCountY, gridBuilderInput.maxCellsY),
    // )
    val grid = new Grid2D(gridParams)

    // mark off all locations already occupied by a "stay" sector group
    stays.foreach { staySg =>
      gridParams.cellsIntersectedBy(staySg.boundingBox).foreach { cell =>
        grid.put(cell, staySg)
      }
    }


    // figure out which sector groups will fit in the grid
    val availableTiles = palette.allSectorGroups().asScala.filter(SquareTileBuilder.isCompatible(_, gridParams.sideLength))
      .map(new SectorGroupPiece(_)).filterNot(_.gridPieceType == GridPiece.Orphan)

    def pasteToCell(cellx: Int, celly: Int, sg: SectorGroup): Option[PastedSectorGroup] = {
      val sg2 = Some(sg) // TODO clean up
      sg2.map { sg3 =>
        val cellBox = grid.params.cellBoundingBox(cellx, celly)
        val target = SquareTileBuilder.alignToBox(sg3, cellBox)
        val psg = writer.builder.pasteSectorGroupAt(sg3, target.withZ(0))
        grid.put((cellx, celly), psg)
        psg
      }
    }

    val b = (1 == 1)
    if(b){
      val tilesToPaint = TilePainter.paintTiles(writer, gridParams, availableTiles)
      tilesToPaint.foreach { case (cell, tile) =>
        pasteToCell(cell.x, cell.y, tile.getSg.get)
      }
    }else{
      oldMethod(writer, grid, availableTiles)
    }

    writer.sgBuilder.autoLinkRedwalls()
    writer.builder.setAnyPlayerStart(force = true)
    writer.sgBuilder.clearMarkers()
    writer.checkSectorCount()
    writer.outMap
  }

  def oldMethod(writer: MapWriter, grid: Grid2D, availableTiles: Iterable[SectorGroupPiece]): Unit = {

    def pasteToCell(cellx: Int, celly: Int, sg: SectorGroup): Option[PastedSectorGroup] = {
      val sg2 = Some(sg) // TODO clean up
      sg2.map { sg3 =>
        val cellBox = grid.params.cellBoundingBox(cellx, celly)
        val target = SquareTileBuilder.alignToBox(sg3, cellBox)
        val psg = writer.builder.pasteSectorGroupAt(sg3, target.withZ(0))
        grid.put((cellx, celly), psg)
        psg
      }
    }
    /**
      * Creates a GridPiece with sides that match the connectors available in neighboors of `cell`
      */
    def describeAvailConnectors(cell: Cell2D): GridPiece = {
      def readSide(heading: Int): Int = {
        val ncell = cell.moveTowards(heading)
        grid.get(ncell.asTuple).map { psg =>
          val conn = GridUtil.heading(ncell, cell).flatMap { h => MapWriter.firstConnWithHeading(psg, h)}
          conn.map(_ => Side.Conn).getOrElse(Side.Blocked)
        }.getOrElse(Side.Unknown)
      }
      SimpleGridPiece(
        readSide(Heading.E),
        readSide(Heading.S),
        readSide(Heading.W),
        readSide(Heading.N),
      )
    }

    val cornerTiles = availableTiles.filter(_.gridPieceType == GridPiece.Corner)

    // 1. corners
    grid.params.cornerCells.foreach { case (cellx, celly) =>
      val tile = writer.randomElement(cornerTiles)
      if(grid.get((cellx, celly)).isEmpty && (writer.canFitSectors(tile.sg))){
        val matchTile = GridPiece.withBlockedSides(grid.params.adjacentEdgeHeadings(cellx, celly))
        tile.rotateToMatch(matchTile).map { sg2 =>
          pasteToCell(cellx, celly, sg2.getSg.get)
        }

      }
    }

    // 2. borders
    // val borderSgs = availableSgs.filter(sg => sidesWithConnectors(sg) < 4).filterNot(gridPieceType(_) == GridPiece.Orphan)
    val borderTiles = availableTiles.filter(_.sidesWithConnectors < 4).filterNot(_.gridPieceType == GridPiece.Orphan)
    val border = grid.params.borderCells.toSet.diff(grid.params.cornerCells.toSet)
    border.foreach { case (cellx, celly) =>
      val tile = writer.randomElement(borderTiles)
      if(grid.get((cellx, celly)).isEmpty && writer.canFitSectors(tile.sg)){

        val matchTile = GridPiece.withBlockedSides(grid.params.adjacentEdgeHeadings(cellx, celly))
        tile.rotateToMatch(matchTile).map { sg2 =>
          pasteToCell(cellx, celly, sg2.getSg.get)
        }
      }
    }

    // 3. interior
    val inner = grid.params.allCells.diff(grid.params.borderCells)
    inner.foreach { case (cellx, celly) =>
      val neighboorConns = GridUtil.neighboors(cellx, celly).flatMap { neighboorCell =>
        grid.get(neighboorCell) match {
          case Some(psg) => {
            GridUtil.heading(neighboorCell._1, neighboorCell._2, cellx, celly).flatMap{ h =>
              MapWriter.firstConnWithHeading(psg, h).map(conn => (neighboorCell, conn))
            }
          }
          case None => None
        }
      }
      require(neighboorConns.size >= 0 && neighboorConns.size < 5)

      val matchTile = describeAvailConnectors(Cell2D(cellx, celly))

      val tiles = writer.randomShuffle(availableTiles.filter(_.sidesWithConnectors >= neighboorConns.size))
      if(grid.get((cellx, celly)).isEmpty){
        val tile = tiles.find{p: SectorGroupPiece => writer.canFitSectors(p.sg) && p.couldMatch(matchTile)}
        tile.flatMap(_.rotateToMatch(matchTile)).foreach { t => pasteToCell(cellx, celly, t.getSg.get) }
      }
    }

  }

}
