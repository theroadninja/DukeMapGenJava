package trn.prefab

import org.junit.{Assert, Test}

class SectorGroupBaseTests {

  private lazy val testPalette: PrefabPalette = PrefabPalette.fromMap(TestUtils.load("BB.MAP"), true)

  /**
    * Sector group with one square sector, 1024 in both directions.
    */
  val Group1 = 1

  /**
    *  +--+--+
    *  |  |  |
    *  +--+--+
    */
  val Group2 = 2

  /**
    *  +--+--+
    *  |  |  |
    *  +--+--+
    *  |  |
    *  +--+
    */
  val Group3 = 3

  /**
    * +-----+
    * | +---+
    * | |
    * | | +-+
    * | | | |
    * | | +-+
    * | |
    * | +---+
    * +-----+
    */
  val Group4 = 4

  /**
    * +-----+
    * | +---+
    * | |
    * | | +--+
    * | | |  /
    * | | +-+
    * | |
    * | +---+
    * +-----+
    */
  val Group5 = 5


  private def assertSameShape(bb: BoundingBox, w: Int, h: Int): Unit = {
    Assert.assertEquals(bb.w, w) // makes for better error messages
    Assert.assertTrue(bb.sameShape(BoundingBox(0, 0, w, h)))
  }
  private def assertSameShape(sg: SectorGroupBase, w: Int, h: Int): Unit = assertSameShape(sg.boundingBox, w, h)

  @Test
  def testFineBoundingBoxes(): Unit = {

    // TODO - make sure all fine bounding boxes are always inside the big one

    val sg = testPalette.getSG(Group1)
    Assert.assertTrue(sg.boundingBox.sameShape(BoundingBox(0, 0, 1024, 1024)))
    val bbs = sg.fineBoundingBoxes
    Assert.assertEquals(1, bbs.size)
    Assert.assertTrue(bbs(0).sameShape(BoundingBox(0, 0, 1024, 1024)))

    val sg2 = testPalette.getSG(Group2)
    Assert.assertTrue(sg2.boundingBox.sameShape(BoundingBox(0, 0, 2048, 1024)))
    Assert.assertEquals(2, sg2.fineBoundingBoxes.size)
    val bbs2 = sg2.fineBoundingBoxes
    Assert.assertTrue(bbs2(0).sameShape(BoundingBox(0, 0, 1024, 1024)))
    Assert.assertTrue(bbs2(1).sameShape(BoundingBox(0, 0, 1024, 1024)))

    val sg3 = testPalette.getSG(Group3)
    assertSameShape(sg3, 2 * 1024, 2 * 1024)
    Assert.assertEquals(3, sg3.fineBoundingBoxes.size)
    for(i <- 0 until 3) {
      val bbs = sg3.fineBoundingBoxes
      assertSameShape(bbs(i), 1024, 1024)
      Assert.assertNotEquals(bbs(i).topLeft, bbs((i+1) % bbs.size))
    }

    val sg4 = testPalette.getSG(Group4)
    assertSameShape(sg4, 2 * 1024, 3 * 1024)
    Assert.assertEquals(1, sg4.fineBoundingBoxes.size)
    assertSameShape(sg4.fineBoundingBoxes(0), 2 * 1024, 3 * 1024)

    val sg5 = testPalette.getSG(Group5)
    assertSameShape(sg5, 2048 + 32, 3 * 1024)
    Assert.assertEquals(2, sg5.fineBoundingBoxes.size)
    assertSameShape(sg5.fineBoundingBoxes(0), 2048, 3 * 1024)
    assertSameShape(sg5.fineBoundingBoxes(1), 1024 + 32, 1 * 1024)
  }


}
