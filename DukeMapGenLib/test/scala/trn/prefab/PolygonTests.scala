package trn.prefab

import org.junit.{Assert, Test}
import trn.{JavaTestUtils, LineSegmentXY, MapUtil, PointXY}
import trn.PointXYImplicits._

import scala.collection.JavaConverters._ // this is the good one

class PolygonTests {

  val FreezeAmmo = 37
  val ShotgunAmmo = 49

  private def midpoint(p1: PointXY, p2: PointXY): PointXY = {
    val p = p1.add(p2)
    new PointXY(p.x / 2, p.y / 2)
  }

  private lazy val testMap = TestUtils.loadJavaMap(JavaTestUtils.JUNIT1)


  @Test
  def testRayIntersectsAny(): Unit = {
    val rayStart = new PointXY(-17920, -58368)
    val rayVector = new PointXY(-1, -9)
    val vertex = new PointXY(-18432, -62976)
    Assert.assertFalse(Polygon.rayIntersectAny(rayStart, rayVector, Seq(), 2))
    Assert.assertFalse(Polygon.rayIntersectAny(rayStart, rayVector, Seq(PointXY.ZERO), 2))
    Assert.assertTrue(Polygon.rayIntersectAny(rayStart, rayVector, Seq(vertex), 2))
    Assert.assertTrue(Polygon.rayIntersectAny(rayStart, rayVector, Seq(vertex, PointXY.ZERO), 2))
  }

  @Test
  def testContainsExclusive(): Unit = {
    val testPalette: PrefabPalette = PrefabPalette.fromMap(testMap, true)

    val sg1 = testPalette.getSectorGroup(1)
    Assert.assertEquals(2, sg1.sprites.size)
    val ammo = sg1.sprites.filter(_.getTexture == ShotgunAmmo).head

    val wallLoop = sg1.getOuterWallLoop(ammo.getSectorId)
    Assert.assertTrue(Polygon.containsExclusive2(wallLoop, ammo.getLocation.asXY))


    val sg2 = testPalette.getSectorGroup(2)
    Assert.assertEquals(4, sg2.sprites.size)
    Assert.assertEquals(1, sg2.allSectorIds.size)
    val sg2ammo = sg2.sprites.filter(_.getTexture == ShotgunAmmo)// .map(_.getLocation.asXY)
    sg2ammo.foreach { ammo =>
      Assert.assertEquals(sg2.allSectorIds.head, ammo.getSectorId)
      val outerWallLoop = sg2.getOuterWallLoop(sg2.allSectorIds.head)
      Assert.assertEquals(5, outerWallLoop.size)
      val bb = BoundingBox(outerWallLoop.map(_.startPoint))
      Assert.assertTrue(bb.contains(ammo.getLocation.asXY))
      Assert.assertTrue(Polygon.containsExclusive2(outerWallLoop, ammo.getLocation.asXY))
    }

    // TODO - account for holes
    // sg2ammo.foreach { ammo1 => sg2ammo.foreach { ammo2 =>
    //   val p = midpoint(ammo1.getLocation.asXY, ammo2.getLocation.asXY)
    //   val outerWallLoop = sg2.getOuterWallLoop(sg2.allSectorIds.head)
    //   Assert.assertFalse(Polygon.containsExclusive2(outerWallLoop, p))
    // }}

    val sg5 = testPalette.getSectorGroup(5)
    Assert.assertTrue(sg2.sprites.size > 1)
    val sg5ammo = sg5.sprites.filter(_.getTexture == ShotgunAmmo)
    Assert.assertTrue(sg5ammo.size > 0)
    val outerWallLoop = sg5.getOuterWallLoop(sg5.allSectorIds.head)
    sg5ammo.foreach { ammo =>
      Assert.assertTrue(s"${ammo.getLocation.asXY}", Polygon.containsExclusive2(outerWallLoop, ammo.getLocation.asXY))
    }

    Polygon.containsPoints(outerWallLoop.map(_.getLineSegment), sg5ammo.map(_.getLocation.asXY))
  }

