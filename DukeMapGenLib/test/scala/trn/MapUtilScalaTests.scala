package trn

import org.junit.{Assert, Test}

class MapUtilScalaTests {

  private def p(x: Int, y: Int): PointXY = new PointXY(x, y)

  /** creates just enough of a WallView to run ConnectorScanner tests -- not even close to being valid though */
  def testWall(wallId: Int, p0: PointXY, p1: PointXY, otherWall: Int = -1): WallView = {
    val w = new Wall(p0.x, p0.y)
    w.setOtherSide(otherWall, -1)
    // w.setLotag(MultiSectorConnector.WALL_LOTAG)
    new WallView(w, wallId, new LineSegmentXY(p0, p1))
  }

  @Test
  def testOuterBorderDfs(): Unit = {

    val walls1 = Seq(
      testWall(1, p(0, 0), p(10, 10)),
      testWall(2, p(10, 10), p(10, 0)),
      testWall(3, p(10, 0), p(0, 0)),
      testWall(4, p(0, 0), p(5, 3), 20),
      testWall(5, p(5, 3), p(10, 10), 20),
    )
    val results1 = MapUtilScala.outerBorderDfs(walls1).map(_.getWallId)
    Assert.assertEquals(3, results1.size)
    Assert.assertTrue(  (results1 ++ results1).containsSlice(Seq(1,2,3)))
  }

  @Test
  def testOuterBorderSimpleStupidHole(): Unit = {
    val walls1 = Seq(
      testWall(1, p(-50, 0), p(0, 0)),
      testWall(2, p(0, 0), p(-10, -10)),
      testWall(3, p(-10, -10), p(10, -10)),
      testWall(4, p(10, -10), p(0, 0)),
      testWall(5, p(0, 0), p(50, 0)),
      testWall(6, p(50, 0), p(0, -64)),
      testWall(7, p(0, -64), p(-50, 0))
    )
    val results1 = MapUtilScala.outerBorderDfs(walls1).map(_.getWallId)
    Assert.assertEquals(4, results1.size)
    Assert.assertTrue((results1 ++ results1).containsSlice(Seq(1,5,6,7)))
  }

  @Test
  def testOuterBorderDfsStupidHolePrep(): Unit = {
    val red = 69

    val walls1 = Seq(
      testWall(1, p(10, 40), p(30, 40)),
      testWall(2, p(30, 40), p(30, 30)),
      testWall(3, p(30, 30), p(20, 30)),
      testWall(4, p(20, 30), p(10, 30)),
      testWall(5, p(10, 30), p(10, 40)),
    )
    val results1 = MapUtilScala.outerBorderDfs(walls1).map(_.getWallId)
    Assert.assertEquals(5, results1.size)
    Assert.assertTrue((results1 ++ results1).containsSlice(Seq(1,2,3, 4)))

    val walls2 = Seq(
      testWall(1, p(10, 40), p(30, 40)),
      testWall(2, p(30, 40), p(30, 30)),
      testWall(3, p(30, 30), p(20, 30)),
      testWall(4, p(20, 30), p(10, 30), red),
      testWall(5, p(10, 30), p(10, 40)),
      //testWall(6, p(30, 30), p(40, 30)),
      //testWall(7, p(40, 30), p(40, 20)),
      //testWall(8, p(40, 20), p(30, 20), red),
      //testWall(9, p(30, 20), p(30, 30)),
      testWall(10, p(10, 30), p(20, 30), red),
      testWall(11, p(20, 30), p(20, 20)),
      testWall(12, p(20, 20), p(30, 20)),
      testWall(13, p(30, 20), p(40, 20)), // red
      testWall(14, p(40, 20), p(40, 10)),
      testWall(15, p(40, 10), p(10, 10)),
      testWall(16, p(10, 10), p(10, 30))
    )
    val results2 = MapUtilScala.outerBorderDfs(walls2).map(_.getWallId)
    Assert.assertEquals(10, results2.size)
    Assert.assertTrue((results2 ++ results2).containsSlice(Seq(1,2,3,11,12,13,14,15,16,5)))

  }
  @Test
  def testOuterBorderDfsStupidHole(): Unit = {
    //   +-------1-------->+
    //   /\                |
    //   |                 2
    //   5                 |
    //   |                \/
    //   +<..4...+<---3----+---6---->+
    //   /\..10.>|        /\         |
    //   |       |         |         |
    //   |       11        9         7
    //   |       |         |         |
    //   |      \/         |         |
    //   |       +---12--->+<...8....+
    //   16                 ...13...>|
    //   |                           |
    //   |                          14
    //   |                           |
    //   +<------------15------------+
    val red = 69
    val walls3 = Seq(
      testWall(1, p(10, 40), p(30, 40)),
      testWall(2, p(30, 40), p(30, 30)),
      testWall(3, p(30, 30), p(20, 30)),
      testWall(4, p(20, 30), p(10, 30), red),
      testWall(5, p(10, 30), p(10, 40)),
      testWall(6, p(30, 30), p(40, 30)),
      testWall(7, p(40, 30), p(40, 20)),
      testWall(8, p(40, 20), p(30, 20), red),
      testWall(9, p(30, 20), p(30, 30)),
      testWall(10, p(10, 30), p(20, 30), red),
      testWall(11, p(20, 30), p(20, 20)),
      testWall(12, p(20, 20), p(30, 20)),
      testWall(13, p(30, 20), p(40, 20), red),
      testWall(14, p(40, 20), p(40, 10)),
      testWall(15, p(40, 10), p(10, 10)),
      testWall(16, p(10, 10), p(10, 30))
    )
    val results3 = MapUtilScala.outerBorderDfs(walls3).map(_.getWallId)
    Assert.assertEquals(8, results3.size)
    Assert.assertTrue((results3 ++ results3).containsSlice(Seq(1,2,6,7,14,15,16,5)))
  }

