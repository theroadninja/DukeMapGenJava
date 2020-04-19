package trn.prefab.experiments

import org.junit.{Assert, Test}
import trn.prefab.grid2d.SectorGroupPiece
import trn.{PointXY, PointXYZ, Map => DMap}
import trn.prefab.{BoundingBox, Heading, MapWriter, PrefabPalette, RedwallConnector, SectorGroup, TestUtils, UnitTestBuilder}

class SquareTileBuilderTests {

  @Test
  def testGuessCellSize(): Unit = {
    val testPalette: PrefabPalette = PrefabPalette.fromMap(TestUtils.load(TestUtils.MapWriterMap), true)
    def sg(i: Int): SectorGroup = testPalette.getSG(i)
    def p(sg: SectorGroup): SectorGroupPiece = new SectorGroupPiece(sg)

    Seq(1,2,3,4,5,6,7,8,9).foreach { groupId =>
      Assert.assertEquals(None, SquareTileBuilder.guessCellSize(Seq(p(sg(groupId)))))
    }
    Assert.assertEquals(Some(5 * 1024), SquareTileBuilder.guessCellSize(Seq(p(sg(10)))))
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

    def align(bb: BoundingBox, groups: (SectorGroup, PointXYZ) *): PointXY = SquareTileBuilder.guessAlignment(bb, stayConns(groups), CellSize)

    val topLeftXY = new PointXY(DMap.MIN_X, DMap.MIN_Y)
    val topLeftXYZ = topLeftXY.withZ(0)
    val gridArea = BoundingBox(0, 0, 8192, 8192)
    val MapBounds = MapWriter.MapBounds

    Assert.assertEquals(new PointXY(DMap.MIN_X, DMap.MIN_Y), SquareTileBuilder.guessAlignment(MapWriter.MapBounds, Seq(), 1024))
    Assert.assertEquals(new PointXY(0, 0), SquareTileBuilder.guessAlignment(gridArea, Seq(), 1024))

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

}
