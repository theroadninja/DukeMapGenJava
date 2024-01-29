package duchy.sg

import org.junit.{Assert, Test}
import trn.{PointXY, Wall, ScalaMapLoader, Sprite, WallView, LineSegmentXY, Map => DMap}
import trn.prefab.{PrefabUtils, DukeConfig, RedwallConnector, TestUtils, Marker}
import trn.MapImplicits._

import java.util
import java.util.{ArrayList, List}

class RedwallConnectorScannerTests {

  val MarkerTex = Marker.MARKER_SPRITE_TEX

  val FireHydrantTex = 981
  val TileFloorTex = 414

  /**
    * This map has one sector, with one sprite that is pointed at a particular wall.  That wall and only that wall has
    * texture 414.
    */
  @Test
  def testSpriteIntersection(): Unit = {
    val map = ScalaMapLoader.loadMap(TestUtils.testDataPath("scala", "sprtint.map"))

    val sprite = map.allSprites.find(_.getTex == FireHydrantTex).get

    val wallId = RedwallConnectorScanner.findSpriteTargetWall(map.asView, sprite)
    Assert.assertEquals(TileFloorTex, map.getWall(wallId).getTex)
  }

  /**
    * This map just has a single group of sectors with a bunch of different wall sections.
    *
    * This test does not use marker sprites.
    */
  @Test
  def testWallCrawler(): Unit = {
    val map = ScalaMapLoader.loadMap(TestUtils.testDataPath("scala", "crawler.map"))

    val allWalls = map.allWallViews
    val wallsById = allWalls.map(wv => wv.getWallId -> wv).toMap
    val wallIdToSector = map.asView.newWallIdToSectorIdMap.toMap
    val pointToWall = RedwallConnectorScanner.pointToWallMap(allWalls)

    // all of the walls have a certain texture to match lotags 1, 2, 3 to make sure the crawler finds the right ones
    val Lotag1Tex = 252 // most walls with lotag 1 are given this tex
    val Lotag2Tex = 258
    val Lotag3Tex = 237

    val results1 = RedwallConnectorScanner.crawlWalls(wallsById, pointToWall, wallIdToSector, allWalls.find(w => w.tex == 616).get)
    Assert.assertEquals(3, results1.size)
    results1.foreach { w =>
      Assert.assertTrue(w.tex == 616 || w.tex == Lotag1Tex)
    }

    val results2 = RedwallConnectorScanner.crawlWalls(wallsById, pointToWall, wallIdToSector, allWalls.find(w => w.tex == 617).get)
    Assert.assertEquals(4, results2.size)
    results2.foreach { w =>
      Assert.assertTrue(w.tex == 617 || w.tex == Lotag1Tex)
    }

    val results3 = RedwallConnectorScanner.crawlWalls(wallsById, pointToWall, wallIdToSector, allWalls.find(w => w.tex == 618).get)
    Assert.assertEquals(5, results3.size)
    results3.foreach { w =>
      Assert.assertTrue(w.tex == 618 || w.tex == Lotag1Tex)
    }

    val results4 = RedwallConnectorScanner.crawlWalls(wallsById, pointToWall, wallIdToSector, allWalls.find(w => w.tex == 619).get)
    Assert.assertEquals(2, results4.size)
    results4.foreach { w =>
      Assert.assertTrue(w.tex == 619 || w.tex == Lotag2Tex)
    }

    val results5 = RedwallConnectorScanner.crawlWalls(wallsById, pointToWall, wallIdToSector, allWalls.find(w => w.tex == 633).get)
    Assert.assertEquals(5, results5.size)
    results5.foreach { w =>
      Assert.assertTrue(w.tex == 633 || w.tex == Lotag3Tex)
    }

    //
    // large loop on the inside
    //
    val results6 = RedwallConnectorScanner.crawlWalls(wallsById, pointToWall, wallIdToSector, allWalls.find(w => w.tex == 595).get)
    Assert.assertEquals(3, results6.size)
    results6.foreach { w =>
      Assert.assertTrue(w.tex == 595 || w.tex == Lotag1Tex)
    }

    val results7 = RedwallConnectorScanner.crawlWalls(wallsById, pointToWall, wallIdToSector, allWalls.find(w => w.tex == 609).get)
    Assert.assertEquals(3, results7.size)
    results7.foreach { w =>
      Assert.assertTrue(w.tex == 609 || w.tex == Lotag2Tex)
    }

    val results8 = RedwallConnectorScanner.crawlWalls(wallsById, pointToWall, wallIdToSector, allWalls.find(w => w.tex == 610).get)
    Assert.assertEquals(3, results8.size)
    results8.foreach { w =>
      Assert.assertTrue(w.tex == 610 || w.tex == Lotag3Tex)
    }

    //
    // small loop where the whole loop is the connector
    //
    val results9 = RedwallConnectorScanner.crawlWalls(wallsById, pointToWall, wallIdToSector, allWalls.find(w => w.tex == 503).get)
    Assert.assertEquals(4, results9.size)
    results9.foreach { w =>
      Assert.assertTrue(w.tex == 503 || w.tex == Lotag3Tex)
    }

    //
    // peninsula sector sticking out of the wall (make sure results10 and results11 don't overlap)
    //
    val results10 = RedwallConnectorScanner.crawlWalls(wallsById, pointToWall, wallIdToSector, allWalls.find(w => w.tex == 285).get)
    Assert.assertEquals(3, results10.size)
    results10.foreach { w =>
      Assert.assertTrue(w.tex == 285 || w.tex == Lotag2Tex)
      Assert.assertTrue(w.lotag == 2)
    }

    val results11 = RedwallConnectorScanner.crawlWalls(wallsById, pointToWall, wallIdToSector, allWalls.find(w => w.tex == 439).get)
    Assert.assertEquals(2, results11.size)
    results11.foreach { w =>
      Assert.assertTrue(w.tex == 439 || w.tex == Lotag2Tex)
      Assert.assertTrue(w.lotag == 2)
      Assert.assertFalse(results10.exists(_.getWallId == w.getWallId))
    }

  }

