package trn.prefab

import java.io.{ByteArrayInputStream, File, FileInputStream}

import org.apache.commons.io.IOUtils
import org.junit.{Assert, Test}
import trn.{AngleUtil, HardcodedConfig, LineSegmentXY, PointXY, Sprite, Wall, WallView, Map => DMap}

import scala.collection.JavaConverters._

class ConnectorScannerTests {

  // private def load(filename: String): DMap = TestUtils.loadTestMap(s"scala/trn.prefab/${filename}")
  private def load(path: String): DMap = DMap.readMap(new ByteArrayInputStream(IOUtils.toByteArray(new FileInputStream(new File(path)))))

  // lazy val palette: PrefabPalette = PrefabPalette.fromMap(load("UNIT2.MAP"), true);
  lazy val palette: PrefabPalette = PrefabPalette.fromMap(load(HardcodedConfig.DOSPATH + "UNITMULT.MAP"), true);

  private def p(x: Int, y: Int): PointXY = new PointXY(x, y)

  /** creates just enough of a WallView to run ConnectorScanner tests -- not even close to being valid though */
  def testWall(wallId: Int, p0: PointXY, p1: PointXY, otherWall: Int = -1): WallView = {
    val w = new Wall(p0.x, p0.y)
    w.setOtherSide(otherWall, -1)
    w.setLotag(MultiSectorConnector.WALL_LOTAG)
    new WallView(w, wallId, new LineSegmentXY(p0, p1))
  }

  @Test
  def testPointToWallMap: Unit = {
    val walls0 = Seq(
      testWall(1, p(0, 0), p(1024, 0))
    )
    val result0 = ConnectorScanner.pointToWallMap(walls0)
    Assert.assertEquals(2, result0.size)
    Assert.assertTrue(result0(p(0, 0)).head == walls0(0))
    Assert.assertTrue(result0(p(1024, 0)).head == walls0(0))

    val walls1 = Seq(
      testWall(1, p(0, 0), p(1024, 0)),
      testWall(2, p(1024, 0), p(1024, 1024)),
    )
    val result1 = ConnectorScanner.pointToWallMap(walls1)
    println(result1.mapValues(_.map(_.getWallId)))

    Assert.assertEquals(3, result1.size)
    Assert.assertTrue(result1(p(0, 0)).size == 1 && result1(p(0, 0)).exists(_.getWallId == 1))
    Assert.assertEquals(2, result1(p(1024, 0)).size)
    Assert.assertTrue(result1(p(1024, 0)).map(_.getWallId).intersect(Set(1, 2)).size == 2)
    Assert.assertTrue(result1(p(1024, 1024)).size == 1 && result1(p(1024, 1024)).exists(_.getWallId == 2))

    Assert.assertTrue(result1.get(p(1024, 0)).get.map(_.getWallId).contains(2))
    Assert.assertTrue(result1.get(p(1024, 0)).getOrElse(Set.empty[WallView]).map(_.getWallId).contains(2))
  }

  private def scanLine(walls: Seq[WallView]): (Set[Int], Seq[WallView]) = {
    ConnectorScanner.scanLine(walls, ConnectorScanner.pointToWallMap(walls))
  }

  private def scanAllLines(walls: Seq[WallView]): Seq[Set[Int]] = {
    ConnectorScanner.scanAllLines(walls, ConnectorScanner.pointToWallMap(walls))
  }

  @Test
  def testGroupBy: Unit = {
    // this test brought to you by: forgetting to implement PointXY.hashCode()
    val x = Seq(
      ("a", 1),
      ("b", 1),
      ("b", 2),
      ("c", 2)
    )
    val grouped = x.groupBy(_._1).mapValues(x => x.map(_._2))
    Assert.assertTrue(grouped("b").contains(1))
    Assert.assertTrue(grouped("b").contains(2))

    val grouped2 = Seq(
      (p(0, 0), 1),
      (p(1024, 0), 1),
      (p(1024, 0), 2),
      (p(1024, 1024), 2),
    ).groupBy(_._1).mapValues(x => x.map(_._2))
    Assert.assertTrue(grouped2(p(1024, 0)).contains(1))
    Assert.assertTrue(grouped2(p(1024, 0)).contains(2))
  }

  @Test
  def testScanLineSingleWall: Unit = {
    // a single line segment
    val walls = Seq(testWall(1, p(0, 0), p(1024, 0)))
    val (line0, remaining0) = scanLine(walls)
    Assert.assertTrue(remaining0.isEmpty)
    Assert.assertEquals(1, line0.size)
    Assert.assertTrue(line0.toSeq(0) == 1)

    val lines = scanAllLines(walls)
    Assert.assertEquals(1, lines.size)
    Assert.assertEquals(1, lines(0).size)
    Assert.assertEquals(1, lines(0).head)
  }

