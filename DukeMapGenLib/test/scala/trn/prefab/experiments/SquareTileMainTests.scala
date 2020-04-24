package trn.prefab.experiments

import org.junit.{Assert, Test}
import trn.prefab.grid2d.{GridPiece, SectorGroupPiece, Side, SimpleGridPiece}
import trn.{PointXY, PointXYZ, Map => DMap}
import trn.prefab.{BoundingBox, Heading, MapWriter, PrefabPalette, RedwallConnector, SectorGroup, TestUtils, UnitTestBuilder}

class SquareTileMainTests {
  def c(x: Int, y: Int): Cell2D = Cell2D(x, y)
  def sgp(e: Int, s: Int, w: Int, n: Int) = SimpleGridPiece(e, s, w, n)
  val C = Side.Conn
  val B = Side.Blocked

  @Test
  def testGuessCellSize(): Unit = {
    val testPalette: PrefabPalette = PrefabPalette.fromMap(TestUtils.load(TestUtils.MapWriterMap), true)
    def sg(i: Int): SectorGroup = testPalette.getSG(i)
    def p(sg: SectorGroup): SectorGroupPiece = new SectorGroupPiece(sg)

    Seq(1,2,3,4,5,6,7,8,9).foreach { groupId =>
      Assert.assertEquals(None, SquareTileMain.guessCellSize(Seq(p(sg(groupId)))))
    }
    Assert.assertEquals(Some(5 * 1024), SquareTileMain.guessCellSize(Seq(p(sg(10)))))
    // TODO - more test cases here; I've only written the ones that I get for free by re-using the MapWriter test file
  }

  @Test
  def testGuessAlignment(): Unit = {
    val palette = PrefabPalette.fromMap(TestUtils.load(TestUtils.MapWriterMap), true)
    val sgWest = palette.getSG(6) // 3x3 (*1024) cube with West connector on left side
    val sgNorth = palette.getSG(4) // 3x3 (*1024) cube with West connector on top
    val CellSize = 3 * 1024

    // pretend that ALL pasted sector groups are "stay" sector groups, and return unliked connections
    def stayConns(groups: Seq[(SectorGroup, PointXYZ)]): Seq[RedwallConnector] = {
      val builder = UnitTestBuilder()
      groups.foreach {
        // this uses top left as the anchor
        case (group, loc) => builder.pasteSectorGroupAt(group, loc)
      }
      builder.pastedSectorGroups.flatMap(_.unlinkedRedwallConnectors)
    }

    def align(bb: BoundingBox, groups: (SectorGroup, PointXYZ) *): PointXY = SquareTileMain.guessAlignment(bb, stayConns(groups), CellSize)

    val topLeftXY = new PointXY(DMap.MIN_X, DMap.MIN_Y)
    val topLeftXYZ = topLeftXY.withZ(0)
    val gridArea = BoundingBox(0, 0, 8192, 8192)
    val MapBounds = MapWriter.MapBounds

    Assert.assertEquals(new PointXY(DMap.MIN_X, DMap.MIN_Y), SquareTileMain.guessAlignment(MapWriter.MapBounds, Seq(), 1024))
    Assert.assertEquals(new PointXY(0, 0), SquareTileMain.guessAlignment(gridArea, Seq(), 1024))

    Assert.assertEquals(topLeftXY, align(MapBounds, (sgWest, topLeftXYZ)))
    Assert.assertEquals(
      MapBounds.topLeft,
      align(MapBounds, (sgWest, topLeftXYZ), (sgNorth, new PointXYZ(DMap.MIN_X + 4096, DMap.MIN_Y, 0)))
    )
    Assert.assertEquals(
      PointXY.ZERO,
      align(gridArea, (sgWest, new PointXYZ(0, 0, 0)), (sgNorth, new PointXYZ(1024, 0, 0)))
    )

    Assert.assertEquals(
      new PointXY(DMap.MIN_X + 1, DMap.MIN_Y),
      align(MapBounds, (sgWest, new PointXYZ(DMap.MIN_X + 1, DMap.MIN_Y, 0)), (sgNorth, new PointXYZ(DMap.MIN_X + 4096, DMap.MIN_Y, 0)))
    )
    Assert.assertEquals(
      new PointXY(DMap.MIN_X + 1024, DMap.MIN_Y),
      align(MapBounds, (sgWest, new PointXYZ(DMap.MIN_X + 1024, 0, 0)), (sgNorth, new PointXYZ(DMap.MIN_X + 4096, DMap.MIN_Y, 0)))
    )
    Assert.assertEquals(
      new PointXY(DMap.MIN_X + 37, DMap.MIN_Y),
      align(MapBounds, (sgWest, new PointXYZ(DMap.MIN_X + CellSize + 37, 0, 0)), (sgNorth, new PointXYZ(DMap.MIN_X + 4096, DMap.MIN_Y, 0)))
    )

    Assert.assertEquals(
      new PointXY(DMap.MIN_X, DMap.MIN_Y + 54),
      align(MapBounds, (sgWest, topLeftXYZ), (sgNorth, new PointXYZ(DMap.MIN_X + 4096, DMap.MIN_Y + 54, 0)))
    )
    Assert.assertEquals(
      new PointXY(DMap.MIN_X, DMap.MIN_Y + 69),
      align(MapBounds, (sgWest, topLeftXYZ), (sgNorth, new PointXYZ(DMap.MIN_X + 4096, DMap.MIN_Y + CellSize + 69, 0)))
    )

    Assert.assertEquals(
      new PointXY(0, 69),
      align(gridArea, (sgWest, new PointXYZ(0, 0, 0)), (sgNorth, new PointXYZ(4096, CellSize + 69, 0)))
    )

    // This guy has a connector outside the grid area (because sgWest is set to be top left of the map
    // could to Test(expected = IllegalArgumentException.class) but i'm too lazy to refactor this into a different method
    try{
      Assert.assertEquals(
        new PointXY(0, 69),
        align(gridArea, (sgWest, topLeftXYZ), (sgNorth, new PointXYZ(4096, CellSize + 69, 0)))
      )
      Assert.fail("expected an exception")
    }catch{
      case _: IllegalArgumentException => {}
    }
  }