  @Test
  def testFindAllRedwallSections(): Unit = {
    val map = ScalaMapLoader.loadMap(TestUtils.testDataPath("scala", "crawler.map"))

    // all of the walls have a certain texture to match lotags 1, 2, 3 to make sure the crawler finds the right ones
    val Lotag1Tex = 252 // most walls with lotag 1 are given this tex
    val Lotag2Tex = 258
    val Lotag3Tex = 237

    val sections: Seq[RedwallSection] = RedwallConnectorScanner.findAllRedwallSections(map.asView)

    Assert.assertEquals(11, sections.size)

    def assertSection(connectorId: Int, size: Int, textures: Seq[Int]): Unit = {
      val section = sections.find(_.marker.getHiTag == connectorId).get
      Assert.assertEquals(size, section.size)
      section.sortedWalls.foreach { wall =>
        Assert.assertTrue(textures.contains(wall.tex))
      }
    }

    assertSection(1, 3, Seq(616, Lotag1Tex))
    assertSection(2, 4, Seq(617, Lotag1Tex))
    assertSection(3, 5, Seq(618, Lotag1Tex))
    assertSection(4, 2, Seq(619, Lotag2Tex))
    assertSection(5, 5, Seq(633, Lotag3Tex))
    assertSection(6, 3, Seq(595, Lotag1Tex))
    assertSection(7, 3, Seq(609, Lotag2Tex))
    assertSection(8, 3, Seq(610, Lotag3Tex))
    assertSection(9, 4, Seq(503, Lotag3Tex))
    assertSection(10, 3, Seq(285, Lotag2Tex))
    assertSection(11, 2, Seq(439, Lotag2Tex))
  }

  @Test
  def testFindAllRedwallConns(): Unit = {
    val map = ScalaMapLoader.loadMap(TestUtils.testDataPath("scala", "crawler.map"))
    val conns: Seq[RedwallConnector] = RedwallConnectorScanner.findAllRedwallConns(map.asView)
    Assert.assertEquals(11, conns.size)

    Assert.assertEquals(3, conns.find(_.getConnectorId == 1).get.getWallCount)
    Assert.assertEquals(4, conns.find(_.getConnectorId == 2).get.getWallCount)
    Assert.assertEquals(5, conns.find(_.getConnectorId == 3).get.getWallCount)
    Assert.assertEquals(2, conns.find(_.getConnectorId == 4).get.getWallCount)
    Assert.assertEquals(5, conns.find(_.getConnectorId == 5).get.getWallCount)
    Assert.assertEquals(3, conns.find(_.getConnectorId == 6).get.getWallCount)
    Assert.assertEquals(3, conns.find(_.getConnectorId == 7).get.getWallCount)
    Assert.assertEquals(3, conns.find(_.getConnectorId == 8).get.getWallCount)
    Assert.assertEquals(4, conns.find(_.getConnectorId == 9).get.getWallCount)
    Assert.assertEquals(3, conns.find(_.getConnectorId == 10).get.getWallCount)
    Assert.assertEquals(2, conns.find(_.getConnectorId == 11).get.getWallCount)
  }

