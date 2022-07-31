package trn.prefab.experiments.tiling
import org.junit.{Assert, Test}

class TilingMainTests {

  val Edge1 = 0
  val Edge2 = 1

  @Test
  def testTileNode(): Unit = {
    val t = TileNode((5, 5), 0, "tile1", Map(Edge1 -> TileEdge(Edge1, (5, 6), None)))
    val t2 = t.withEdge(TileEdge(Edge1, (5, 6), Some("MONKEY")))
    Assert.assertEquals("MONKEY", t2.edges(Edge1).info.get)
  }

  @Test
  def testAdd(): Unit = {
    Assert.assertEquals((1, 0), Tiling.add((0, 0))(1, 0))
    Assert.assertEquals((2, 0), Tiling.add((1, 0))(1, 0))
    Assert.assertEquals((1, 1), Tiling.add((0, 1))(1, 0))
    Assert.assertEquals(Seq((10, 10), (11, 12), (13, 14)), Seq((0, 0), (1, 2), (3, 4)).map(Tiling.add(10, 10)))
  }

}