  @Test
  def testStarOfEdgeCases: Unit = {
    // every point on the outer edge is also on an inner loop
    val walls = Seq(
      testWall(1, p(40, -180), p(80, -140)),
      testWall(2, p(80, -140), p(60, -150)),
      testWall(3, p(60, -150), p(50, -160), 18),
      testWall(4, p(50, -160), p(40, -180)),

      testWall(5, p(80, -140), p(40, -100)),
      testWall(6, p(40, -100), p(50, -120)),
      testWall(7, p(50, -120), p(60, -130), 20),
      testWall(8, p(60, -130), p(80, -140)),

      testWall(9, p(40, -100), p(0, -140)),
      testWall(10, p(0, -140), p(20, -130)),
      testWall(11, p(20, -130), p(30, -120), 22),
      testWall(12, p(30, -120), p(40, -100)),

      testWall(13, p(0, -140), p(40, -180)),
      testWall(14, p(40, -180), p(30, -160)),
      testWall(15, p(30, -160), p(20, -150), 24),
      testWall(16, p(20, -150), p(0, -140)),

      testWall(17, p(30, -160), p(50, -160)),
      testWall(18, p(50, -160), p(60, -150), 3),
      testWall(19, p(60, -150), p(60, -130)),
      testWall(20, p(60, -130), p(50, -120), 7),
      testWall(21, p(50, -120), p(30, -120)),
      testWall(22, p(30, -120), p(20, -130), 11),
      testWall(23, p(20, -130), p(20, -150)),
      testWall(24, p(20, -150), p(30, -160), 15)
    )
    val results = MapUtilScala.outerBorderDfs(walls).map(_.getWallId)
    Assert.assertEquals(4, results.size)
    Assert.assertTrue((results ++ results).containsSlice(Seq(1,5,9,13)))
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testOuterBorderDfsEmpty: Unit = {
    MapUtilScala.outerBorderDfs(Seq.empty)
  }

  @Test(expected = classOf[IllegalArgumentException])
  def testOuterBorderDfsIds: Unit = {
    MapUtilScala.outerBorderDfs(Seq(
      testWall(1, p(0, 0), p(10, 10)),
      testWall(2, p(10, 10), p(10, 0)),
      testWall(2, p(10, 0), p(0, 0))
    ))
  }

  @Test
  def testOpposingWallLoop: Unit = {
    val walls = Seq(
      testWall(1, p(0, 0), p(5, 10)),
      testWall(2, p(5, 10), p(10, 0)),
      testWall(3, p(10, 0), p(0, 0)),
    )

    val walls2 = MapUtilScala.opposingRedwallLoop(walls)
    Assert.assertEquals(walls.size, walls2.size)

    Assert.assertEquals(walls(0).p2, walls(1).p1)
    Assert.assertEquals(walls(1).p2, walls(2).p1)
    Assert.assertEquals(walls(2).p2, walls(0).p1)
    Assert.assertEquals(walls2(0).p2, walls2(1).p1)
    Assert.assertEquals(walls2(1).p2, walls2(2).p1)
    Assert.assertEquals(walls2(2).p2, walls2(0).p1)

    Assert.assertTrue(walls(0).isBackToBack(walls2(2)))
    Assert.assertTrue(walls(1).isBackToBack(walls2(1)))
    Assert.assertTrue(walls(2).isBackToBack(walls2(0)))
  }

}
