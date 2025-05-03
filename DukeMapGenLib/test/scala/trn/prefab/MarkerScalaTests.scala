package trn.prefab

import org.junit.{Assert, Test}
import trn.prefab.MarkerScala.scanAnchors
import trn.{Sprite, PointXYZ}

class MarkerScalaTests {

  def p(x: Int, y: Int, z: Int): PointXYZ = new PointXYZ(x, y, z)

  def newAnchor(location: PointXYZ, hitag: Int = 0): Sprite = {
    val sectnum = -1
    new Sprite(location, sectnum, Marker.TEX, hitag, Marker.Lotags.ANCHOR)
  }

  @Test
  def testScanMarkers(): Unit = {
    val P0 = PointXYZ.ZERO
    val m1 = newAnchor(p(0, 0, 0), hitag=2)
    val m2 = newAnchor(p(2, 2, 2), hitag=2)

    Assert.assertTrue(scanAnchors(Seq.empty).isEmpty)

    // single "paired" anchor wont resolve
    Assert.assertTrue(scanAnchors(Seq(m1)).isEmpty)

    Assert.assertEquals(1, scanAnchors(Seq(newAnchor(P0))).size)

    Assert.assertEquals(1, scanAnchors(Seq(m1, m2)).size)

    Assert.assertEquals(2, scanAnchors(Seq(newAnchor(P0), m1, m2)).size)
    Assert.assertEquals(3, scanAnchors(Seq(newAnchor(P0), newAnchor(P0), m1, m2)).size)
  }

}
