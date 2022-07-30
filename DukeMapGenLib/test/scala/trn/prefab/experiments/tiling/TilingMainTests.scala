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

}