  @Test
  def testScanLineTwoWalls: Unit = {

    // two line segments, connected
    val walls1 = Seq(
      testWall(1, p(0, 0), p(1024, 0)),
      testWall(2, p(1024, 0), p(1024, 1024)),
    )
    val pointToWall = ConnectorScanner.pointToWallMap(walls1)
    Assert.assertTrue(pointToWall(p(1024, 0)).map(_.getWallId).intersect(Set(1, 2)).size == 2)

    val x = pointToWall.get(p(1024, 0)).getOrElse(Set.empty[WallView])
    Assert.assertTrue(x.map(_.getWallId).contains(1))
    Assert.assertTrue(x.map(_.getWallId).contains(2))

    val points = walls1.find(_.getWallId == 1).get.getLineSegment.toList.asScala
    Assert.assertEquals(2, points.size)
    Assert.assertTrue(points.contains(p(0, 0)))
    Assert.assertTrue(points.contains(p(1024, 0)))

    val (line1, remaining1) = scanLine(walls1)
    Assert.assertTrue(ConnectorScanner.adjacent(walls1(0), walls1(1)))
    Assert.assertTrue(remaining1.isEmpty)
    Assert.assertEquals(2, line1.size)
    Assert.assertTrue(line1.intersect(Set(1, 2)).size == 2)

    val lines = scanAllLines(walls1)
    Assert.assertEquals(1, lines.size)
    Assert.assertEquals(2, lines(0).size)
    Assert.assertTrue(lines(0).intersect(Set(1, 2)).size == 2)

  }

  @Test
  def testScanLineSeparateWalls: Unit = {
    // two line segments, not connected

    val walls = Seq(
      testWall(1, p(0, 0), p(1024, 0)),
      testWall(2, p(2048, 0), p(3096, 0))
    )
    val (line1, remaining1) = scanLine(walls)
    Assert.assertEquals(1, remaining1.size)
    Assert.assertTrue(line1.contains(1) || line1.contains(2))

    val (line2, remaining2) = scanLine(remaining1)
    Assert.assertTrue(remaining2.isEmpty)
    Assert.assertTrue(line2.contains(1) || line2.contains(2))
    Assert.assertEquals(2, (line1 ++ line2).intersect(Set(1,2)).size)

    val lines = scanAllLines(walls)
    Assert.assertEquals(2, lines.size)
    Assert.assertEquals(1, lines(0).size)
    Assert.assertEquals(1, lines(1).size)
  }

  @Test
  def testScanLineLoop: Unit = {
    val walls = Seq(
      testWall(1, p(0, 0), p(1024, 0)),
      testWall(2, p(1024, 0), p(1024, 1024)),
      testWall(3, p(1024, 1024), p(0, 1024)),
      testWall(4, p(0, 1024), p(0, 0)),
    )
    val (line, remaining) = scanLine(walls)
    Assert.assertTrue(remaining.isEmpty)
    Assert.assertEquals(4, line.size)
    Assert.assertEquals(4, line.intersect(Set(1,2,3,4)).size)
  }

  @Test
  def testScanLineTwoConnectors: Unit = {

    /*
     * --->+   +----
     *     |   /\
     *     1   4
     *     |   |
     *    \/   |
     *     +   +
     *     |   /\
     *     2   3
     *     |   |
     *    \/   |
     * ----+   +----
     */

    val walls = Seq(
      // connector 1
      testWall(1, p(0, 0), p(0, 512), otherWall = 4),
      testWall(2, p(0, 512), p(0, 1024), otherWall = 3),
      // connector 2
      testWall(3, p(0, 1024), p(0, 512), otherWall = 2),
      testWall(4, p(0, 512), p(0, 0), otherWall = 1)
    )

    val w1 = walls(0)
    Assert.assertEquals(1, w1.getWallId)
    val w2 = walls(1)
    val w3 = walls(2)
    val w4 = walls(3)
    Assert.assertEquals(4, w4.getWallId)
    Assert.assertTrue(w1.isOtherSide(w4))
    Assert.assertFalse(ConnectorScanner.adjacent(w1, w4))
    Assert.assertFalse(ConnectorScanner.adjacent(w1, w3))

    Assert.assertFalse(ConnectorScanner.adjacent(w2, w3))
    Assert.assertFalse(ConnectorScanner.adjacent(w2, w4))

    val (line, remaining) = scanLine(walls)
    val (line2, zero) = scanLine(remaining)
    Assert.assertTrue(zero.isEmpty)

    Assert.assertEquals(2, line.size)
    Assert.assertEquals(2, line2.size)
    if(line.contains(1)){
      Assert.assertTrue(line.intersect(Set(1,2)).size == 2)
      Assert.assertTrue(line2.intersect(Set(3,4)).size == 2)
    }else{
      Assert.assertTrue(line2.intersect(Set(1,2)).size == 2)
      Assert.assertTrue(line.intersect(Set(3,4)).size == 2)
    }

    val lines = scanAllLines(walls)
    Assert.assertEquals(2, lines.size)
    Assert.assertEquals(2, lines(0).size)
    Assert.assertEquals(2, lines(1).size)
  }

