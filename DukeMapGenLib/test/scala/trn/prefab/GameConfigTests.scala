package trn.prefab

import org.junit.{Assert, Test}
import trn.{PointXYZ, Sprite, Wall}
import trn.duke.{Lotags, TextureList}

/**
  * See also UniqueTagsTests.
  */
class GameConfigTests {

  val sectorId = 0

  def spr(tex: Int, hitag: Int, lotag: Int): Sprite = new Sprite(PointXYZ.ZERO, sectorId, tex, hitag, lotag)

  def se(hitag: Int, lotag: Int): Sprite = spr(TextureList.SE, hitag, lotag)

  def activator(hitag: Int, lotag: Int): Sprite = spr(TextureList.ACTIVATOR, hitag, lotag)

  def wall(tex: Int, hitag: Int, lotag: Int, maskTex: Int = 0): Wall = {
    val w: Wall = new Wall();
    w.setTexture(tex)
    w.setHitag(hitag)
    w.setLotag(lotag)
    w.setMaskTex(maskTex)
    w
  }

  @Test
  def testGroupedUniqueTags(): Unit = {

    val cfg = DukeConfig.empty
    Assert.assertTrue(cfg.groupedUniqueTags(se(21, 0)) == Seq.empty) // SE0 has a uniqe hitag, but its not in a group
    Assert.assertTrue(cfg.groupedUniqueTags(spr(TextureList.Switches.MULTI_SWITCH, 123, 456)) == Seq(456, 457, 458, 459))
    // Two-way train
    Assert.assertTrue(cfg.uniqueTags(se(123, 30)) == Seq(123, 124, 125))
  }

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
    // Ignoring DoorTile for sprites
    // Assert.assertTrue(cfg.uniqueTags(spr(TextureList.DOORS.DOORTILE1, 123, 456)) == Seq(456))
    Assert.assertTrue(cfg.uniqueTags(spr(TextureList.EXPLOSIVE_TRASH.SEENINE, 123, 456)) == Seq(123))
    Assert.assertTrue(cfg.uniqueTags(spr(TextureList.FEM.FEM1, 69, 0)) == Seq(69))


  }

  @Test
  def testUniqueTagWall(): Unit = {
    val cfg = DukeConfig.empty
    Assert.assertTrue(cfg.uniqueTags(new Wall(0, 0, TextureList.GLASS)) == Seq.empty) // Glass as no special tags
    Assert.assertTrue(cfg.uniqueTags(new Wall(0, 0, TextureList.DOORS.DOORTILE1)) == Seq.empty)
    val w = new Wall(0, 0, TextureList.DOORS.DOORTILE1)
    w.setLotag(456)
    Assert.assertTrue(cfg.uniqueTags(w) == Seq(456))

    Assert.assertTrue(cfg.uniqueTags(wall(TextureList.ForceFields.BIGFORCE, 123, 456)) == Seq(456))
    Assert.assertTrue(cfg.uniqueTags(wall(TextureList.ForceFields.W_FORCEFIELD, 123, 456)) == Seq(456))
    Assert.assertTrue(cfg.uniqueTags(wall(TextureList.ForceFields.BIGFORCE, 0, 0)) == Seq.empty)
    Assert.assertTrue(cfg.uniqueTags(wall(TextureList.ForceFields.W_FORCEFIELD, 0, 0)) == Seq.empty)
  }

  private def testUpdateUniqueTag(
    sprite: Sprite,
    tagMap: Map[Int, Int],
    expectedHitag: Int,
    expectedLotag: Int
  ): Unit = {
    DukeConfig.empty.updateUniqueTagInPlace(sprite, tagMap)
    Assert.assertEquals(expectedHitag, sprite.getHiTag)
    Assert.assertEquals(expectedLotag, sprite.getLotag)
  }

  @Test
  def updateUniqueTagInPlace(): Unit = {
    val cfg = DukeConfig.empty
    val sprite = spr(TextureList.Switches.ACCESS_SWITCH, 99, 123)
    Assert.assertEquals(123, sprite.getLotag)
    cfg.updateUniqueTagInPlace(sprite, Map(123 -> 5))
    Assert.assertEquals(5, sprite.getLotag)

    testUpdateUniqueTag(spr(TextureList.Switches.ACCESS_SWITCH, 99, 123), Map(123 -> 5), 99, 5)
    testUpdateUniqueTag(spr(TextureList.SE, 14, 0), Map(14 -> 1), 1, 0)
    testUpdateUniqueTag(spr(217, 14, 0), Map(14 -> 1), 14, 0) // just a random texture
    testUpdateUniqueTag(spr(TextureList.SE, 14, 10), Map(14 -> 1), 14, 10) // SE 10 does not have a unique tag
    testUpdateUniqueTag(spr(TextureList.SE, 14, Lotags.SE.TWO_WAY_TRAIN), Map(14 -> 100), 100, Lotags.SE.TWO_WAY_TRAIN)
    Seq(TextureList.TOUCHPLATE, TextureList.ACTIVATOR_LOCKED, TextureList.MASTERSWITCH, TextureList.RESPAWN).foreach { tex =>
      testUpdateUniqueTag(spr(tex, 1203, 500), Map(500 -> 111), 1203, 111)
    }

    Seq(TextureList.VIEWSCREEN, TextureList.VIEWSCREEN_SPACE, TextureList.CRACK1, TextureList.CRACK4).foreach { tex =>
      testUpdateUniqueTag(spr(tex, 1400, 5), Map(1400 -> 12345), 12345, 5)
    }
    Seq(TextureList.EXPLOSIVE_TRASH.SEENINE, TextureList.FEM.FEM1).foreach { tex =>
      testUpdateUniqueTag(spr(tex, 1400, 5), Map(1400 -> 12345), 12345, 5)
    }

    Seq(TextureList.CAMERA1, TextureList.Switches.ALIEN_SWITCH, TextureList.Switches.FRANKENSTINE_SWITCH).foreach { tex =>
      testUpdateUniqueTag(spr(tex, 1203, 500), Map(500 -> 111), 1203, 111)
    }

    // DOOR TILE IGNORED FOR SPRITES
    testUpdateUniqueTag(spr(TextureList.DOORS.DOORTILE2, 1203, 500), Map(500 -> 111), 1203, 500)
  }

  @Test
  def updateUniqueTagInPlaceForWall(): Unit = {
    val cfg = DukeConfig.empty

    val wall1 = new Wall(0, 0, TextureList.DOORS.DOORTILE3)
    wall1.setLotag(42)

    Assert.assertEquals(42, wall1.getLotag)
    cfg.updateUniqueTagInPlace(wall1, Map(42 -> 16))
    Assert.assertEquals(16, wall1.getLotag)

    val wall2 = new Wall(0, 0, 353) // not a door
    wall2.setLotag(42)
    Assert.assertEquals(42, wall2.getLotag)
    cfg.updateUniqueTagInPlace(wall2, Map(42 -> 16))
    Assert.assertEquals(42, wall2.getLotag)

    Seq(TextureList.ForceFields.BIGFORCE, TextureList.ForceFields.W_FORCEFIELD).foreach { force =>
      val wall3 = wall(force, 0, 42)
      Assert.assertEquals(42, wall3.getLotag)
      cfg.updateUniqueTagInPlace(wall3, Map(42 -> 16))
      Assert.assertEquals(16, wall3.getLotag)

      // dont touch zeros
      val wall4 = wall(force, 0, 0)
      Assert.assertEquals(0, wall4.getLotag)
      cfg.updateUniqueTagInPlace(wall4, Map(0 -> 16))
      Assert.assertEquals(0, wall4.getLotag)

      // now as masked walls
      val wall5 = wall(355, 0, 42, force)
      Assert.assertEquals(42, wall5.getLotag)
      cfg.updateUniqueTagInPlace(wall5, Map(42 -> 16))
      Assert.assertEquals(16, wall5.getLotag)

      val wall6 = wall(355, 0, 0, force)
      Assert.assertEquals(0, wall6.getLotag)
      cfg.updateUniqueTagInPlace(wall6, Map(0 -> 16))
      Assert.assertEquals(0, wall6.getLotag)
    }
  }

}