  @Test
  def testGridParams2D(): Unit = {
    val gridArea = BoundingBox(0, 0, 1024, 1024)
    Assert.assertEquals(GridParams2D(PointXY.ZERO, 32, 32, 32), GridParams2D.fillMap(gridArea, PointXY.ZERO, 32))
    Assert.assertEquals(BoundingBox(0, 0, 1024, 1024), GridParams2D.fillMap(gridArea, PointXY.ZERO, 32).boundingBox)
    Assert.assertEquals(GridParams2D(PointXY.ZERO, 16, 16, 64), GridParams2D.fillMap(gridArea, PointXY.ZERO, 64))
    Assert.assertEquals(GridParams2D(new PointXY(13, 0), 15, 15, 64), GridParams2D.fillMap(gridArea, new PointXY(13, 0), 64))
  }

  @Test
  def cellsIntersectedBy(): Unit = {
    val gp1 = GridParams2D(PointXY.ZERO, 10, 10, 10)

    // throws: Assert.assertTrue(gp1.cellsIntersectedBy(BoundingBox(0, 0, 0, 0)).isEmpty)
    Assert.assertTrue(gp1.cellsIntersectedBy(BoundingBox(-10, -10, 0, 0)).isEmpty)
    // throws: Assert.assertTrue(gp1.cellsIntersectedBy(BoundingBox(100, 100, 100, 100)).isEmpty)
    Assert.assertTrue(gp1.cellsIntersectedBy(BoundingBox(100, 100, 110, 110)).isEmpty)

    Assert.assertEquals(Seq((0, 0)).toSet, gp1.cellsIntersectedBy(BoundingBox(-10, -10, 5, 5)).toSet)
    Assert.assertEquals(Seq((0, 0), (1, 0)).toSet, gp1.cellsIntersectedBy(BoundingBox(-10, -10, 15, 5)).toSet)
    Assert.assertEquals(Seq((0, 0), (0, 1)).toSet, gp1.cellsIntersectedBy(BoundingBox(-10, -10, 5, 15)).toSet)
    Assert.assertEquals(Seq((0, 0), (1, 0), (0, 1), (1, 1)).toSet, gp1.cellsIntersectedBy(BoundingBox(-10, -10, 15, 15)).toSet)


    Assert.assertEquals(Seq((5, 5)).toSet, gp1.cellsIntersectedBy(BoundingBox(50, 50, 60, 60)).toSet)
    Assert.assertEquals(Seq((5, 5)).toSet, gp1.cellsIntersectedBy(BoundingBox(51, 51, 59, 59)).toSet)
    Assert.assertEquals(Seq((4, 5), (5, 5)).toSet, gp1.cellsIntersectedBy(BoundingBox(49, 50, 60, 60)).toSet)
    Assert.assertEquals(Seq((5, 4), (5, 5)).toSet, gp1.cellsIntersectedBy(BoundingBox(50, 49, 60, 60)).toSet)
    Assert.assertEquals(Seq((5, 5), (6, 5)).toSet, gp1.cellsIntersectedBy(BoundingBox(50, 50, 61, 60)).toSet)
    Assert.assertEquals(Seq((5, 5), (5, 6)).toSet, gp1.cellsIntersectedBy(BoundingBox(50, 50, 60, 61)).toSet)

    Assert.assertEquals(Seq((9, 9)).toSet, gp1.cellsIntersectedBy(BoundingBox(90, 90, 100, 100)).toSet)
    Assert.assertEquals(Seq((9, 9)).toSet, gp1.cellsIntersectedBy(BoundingBox(90, 90, 110, 120)).toSet)
  }

