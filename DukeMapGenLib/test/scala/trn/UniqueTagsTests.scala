package trn

import org.junit.{Assert, Test}
import trn.duke.{Lotags, TextureList}
import trn.prefab.{DukeConfig, GameConfig}
import trn.{Map => DMap}

class UniqueTagsTests {

  val sectorId = 0

  def spr(tex: Int, hitag: Int, lotag: Int): Sprite = new Sprite(PointXYZ.ZERO, sectorId, tex, hitag, lotag)

  def se(hitag: Int, lotag: Int): Sprite = spr(TextureList.SE, hitag, lotag)

  def w(tex: Int, lotag: Int): Wall = {
    val ww = new Wall(0, 0, tex)
    ww.setLotag(lotag)
    ww
  }

  @Test
  def testFindGap(): Unit = {
    Assert.assertEquals(0, UniqueTags.findGap(Set.empty, 0, 1))
    Assert.assertEquals(100, UniqueTags.findGap(Set.empty, 100, 1))
    Assert.assertEquals(0, UniqueTags.findGap(Set(1), 0, 1))
    Assert.assertEquals(1, UniqueTags.findGap(Set(0), 0, 1))
    Assert.assertEquals(2, UniqueTags.findGap(Set(0,1), 0, 1))

    Assert.assertEquals(0, UniqueTags.findGap(Set.empty, 0, 2))
    Assert.assertEquals(1, UniqueTags.findGap(Set(0), 0, 2))
    Assert.assertEquals(2, UniqueTags.findGap(Set(1), 0, 2))
    Assert.assertEquals(0, UniqueTags.findGap(Set(2), 0, 2))
    Assert.assertEquals(0, UniqueTags.findGap(Set(3), 0, 2))

    Assert.assertEquals(36, UniqueTags.findGap(Set(9, 17, 23, 28, 32, 35, 46), 0, 10))
    Assert.assertEquals(0, UniqueTags.findGap(Set(10, 18, 24, 29, 33, 36, 47), 0, 10))
    Assert.assertEquals(37, UniqueTags.findGap(Set(10, 18, 24, 29, 33, 36, 47), 1, 10))
  }

  @Test
  def testFindSingleGaps(): Unit = {
    Assert.assertTrue(UniqueTags.findSingleGaps(Set.empty, 0, 0) == Seq.empty)
    Assert.assertTrue(UniqueTags.findSingleGaps(Set.empty, 0, 1) == Seq(0))
    Assert.assertTrue(UniqueTags.findSingleGaps(Set.empty, 0, 2) == Seq(0, 1))
    Assert.assertTrue(UniqueTags.findSingleGaps(Set.empty, 1, 0) == Seq.empty)
    Assert.assertTrue(UniqueTags.findSingleGaps(Set.empty, 1, 1) == Seq(1))
    Assert.assertTrue(UniqueTags.findSingleGaps(Set.empty, 1, 2) == Seq(1, 2))

    Assert.assertTrue(UniqueTags.findSingleGaps(Set(1), 0, 0) == Seq.empty)
    Assert.assertTrue(UniqueTags.findSingleGaps(Set(1), 0, 1) == Seq(0))
    Assert.assertTrue(UniqueTags.findSingleGaps(Set(1), 0, 2) == Seq(0, 2))
    Assert.assertTrue(UniqueTags.findSingleGaps(Set(1), 1, 0) == Seq.empty)
    Assert.assertTrue(UniqueTags.findSingleGaps(Set(1), 1, 1) == Seq(2))
    Assert.assertTrue(UniqueTags.findSingleGaps(Set(1), 1, 2) == Seq(2, 3))

    Assert.assertTrue(UniqueTags.findSingleGaps(Set(2, 3, 5, 7, 11, 13), 0, 9) == Seq(0, 1, 4, 6, 8, 9, 10, 12, 14))
    Assert.assertTrue(UniqueTags.findSingleGaps(Set(4, 5, 6, 7, 8, 9), 0, 6) == Seq(0, 1, 2, 3, 10, 11))
  }

  @Test
  def testUsedUniqueTags(): Unit = {

    val map = DMap.createNew()
    val cfg = DukeConfig.empty

    map.addSprite(se(10, 0)) // there IS an SE 0, its rotate sector
    map.addSprite(se(11, 1))
    map.addSprite(se(12, 2)) // SE 2 does not involve a unique tag
    map.addSprite(se(13, 3))
    map.addSprite(se(13, 6))
    map.addSprite(se(14, 6))
    map.addSprite(se(14, 6))

    // println(MapUtilScala.usedUniqueTags(cfg, map))
    Assert.assertTrue(UniqueTags.usedUniqueTags(cfg, map) == Set(10, 11, 13, 14))
  }

