package trn.prefab

import org.junit.{Assert, Test}
import trn.{PointXYZ, Sprite, Wall}
import trn.duke.TextureList

class GameConfigTests {

  val sectorId = 0

  def spr(tex: Int, hitag: Int, lotag: Int): Sprite = new Sprite(PointXYZ.ZERO, sectorId, tex, hitag, lotag)

  def se(hitag: Int, lotag: Int): Sprite = spr(TextureList.SE, hitag, lotag)

  def activator(hitag: Int, lotag: Int): Sprite = spr(TextureList.ACTIVATOR, hitag, lotag)

  @Test
  def testUniqueTag(): Unit = {

    val cfg = DukeConfig.empty
    val s = new Sprite(PointXYZ.ZERO, sectorId, TextureList.SE, 42, 1)

    Assert.assertEquals(TextureList.SE, s.getTex)
    Assert.assertTrue(s.getHiTag != 0)
    Assert.assertTrue(DukeConfig.UniqueHiSE.contains(s.getLotag))
    Assert.assertTrue(cfg.uniqueTags(s) == Seq(42))
    Assert.assertTrue(cfg.uniqueTags(se(21, 0)) == Seq(21))

    // SE lotag 2 does not have a unique hitag value
    Assert.assertTrue(cfg.uniqueTags(se(21, 2)) == Seq.empty)

    // Two-way train
    Assert.assertTrue(cfg.uniqueTags(se(123, 30)) == Seq(123, 124, 125))

    Assert.assertTrue(cfg.uniqueTags(activator(123, 456)) == Seq(456))
    Assert.assertTrue(cfg.uniqueTags(spr(TextureList.Switches.MULTI_SWITCH, 123, 456)) == Seq(456, 457, 458, 459))
    Seq(TextureList.TOUCHPLATE, TextureList.ACTIVATOR_LOCKED, TextureList.MASTERSWITCH, TextureList.RESPAWN).foreach { tex =>
      Assert.assertTrue(cfg.uniqueTags(spr(tex, 123, 456)) == Seq(456))
    }
    Assert.assertTrue(cfg.uniqueTags(spr(TextureList.VIEWSCREEN, 123, 456)) == Seq(123))
    Assert.assertTrue(cfg.uniqueTags(spr(TextureList.VIEWSCREEN_SPACE, 123, 456)) == Seq(123))
    Assert.assertTrue(cfg.uniqueTags(spr(TextureList.CRACK1, 123, 456)) == Seq(123))
    Assert.assertTrue(cfg.uniqueTags(spr(TextureList.Switches.ACCESS_SWITCH, 123, 456)) == Seq(456))
    Assert.assertTrue(cfg.uniqueTags(spr(TextureList.Switches.ACCESS_SWITCH_2, 123, 456)) == Seq(456))
    Assert.assertTrue(cfg.uniqueTags(spr(TextureList.Switches.LIGHT_SWITCH, 123, 456)) == Seq(456))
    Assert.assertTrue(cfg.uniqueTags(spr(TextureList.CAMERA1, 123, 456)) == Seq(456))
    Assert.assertTrue(cfg.uniqueTags(spr(TextureList.DOORS.DOORTILE1, 123, 456)) == Seq(456))
    Assert.assertTrue(cfg.uniqueTags(spr(TextureList.EXPLOSIVE_TRASH.SEENINE, 123, 456)) == Seq(123))
    Assert.assertTrue(cfg.uniqueTags(spr(TextureList.FEM.FEM1, 69, 0)) == Seq(69))
  }

  @Test
  def testUniqueTagWall(): Unit = {
    val cfg = DukeConfig.empty
    Assert.assertTrue(cfg.uniqueTags(new Wall(0, 0, TextureList.GLASS)) == Seq.empty)
    Assert.assertTrue(cfg.uniqueTags(new Wall(0, 0, TextureList.DOORS.DOORTILE1)) == Seq.empty)
    val w = new Wall(0, 0, TextureList.DOORS.DOORTILE1)
    w.setLotag(456)
    Assert.assertTrue(cfg.uniqueTags(w) == Seq(456))
  }

}