  @Test
  def testCellBoundingBox(): Unit = {
    val gp1 = GridParams2D(PointXY.ZERO, 10, 10, 10)
    Assert.assertEquals(BoundingBox(0, 0, 10, 10), gp1.cellBoundingBox(0, 0))
    Assert.assertEquals(BoundingBox(40, 50, 50, 60), gp1.cellBoundingBox(4, 5))

    val gp2 = GridParams2D(new PointXY(5, 6), 10, 10, 10)
    Assert.assertEquals(BoundingBox(5, 6, 15, 16), gp2.cellBoundingBox(0, 0))
    Assert.assertEquals(BoundingBox(45, 56, 55, 66), gp2.cellBoundingBox(4, 5))
  }

  @Test
  def testCornersAndBorders(): Unit = {
    val gp1 = GridParams2D(PointXY.ZERO, 3, 3, 10)
    Assert.assertTrue(Seq((0, 0), (2, 0), (0, 2), (2, 2)).toSet == gp1.cornerCells.toSet)
    Assert.assertTrue(Seq((0, 0), (1, 0), (2, 0), (0, 1), (2, 1), (0, 2), (1, 2), (2, 2)).toSet == gp1.borderCells.toSet)
  }

  @Test
  def testGridCell(): Unit = {
    Assert.assertEquals(Cell2D(1, 0), Cell2D(0, 0).moveTowards(Heading.E))
    Assert.assertEquals(Cell2D(-1, 0), Cell2D(0, 0).moveTowards(Heading.W))
    Assert.assertEquals(Cell2D(0, 1), Cell2D(0, 0).moveTowards(Heading.S))
    Assert.assertEquals(Cell2D(0, -1), Cell2D(0, 0).moveTowards(Heading.N))
  }


  @Test
  def testDescribeAvailConnectors(): Unit = {
    val topLeft = Cell2D(-1, -1)
    val bottomRight = Cell2D(3, 4)
    val U = Side.Unknown
    val blocked = SimpleGridPiece(B, B, B, B)
    val grid0 = Map.empty[Cell2D, SimpleGridPiece]
    val grid1 = Map(
      (0, 0) -> blocked, (1, 0) -> blocked, (2, 0) -> blocked,
      (0, 1) -> blocked,                    (2, 1) -> blocked,
      (0, 2) -> blocked, (1, 2) -> blocked, (2, 2) -> blocked
    ).map{ case (xy, v) => Cell2D(xy) -> v }
    val grid2 = Map(
      (0, 0) -> blocked, (1, 0) -> blocked, (2, 0) -> blocked,
      (0, 1) -> blocked,                    // missing
      (0, 2) -> blocked, (1, 2) -> blocked, (2, 2) -> blocked
    ).map{ case (xy, v) => Cell2D(xy) -> v }

    Assert.assertEquals(SimpleGridPiece(U, U, U, U), TilePainter.describeAvailConnectors(grid0, Cell2D(1, 1), topLeft, bottomRight))
    Assert.assertEquals(blocked, TilePainter.describeAvailConnectors(grid1, Cell2D(1, 1), topLeft, bottomRight))
    Assert.assertEquals(SimpleGridPiece(Side.Unknown, B, B, B), TilePainter.describeAvailConnectors(grid2, Cell2D(1, 1), topLeft, bottomRight))

    val grid3 = Map(
      (0, 0) -> blocked,         (1, 0) -> sgp(U, C, U, U ), (2, 0) -> blocked,
      (0, 1) -> sgp(C, U, U, U),                             (2, 1) -> sgp(U, U, C, U),
      (0, 2) -> blocked,         (1, 2) -> sgp(U, U, U, C), (2, 2) -> blocked
    ).map{ case (xy, v) => Cell2D(xy) -> v }
    Assert.assertEquals(SimpleGridPiece(C, C, C, C), TilePainter.describeAvailConnectors(grid3, Cell2D(1, 1), topLeft, bottomRight))

    val unknown = SimpleGridPiece(U, U, U, U)
    val conns = SimpleGridPiece(C, C, C, C)
    val grid4 = Map(
      (0, 0) -> blocked, (1, 0) -> blocked, (2, 0) -> blocked,
      (0, 1) -> conns,                    (2, 1) -> conns,
      (0, 2) -> blocked, (1, 2) -> unknown, (2, 2) -> blocked
    ).map{ case (xy, v) => Cell2D(xy) -> v }
    Assert.assertEquals(SimpleGridPiece(C, U, C, B), TilePainter.describeAvailConnectors(grid4, Cell2D(1, 1), topLeft, bottomRight))

    // border
    Assert.assertEquals(sgp(U, U, B, B), TilePainter.describeAvailConnectors(grid0, Cell2D(0, 0), topLeft, bottomRight))
    Assert.assertEquals(sgp(U, U, U, B), TilePainter.describeAvailConnectors(grid0, Cell2D(1, 0), topLeft, bottomRight))
    Assert.assertEquals(sgp(B, U, U, B), TilePainter.describeAvailConnectors(grid0, Cell2D(2, 0), topLeft, bottomRight))
    Assert.assertEquals(sgp(B, U, U, U), TilePainter.describeAvailConnectors(grid0, Cell2D(2, 1), topLeft, bottomRight))
    Assert.assertEquals(sgp(B, U, U, U), TilePainter.describeAvailConnectors(grid0, Cell2D(2, 2), topLeft, bottomRight))
    Assert.assertEquals(sgp(B, B, U, U), TilePainter.describeAvailConnectors(grid0, Cell2D(2, 3), topLeft, bottomRight))
    Assert.assertEquals(sgp(U, B, U, U), TilePainter.describeAvailConnectors(grid0, Cell2D(1, 3), topLeft, bottomRight))
    Assert.assertEquals(sgp(U, B, B, U), TilePainter.describeAvailConnectors(grid0, Cell2D(0, 3), topLeft, bottomRight))
    Assert.assertEquals(sgp(U, U, B, U), TilePainter.describeAvailConnectors(grid0, Cell2D(0, 2), topLeft, bottomRight))
    Assert.assertEquals(sgp(U, U, B, U), TilePainter.describeAvailConnectors(grid0, Cell2D(0, 1), topLeft, bottomRight))
  }

