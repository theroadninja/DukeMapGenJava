package trn.logic

import org.junit.{Assert, Test}
import trn.logic.Tile2d._
import trn.math.SnapAngle
import trn.prefab.Heading

class Tile2dTests {

  @Test
  def testSide(): Unit = {
    val t = Tile2d(Wildcard, Blocked, Conn, Conn)
    Assert.assertEquals(Wildcard, t.e)
    Assert.assertEquals(Wildcard, t.side(Heading.E))
    Assert.assertEquals(Blocked, t.s)
    Assert.assertEquals(Blocked, t.side(Heading.S))
    Assert.assertEquals(Conn, t.w)
    Assert.assertEquals(Conn, t.side(Heading.W))
    Assert.assertEquals(Conn, t.n)
    Assert.assertEquals(Conn, t.side(Heading.N))

    val t2 = t.withSide(Heading.E, Conn).withSide(Heading.N, Blocked)
    Assert.assertEquals(Conn, t2.e)
    Assert.assertEquals(Conn, t2.side(Heading.E))
    Assert.assertEquals(Blocked, t2.s)
    Assert.assertEquals(Blocked, t2.side(Heading.S))
    Assert.assertEquals(Conn, t2.w)
    Assert.assertEquals(Conn, t2.side(Heading.W))
    Assert.assertEquals(Blocked, t2.n)
    Assert.assertEquals(Blocked, t2.side(Heading.N))

    val t3 = Tile2d().withSide(Heading.S, Conn).withSide(Heading.W, Blocked)
    Assert.assertEquals(Wildcard, t3.e)
    Assert.assertEquals(Wildcard, t3.side(Heading.E))
    Assert.assertEquals(Conn, t3.s)
    Assert.assertEquals(Conn, t3.side(Heading.S))
    Assert.assertEquals(Blocked, t3.w)
    Assert.assertEquals(Blocked, t3.side(Heading.W))
    Assert.assertEquals(Wildcard, t3.n)
    Assert.assertEquals(Wildcard, t3.side(Heading.N))
  }

  @Test
  def testRotationTo(): Unit = {
    val t0 = Tile2d(Conn, Wildcard, Wildcard, Wildcard)
    val t1 = Tile2d(Blocked, Conn, Blocked, Blocked)
    val t2 = Tile2d(Blocked, Blocked, Conn, Blocked)
    val t3 = Tile2d(Blocked, Blocked, Blocked, Conn)
    val t4 = Tile2d(Conn, Blocked, Blocked, Blocked)

    Assert.assertEquals(SnapAngle(1), t0.rotationTo(t1).get)
    Assert.assertEquals(SnapAngle(2), t0.rotationTo(t2).get)
    Assert.assertEquals(SnapAngle(3), t0.rotationTo(t3).get)
    Assert.assertEquals(SnapAngle(0), t0.rotationTo(t4).get)
    Assert.assertFalse(t0.rotationTo(Tile2d(Blocked, Blocked, Blocked, Blocked)).isDefined)
  }

  @Test
  def testFold(): Unit = {
    val headings = Seq(Heading.E, Heading.W)
    val t = headings.foldLeft(Tile2d(Tile2d.Wildcard)){ (tile, heading) => tile.withSide(heading, Tile2d.Conn) }
    Assert.assertEquals(Conn, t.e)
    Assert.assertEquals(Wildcard, t.s)
    Assert.assertEquals(Conn, t.w)
    Assert.assertEquals(Wildcard, t.n)
  }

}
