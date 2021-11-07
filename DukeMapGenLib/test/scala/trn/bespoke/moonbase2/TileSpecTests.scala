package trn.bespoke.moonbase2

import org.junit.{Assert, Test}
import TileSpec._
import trn.logic.Tile2d

class TileSpecTests {

  @Test
  def testOneWayTile(): Unit = {
    val t = TileSpec(
      Side(ConnRequired, Some(2)),
      Side(ConnRequired, Some(3)),
      Side(ConnBlocked, None),
      Side(ConnBlocked, None)
    )
    Assert.assertEquals(Tile2d(Tile2d.Conn, 2, Tile2d.Blocked, Tile2d.Blocked), t.toOneWayTile2d(ConnBlocked))
  }

  @Test
  def testOneWayTileError(): Unit = {
    val t = TileSpec(Side(1,Some(1)),Side(-1,None),Side(0,Some(2)),Side(1,Some(2)))
    // the bad result was:  result=Tile2d(1,0,2,1)
    Assert.assertEquals(
      Tile2d(Tile2d.Conn, Tile2d.Blocked, Tile2d.Blocked, 2),
      t.toOneWayTile2d(ConnBlocked)
    )
  }
}