  @Test
  def testSingleSituation(): Unit = {
    val U = Side.Unknown
    val grid0 = Map(
      (2, 1) -> sgp(U, U, C, U),
    ).map{ case (xy, v) => Cell2D(xy) -> v }

    val topLeft = Cell2D(-1, -1)
    val bottomRight = Cell2D(3, 4)
    val cell = Cell2D(1, 1)
    val matchTile = TilePainter.describeAvailConnectors(grid0, cell, topLeft, bottomRight)
    Assert.assertTrue(grid0.get(cell.moveTowards(Heading.E)).get.gridPieceType == GridPiece.Single)
    Assert.assertTrue(TilePainter.singleSituation(grid0, cell, matchTile))
  }

  @Test
  def testConnectedComponents(): Unit = {
    Assert.assertTrue(TilePainter.connectedComponents(Map.empty).isEmpty)

    val grid0 = Seq(Cell2D(120, 400) -> sgp(C, C, C, C)).toMap
    Assert.assertTrue(TilePainter.connectedComponents(grid0).size == 1)

    // two pieces, not connected
    val grid1 = Seq(
      Cell2D(5, 5) -> sgp(B, B, B, B),
      Cell2D(6, 5) -> sgp(B, B, B, B),
    ).toMap
    Assert.assertEquals(2, TilePainter.connectedComponents(grid1).size)

    // two pieces, connected
    val grid2 = Seq(
      Cell2D(5, 5) -> sgp(C, B, B, B),
      Cell2D(6, 5) -> sgp(B, B, C, B),
    ).toMap
    Assert.assertEquals(1, TilePainter.connectedComponents(grid2).size)

    // unconnected again
    val grid3 = Seq(
      Cell2D(5, 5) -> sgp(C, B, B, B),
      Cell2D(7, 5) -> sgp(B, B, C, B),
    ).toMap
    Assert.assertEquals(2, TilePainter.connectedComponents(grid3).size)

    // three pieces
    // +  +--+
    // |
    // +--+--+
    //       |
    // +--+  +
    val grid4 = Seq(
      c(0, 0) -> sgp(B, C, C, C), c(1, 0) -> sgp(C, B, B, C), c(2, 0) -> sgp(C, B, C, C),
      c(0, 1) -> sgp(C, B, C, C), c(1, 1) -> sgp(C, B, C, B), c(2, 1) -> sgp(C, C, C, B),
      c(0, 2) -> sgp(C, C, C, B), c(1, 2) -> sgp(B, C, C, B), c(2, 2) -> sgp(C, C, B, C),
    ).toMap
    val results4 = TilePainter.connectedComponents(grid4)
    Assert.assertEquals(3, results4.size)
    Assert.assertTrue(results4.map(_.size).sorted == Seq(2, 2, 5))
  }

  @Test
  def testAllAdjacent(): Unit = {
    Assert.assertTrue(TilePainter.allAdjacent(Seq(c(0, 0), c(0, 1)), Seq(c(2, 1), c(2, 2))) == Seq.empty)
    Assert.assertTrue(TilePainter.allAdjacent(Seq(c(0, 0), c(0, 1)), Seq(c(1, 1), c(1, 2))) == Seq((c(0, 1), c(1, 1))))
  }
}