  @Test
  def testSpritePointedAtWalls: Unit = {
    val s: Sprite = new Sprite(0, 0, 0, 0)
    s.setAngle(AngleUtil.ANGLE_RIGHT)

    val walls = Seq(
      testWall(1, p(64, -1024), p(80, 1024)),
      testWall(2, p(32, -1024), p(32, 1024)),
      testWall(3, p(512, -1024), p(512, 1024)),
    )
    val result = ConnectorScanner.rayIntersect(s, walls)
    Assert.assertTrue(result.isDefined)
    Assert.assertEquals(2, result.get.getWallId)

    val s2: Sprite = new Sprite(0, 0, 0, 0)
    Seq(AngleUtil.ANGLE_DOWN, AngleUtil.ANGLE_LEFT, AngleUtil.ANGLE_UP).foreach { ang =>
      s2.setAng(ang)
      val result2 = ConnectorScanner.rayIntersect(s2, walls)
      Assert.assertTrue(result2.isEmpty)
    }
  }

  @Test
  def testSortWalls: Unit = {
    val a = p(0, 0)
    val b = p(32, 0)
    val c = p(64, 0)
    val d = p(64, 1024)
    val e = p(96, 1024)
    val f = p(96, 0)

    Assert.assertTrue(ConnectorScanner.sortContinuousWalls(Seq.empty).isEmpty)

    val walls = Seq(
      testWall(1, a, b),
      testWall(2, b, c),
      testWall(3, c, d),
    )

    val results = ConnectorScanner.sortContinuousWalls(walls).map(_.getWallId)
    println(results)
    Assert.assertEquals(3, results.size)
    Assert.assertTrue(results.equals(Seq(1,2, 3)))
    Assert.assertTrue(results.equals(ConnectorScanner.sortContinuousWalls(walls.reverse).map(_.getWallId)))

    val morewalls = Seq(
      testWall(3, c, d),
      testWall(5, e, f),
      testWall(2, b, c),
      testWall(4, d, e),
      testWall(1, a, b),
    )
    val results2 = ConnectorScanner.sortContinuousWalls(morewalls).map(_.getWallId)
    Assert.assertEquals(5, results2.size)
    Assert.assertTrue(results2.equals(Seq(1, 2, 3, 4, 5)))
  }

  @Test
  def testSortWallsLoop: Unit = {
    val a = p(0, 0)
    val b = p(32, 0)
    val c = p(64, 0)
    val walls = Seq(
      testWall(1, a, b),
      testWall(2, b, c),
      testWall(3, c, a),
    )
    val results = ConnectorScanner.sortContinuousWalls(walls).map(_.getWallId)
    println(results)

    // TODO - fill this in
  }

  @Test
  def testSortWallsVertical: Unit = {
    // the loop detection algo has consequences for vertical segments going up vs down

    val a = p(0, 0)
    val b = p(0, 64)
    val c = p(0, 128)
    val d = p(0, 196)
    val walls = Seq(
      testWall(1, a, b),
      testWall(2, b, c),
      testWall(3, c, d),
    )
    val results = ConnectorScanner.sortContinuousWalls(walls).map(_.getWallId)
    Assert.assertEquals(3, results.size)
    // TODO - fill this in

    val e = p(0, 0)
    val f = p(0, -64)
    val g = p(0, -128)
    val h = p(0, -196)
    val walls2 = Seq(
      testWall(1, e, f),
      testWall(2, f, g),
      testWall(3, g, h),
    )
    val results2 = ConnectorScanner.sortContinuousWalls(walls2).map(_.getWallId)
    Assert.assertEquals(3, results2.size)
    // TODO - fill this in

    val walls3 = Seq(
      testWall(1, p(-62464, -54272), p(-62464, -54784)),
      testWall(2, p(-62464, -52736), p(-62464, -53248)),
      testWall(3, p(-62464, -53248), p(-62464, -54272)),
    )
    val results3 = ConnectorScanner.sortContinuousWalls(walls3).map(_.getWallId)
    Assert.assertEquals(3, results3.size)
  }

  @Test
  def testAnchor: Unit = {

    Assert.assertEquals(
      p(0, 0),
      ConnectorScanner.anchor(Seq(testWall(1, p(0, 0), p(32, 32))))
    )
    Assert.assertNotEquals(
      p(1, 0),
      ConnectorScanner.anchor(Seq(testWall(1, p(0, 0), p(32, 32))))
    )
    Assert.assertEquals(
      p(0, 0),
      ConnectorScanner.anchor(Seq(testWall(1, p(32, 32), p(0, 0))))
    )
    Assert.assertEquals(
      p(-1024, -64),
      ConnectorScanner.anchor(Seq(testWall(1, p(-1024, 512), p(32, -64))))
    )
    Assert.assertEquals(
      p(-1024, -128),
      ConnectorScanner.anchor(Seq(testWall(1, p(-1024, 512), p(32, -64)), testWall(2, p(64, -128), p(0, 0))))
    )


  }

  @Test
  def testSomething: Unit = {
    palette.getSG(1)

  }

}