  @Test
  def testContainsExclusiveOutside(): Unit = {
    val testPalette: PrefabPalette = PrefabPalette.fromMap(testMap, true)

    def midPoint(p1: PointXY, p2: PointXY): PointXY ={
      val p3 = p1.add(p2)
      new PointXY(p3.x/2, p3.y/2)
    }

    val sg6 = testPalette.getSectorGroup(6)
    val sectorId = sg6.sprites.find(_.getTex == PrefabUtils.MARKER_SPRITE_TEX).head.getSectorId
    val shotgunAmmo = sg6.sprites.filter(_.getTex == ShotgunAmmo).map(_.getLocation.asXY)
    val freezeAmmo = sg6.sprites.filter(_.getTex == FreezeAmmo).map(_.getLocation.asXY)
    val testPoints = shotgunAmmo.flatMap { p1 => freezeAmmo.map { p2 => midPoint(p1, p2) }}
    Assert.assertTrue(testPoints.size > 0)

    val outerWallLoop = sg6.getOuterWallLoop(sectorId)
    testPoints.foreach { testPoint =>
      Assert.assertFalse(Polygon.containsExclusive2(outerWallLoop, testPoint))
    }
  }

  def getAllPolygons(sg: SectorGroup): Seq[Seq[LineSegmentXY]] = sg.allSectorIds.map(sg.getOuterWallLoop).map { loop =>
    loop.map(_.getLineSegment)
  }.toSeq


  @Test
  def testNoOverlap(): Unit = {
    val m = TestUtils.loadJavaMap(JavaTestUtils.JUNIT1)
    val testPalette: PrefabPalette = PrefabPalette.fromMap(m, true)

    val sg7 = testPalette.getSectorGroup(7)
    // val wallLoops = sg7.allSectorIds.map(sectorId => sg7.getOuterWallLoop(sectorId))
    // val polygons = wallLoops.map(loop => loop.map(_.getLineSegment))
    val polygons = getAllPolygons(sg7)
    require(polygons.size > 1)
    polygons.foreach { poly1 => polygons.foreach { poly2 =>
      Assert.assertFalse(Polygon.guessOverlap(poly1, poly2))
      Assert.assertFalse(Polygon.guessGroupsOverlap(Seq(poly1), Seq(poly2)))
    }}

  }

  @Test
  def testOverlap(): Unit = {
    val m = TestUtils.loadJavaMap(JavaTestUtils.JUNIT1)
    val testPalette: PrefabPalette = PrefabPalette.fromMap(m, true)

    val sg9 = testPalette.getSectorGroup(9)
    val sectorA = sg9.sprites.find(_.getTex == PrefabUtils.MARKER_SPRITE_TEX).get.getSectorId
    val polygonA = sg9.getOuterWallLoop(sectorA).map(_.getLineSegment)
    val ammo = sg9.sprites.filter(_.getTex == ShotgunAmmo).map(_.getLocation.asXY)
    require(ammo.size > 0)
    ammo.foreach { xy =>
      Assert.assertTrue(Polygon.containsExclusive(polygonA, xy))
    }
    Assert.assertTrue(Polygon.containsPoints(polygonA, ammo))

    val sectorB = sg9.allSectorIds.find(_ != sectorA).get
    val polygonsB = sg9.getOuterWallLoop(sectorB).map(_.getLineSegment)
    Assert.assertTrue(Polygon.containsPoints(polygonA, polygonsB.map(_.getP1)))

    val sg8 = testPalette.getSectorGroup(8)
    val polygons = getAllPolygons(sg8)
    require(polygons.size > 1)
    polygons.foreach { poly1 => polygons.foreach { poly2 =>
      if(poly1.head == poly2.head){
        Assert.assertFalse(Polygon.guessOverlap(poly1, poly2))
        Assert.assertFalse(Polygon.guessGroupsOverlap(Seq(poly1), Seq(poly2)))
      }else{
        Assert.assertTrue(Polygon.guessOverlap(poly1, poly2))
        Assert.assertTrue(Polygon.guessGroupsOverlap(Seq(poly1), Seq(poly2)))
      }
    }}
  }

  @Test
  def testIntersectsWith(): Unit = {
    val testPalette: PrefabPalette = PrefabPalette.fromMap(testMap, true)

    val allGroups = testPalette.numberedSectorGroupIds().asScala
    allGroups.foreach { id1 => allGroups.foreach { id2 =>
      if(id1 != id2){
        Assert.assertFalse(testPalette.getSG(id1).intersectsWith(testPalette.getSG(id2)))
      }
    }}

  }


}
