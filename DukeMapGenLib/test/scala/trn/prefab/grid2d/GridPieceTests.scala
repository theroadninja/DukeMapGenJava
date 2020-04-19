package trn.prefab.grid2d

import org.junit.{Assert, Test}
import trn.prefab.Heading

class GridPieceTests {

  private def sgp(e: Int, s: Int, w: Int, n: Int): SimpleGridPiece = SimpleGridPiece(e, s, w, n)

  @Test
  def testSimpleGridPice(): Unit = {
    val p1 = sgp(Side.Unknown, Side.Unknown, Side.Unknown, Side.Unknown)
    val p2 = sgp(Side.Blocked, Side.Blocked, Side.Blocked, Side.Blocked)
    Assert.assertTrue(p1.matches(p1))
    Assert.assertTrue(p1.couldMatch(p1))
    Assert.assertTrue(p1.matches(p1.rotatedCW))

    Assert.assertTrue(p1.matches(p2))
    Assert.assertTrue(p1.couldMatch(p2))
    Assert.assertTrue(p1.matches(p2.rotatedCW))
    Assert.assertTrue(p2.matches(p2))

    val pT = sgp(Side.Blocked, Side.Conn, Side.Conn, Side.Conn)
    Assert.assertTrue(pT.matches(pT))
    Assert.assertTrue(pT.couldMatch(pT))
    Assert.assertFalse(pT.matches(pT.rotatedCW))
    Assert.assertTrue(pT.couldMatch(pT.rotatedCW))

    Assert.assertTrue(p1.matches(pT))
    Assert.assertTrue(p1.matches(pT.rotatedCW))
    Assert.assertFalse(p2.matches(pT))

    val p3 = sgp(Side.Unknown, Side.Unknown, Side.Conn, Side.Conn)
    Assert.assertTrue(p3.matches(p1))
    Assert.assertTrue(p3.matches(pT))

    Assert.assertFalse(p3.rotatedCW.matches(pT))
    Assert.assertTrue(p3.rotatedCW.couldMatch(pT))

    Assert.assertEquals(Side.Conn, p3.rotatedCW.side(Heading.E))
    Assert.assertEquals(Side.Conn, p3.rotatedCW.rotatedCW.side(Heading.E))

    Assert.assertFalse(p3.rotatedCW.rotatedCW.matches(pT))
    Assert.assertTrue(p3.rotatedCW.rotatedCW.couldMatch(pT))

    Assert.assertTrue(p3.rotatedCW.rotatedCW.rotatedCW.matches(pT))
  }

  @Test
  def testRotateToMach(): Unit = {
    val p0 = sgp(Side.Blocked, Side.Blocked, Side.Blocked, Side.Blocked)
    val p1 = sgp(Side.Blocked, Side.Blocked, Side.Blocked, Side.Unknown)
    Assert.assertEquals(Some(p1), p1.rotateToMatch(p0))
    Assert.assertEquals(Some(p1), p1.rotateToMatch(p1))

    val p2 = sgp(Side.Conn, Side.Blocked, Side.Blocked, Side.Blocked)
    Assert.assertEquals(None, p2.rotateToMatch(p0))
    Assert.assertEquals(Some(sgp(Side.Blocked, Side.Blocked, Side.Blocked, Side.Conn)), p2.rotateToMatch(p1))
  }

}
