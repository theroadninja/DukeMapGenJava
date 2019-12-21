package trn.prefab

import org.junit.Assert
import org.junit.Test

class AngleUtilScalaTests {
  val OfficeChairTex: Int = 556

  private lazy val testPalette: PrefabPalette = PrefabPalette.fromMap(TestUtils.load("UNIT.MAP"), true)

  @Test
  def testAngleToHeading(): Unit = {
    val sg = testPalette.getSG(5)

    val chairs = sg.sprites.filter(s => s.getLotag == 1 && s.getTex == OfficeChairTex)

    val east = chairs.find(_.getHiTag == 0).get
    val south = chairs.find(_.getHiTag == 1).get
    val west = chairs.find(_.getHiTag == 2).get
    val north = chairs.find(_.getHiTag == 3).get
    val invalid = chairs.find(_.getHiTag == 4).get

    Assert.assertEquals(Heading.E, Heading.fromDukeAngle(east.getAngle))
    Assert.assertEquals(Heading.S, Heading.fromDukeAngle(south.getAngle))
    Assert.assertEquals(Heading.W, Heading.fromDukeAngle(west.getAngle))
    Assert.assertEquals(Heading.N, Heading.fromDukeAngle(north.getAngle))
    Assert.assertEquals(None, Option(Heading.fromDukeAngle(invalid.getAngle)))
  }
}