  @Test
  def testGetUniqueTagCopyMap(): Unit = {
    val cfg: GameConfig = DukeConfig.empty

    val sourceSprites = Set[Sprite](
      se(1, Lotags.SE.ROTATE), // this gets mapped
      se(1, 2),
      se(1, 4),
      se(1, 5),
      se(123, Lotags.SE.TWO_WAY_TRAIN) // 123,124, 125
    )
    val sourceWalls: Set[Wall] = Set(
      w(355, 5),
      w(TextureList.DOORS.DOORTILE1, 6000) // this gets mapped
    )

    // test0 - empty unique tags
    val results0 = UniqueTags.getUniqueTagCopyMap(cfg, sourceSprites, sourceWalls, Set.empty[Int])
    val expected0 = scala.collection.immutable.Map(123 -> 1, 124 -> 2, 125 -> 3, 1 -> 4, 6000 -> 5)
    Assert.assertTrue(results0 == expected0)

    // test1 - TWO WAY processed first (also, note that is skips zero)
    val usedUniqueDestTags1: Set[Int] = Set(4, 5, 7, 9) // 1, 2, 3, 6, 8
    Assert.assertEquals(0, UniqueTags.findGap(usedUniqueDestTags1, 0, 3))
    val results1 = UniqueTags.getUniqueTagCopyMap(cfg, sourceSprites, sourceWalls, usedUniqueDestTags1)
    val expected1 = scala.collection.immutable.Map(123 -> 1, 124 -> 2, 125 -> 3, 1 -> 6, 6000 -> 8)
    Assert.assertTrue(results1 == expected1)

    // test2 - TWO WAY processed first; not enought room
    val usedUniqueDestTags2: Set[Int] = Set(0, 1, 3, 7, 8) // 2, 4, 5, 8
    val results2 = UniqueTags.getUniqueTagCopyMap(cfg, sourceSprites, sourceWalls, usedUniqueDestTags2)
    val expected2 = scala.collection.immutable.Map(123 -> 4, 124 -> 5, 125 -> 6, 1 -> 2, 6000 -> 9)
    Assert.assertTrue(results2 == expected2)

    // TODO - do a test with no groups, and a test with no singles,

  }

  @Test
  def testGetUniqueTagCopyMapNoGroups(): Unit = {
    val cfg: GameConfig = DukeConfig.empty

    val sourceSprites = Set[Sprite](
      se(1, Lotags.SE.ROTATE), // this gets mapped
      se(1, 2),
      se(1, 4),
      se(1, 5),
    )
    val sourceWalls: Set[Wall] = Set(
      w(355, 5),
      w(TextureList.DOORS.DOORTILE1, 6000) // this gets mapped
    )

    val results0 = UniqueTags.getUniqueTagCopyMap(cfg, sourceSprites, sourceWalls, Set.empty[Int])
    val expected0 = scala.collection.immutable.Map(1 -> 1, 6000 -> 2)
    Assert.assertTrue(results0 == expected0)

    val usedUniqueDestTags1: Set[Int] = Set(1, 3, 5)
    val results1 = UniqueTags.getUniqueTagCopyMap(cfg, sourceSprites, sourceWalls, usedUniqueDestTags1)
    val expected1 = scala.collection.immutable.Map(1 -> 2, 6000 -> 4)
    Assert.assertTrue(results1 == expected1)
  }

  @Test
  def testGetUniqueTagCopyMapNoSingles(): Unit = {
    val cfg: GameConfig = DukeConfig.empty

    val sourceSprites = Set[Sprite](
      se(123, Lotags.SE.TWO_WAY_TRAIN), // 123, 124, 125
      spr(TextureList.Switches.MULTI_SWITCH, 600, 300) // uses the lotag: 300, 301, 302, 303
    )
    val sourceWalls: Set[Wall] = Set(
      w(355, 5),
    )

    val results0 = UniqueTags.getUniqueTagCopyMap(cfg, sourceSprites, sourceWalls, Set.empty[Int])
    val expected0 = scala.collection.immutable.Map(123 -> 1, 124 -> 2, 125 -> 3, 300 -> 4, 301 -> 5, 302 -> 6, 303 -> 7)
    Assert.assertTrue(results0 == expected0)

    val usedUniqueDestTags1: Set[Int] = Set(2, 3, 5, 7, 11, 13, 17, 19, 27)
    val results1 = UniqueTags.getUniqueTagCopyMap(cfg, sourceSprites, sourceWalls, usedUniqueDestTags1)
    val expected1 = scala.collection.immutable.Map(123 -> 20, 124 -> 21, 125 -> 22, 300 -> 23, 301 -> 24, 302 -> 25, 303 -> 26)
    Assert.assertTrue(results1 == expected1)

  }
}
