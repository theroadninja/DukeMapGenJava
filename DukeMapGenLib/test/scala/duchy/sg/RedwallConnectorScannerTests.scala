package duchy.sg

import org.junit.{Test, Assert}
import trn.ScalaMapLoader
import trn.prefab.TestUtils
import trn.MapImplicits._

class RedwallConnectorScannerTests {

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

}
