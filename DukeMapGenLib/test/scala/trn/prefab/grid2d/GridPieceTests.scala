package trn.prefab.grid2d

import org.junit.{Assert, Test}
import trn.prefab.Heading
import trn.prefab.experiments.Cell2D

class GridPieceTests {

  def c(x: Int, y: Int): Cell2D = Cell2D(x, y)
  val B = Side.Blocked
  val C = Side.Conn
  val U = Side.Unknown

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

  @Test
  def testConnected(): Unit = {
    val c = sgp(C, C, C, C)
    val b = sgp(B, B, B, B)
    Assert.assertFalse(GridPiece.connected(c, Cell2D(0, 0), c, Cell2D(0, 0)))
    Assert.assertTrue(GridPiece.connected(c, Cell2D(0, 0), c, Cell2D(1, 0)))
    Assert.assertTrue(GridPiece.connected(c, Cell2D(0, 0), c, Cell2D(0, 1)))
    Assert.assertFalse(GridPiece.connected(c, Cell2D(0, 0), c, Cell2D(1, 1)))

    Assert.assertFalse(GridPiece.connected(c, Cell2D(0, 0), b, Cell2D(0, 0)))
    Assert.assertFalse(GridPiece.connected(c, Cell2D(0, 0), b, Cell2D(1, 0)))
    Assert.assertFalse(GridPiece.connected(c, Cell2D(0, 0), b, Cell2D(0, 1)))
    Assert.assertFalse(GridPiece.connected(c, Cell2D(0, 0), b, Cell2D(1, 1)))

    Assert.assertTrue(GridPiece.connected(sgp(C, B, B, B), Cell2D(1, 1), sgp(B, B, C, B), Cell2D(2, 1)))
    Assert.assertFalse(GridPiece.connected(sgp(C, B, B, B), Cell2D(1, 1), sgp(B, B, U, B), Cell2D(2, 1)))
    Assert.assertFalse(GridPiece.connected(sgp(C, B, B, B), Cell2D(1, 1), sgp(B, B, C, B), Cell2D(0, 1)))

    Assert.assertTrue(GridPiece.connected(sgp(B, C, B, B), Cell2D(1, 1), sgp(B, B, B, C), Cell2D(1, 2)))
    Assert.assertFalse(GridPiece.connected(sgp(B, U, B, B), Cell2D(1, 1), sgp(B, B, B, C), Cell2D(1, 2)))

    Assert.assertTrue(GridPiece.connected(sgp(B, B, C, B), Cell2D(5, 1), sgp(C, B, B, B), Cell2D(4, 1)))
    Assert.assertFalse(GridPiece.connected(sgp(B, B, C, B), Cell2D(5, 1), sgp(C, B, B, B), Cell2D(3, 1)))

    Assert.assertTrue(GridPiece.connected(sgp(B, B, B, C), Cell2D(5, 9), sgp(B, C, B, B), Cell2D(5, 8)))
    Assert.assertFalse(GridPiece.connected(sgp(B, B, B, C), Cell2D(5, 9), sgp(B, C, B, B), Cell2D(5, 7)))
  }

  @Test
  def testConnectedMatchPieces(): Unit = {
    val connected = sgp(C, C, C, C)
    val blocked = sgp(B, B, B, B)

    Assert.assertEquals(None, GridPiece.connectedMatchPiecees(blocked, c(0, 0), blocked, c(0, 0)))
    Assert.assertEquals(Some(sgp(C, B, B, B), sgp(B, B, C, B)), GridPiece.connectedMatchPiecees(blocked, c(0, 0), blocked, c(1, 0)))
    Assert.assertEquals(Some(connected, connected), GridPiece.connectedMatchPiecees(connected, c(0, 0), connected, c(1, 0)))

    Assert.assertEquals(Some(sgp(B, B, C, B), sgp(C, B, B, B)), GridPiece.connectedMatchPiecees(blocked, c(1, 0), blocked, c(0, 0)))

    Assert.assertEquals(Some(sgp(B, C, B, B), sgp(B, B, B, C)), GridPiece.connectedMatchPiecees(blocked, c(0, 0), blocked, c(0, 1)))
    Assert.assertEquals(Some(sgp(B, B, B, C), sgp(B, C, B, B)), GridPiece.connectedMatchPiecees(blocked, c(0, 0), blocked, c(0, -1)))

    Assert.assertEquals(None, GridPiece.connectedMatchPiecees(blocked, c(0, 0), blocked, c(1, 1)))
  }

}