  @Test
  def testFindMultiSector(): Unit = {
    val map = ScalaMapLoader.loadMap(TestUtils.testDataPath("scala", "multi.map"))
    val cfg = DukeConfig.loadHardCodedVersion()
    val sgFrags = SectorGroupScanner.scanFragments(map, cfg)
    val frag1 = sgFrags.find(_.groupId.getOrElse(-1) == 1).get
    val sections: Seq[RedwallSection] = RedwallConnectorScanner.findAllRedwallSections(frag1.clipboard.asView)
    Assert.assertEquals(2, sections.size)

    val parent = sections.find(!_.isChild).get
    val child = sections.find(_.isChild).get
    Assert.assertTrue(parent.isBefore(child))
    Assert.assertFalse(child.isBefore(parent))

    val frag2 = sgFrags.find(_.groupId.getOrElse(-1) == 2).get
    val sections2: Seq[RedwallSection] = RedwallConnectorScanner.findAllRedwallSections(frag2.clipboard.asView)
    Assert.assertEquals(3, sections2.size)

  }

  @Test
  def testScannMultiSectorHappyCase(): Unit = {
    val map = ScalaMapLoader.loadMap(TestUtils.testDataPath("scala", "multi.map"))
    val cfg = DukeConfig.loadHardCodedVersion()
    val sgFrags = SectorGroupScanner.scanFragments(map, cfg)
    def getFrag(sectorGroupId: Int): SectorGroupFragment = sgFrags.find(_.groupId == Some(sectorGroupId)).get

    val frag1 = getFrag(1)
    val conns = RedwallConnectorScanner.findAllRedwallConns(frag1.clipboard.asView)
    Assert.assertEquals(1, conns.size)
    val multi = conns.head
    Assert.assertTrue(multi.isRedwall)
    Assert.assertEquals(5, multi.getWallIds.size())

    val conns2 = RedwallConnectorScanner.findAllRedwallConns(getFrag(2).clipboard.asView)
    Assert.assertEquals(1, conns2.size)
    val multi2 = conns2.head
    Assert.assertTrue(multi2.isRedwall)
    Assert.assertEquals(9, multi2.getWallIds.size())

    val conns3 = RedwallConnectorScanner.findAllRedwallConns(getFrag(3).clipboard.asView)
    Assert.assertEquals(2, conns3.size)
    Assert.assertEquals(9, conns3(0).getWallIds.size)
    Assert.assertEquals(9, conns3(1).getWallIds.size)
  }

  @Test
  def testRelativeConnPoints(): Unit = {
    def p(x: Int, y: Int) = new PointXY(x, y)

    def testWall(wallId: Int, p0: PointXY, p1: PointXY, otherWall: Int = -1): WallView = {
      val w = new Wall(p0.x, p0.y)
      w.setOtherSide(otherWall, -1)
      new WallView(w, wallId, new LineSegmentXY(p0, p1), -1, -1)
    }

    val walls = Seq(
      testWall(1, p(32, 16), p(64, 17)),
      testWall(2, p(64, 17), p(96, 18)),
      testWall(2, p(96, 18), p(128, 19)),
    )
    val anchor: PointXY = p(32, 16)

    val results = RedwallConnectorScanner.getRelativeConnPoints(walls, anchor)
    Assert.assertEquals(4, results.size)
    Assert.assertEquals(p(0, 0), results(0))
    Assert.assertEquals(p(32, 1), results(1))
    Assert.assertEquals(p(64, 2), results(2))
    Assert.assertEquals(p(96, 3), results(3))
  }
}
