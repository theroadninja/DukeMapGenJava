package trn.duke

import trn.Sprite

import org.junit.{Assert, Test}

class GameLogicTests {

  def sprite(tex: Int, lotag: Int): Sprite = {
    val s = new Sprite(0, 0, 0, 0)
    s.setTexture(tex)
    s.setLotag(lotag)
    s
  }

  @Test
  def testShouldRotate(): Unit = {
    Assert.assertFalse(GameLogic.shouldRotate(sprite(TextureList.SE, 0)))
    Assert.assertFalse(GameLogic.shouldRotate(sprite(TextureList.SE, 1)))
    Assert.assertTrue(GameLogic.shouldRotate(sprite(TextureList.SE, 20)))
    Assert.assertFalse(GameLogic.shouldRotate(sprite(TextureList.CYCLER, 0)))
    Assert.assertTrue(GameLogic.shouldRotate(sprite(TextureList.VIEWSCREEN, 0)))
  }

}
