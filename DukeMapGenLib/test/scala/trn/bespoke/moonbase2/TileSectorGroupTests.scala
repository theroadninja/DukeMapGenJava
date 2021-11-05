package trn.bespoke.moonbase2

import org.junit.{Assert, Test}
import trn.AngleUtil
import trn.logic.Tile2d
import trn.logic.Tile2d._
import trn.math.SnapAngle
import trn.prefab.{Heading, PrefabPalette, TestUtils}
class TileSectorGroupTests {

  private lazy val testPalette: PrefabPalette = PrefabPalette.fromMap(TestUtils.load(TestUtils.MapWriterMap), true)

  @Test
  def testRotateToOneWay(): Unit = {
    val sg = testPalette.getSG(1) // just grab any old sector
    val tsg = TileSectorGroup.oneWay("1", Tile2d(Conn, Blocked, Conn, Blocked), sg, Set(RoomTags.OneWay), AngleUtil.ANGLE_RIGHT)

    Assert.assertEquals(Some(Tile2d(2, Blocked, Conn, Blocked)), tsg.oneWayTile)

    Assert.assertEquals(SnapAngle(0), tsg.rotateAngleToOneWayTarget(Tile2d(2, Blocked, Conn, Blocked)))
    Assert.assertEquals(SnapAngle(2), tsg.rotateAngleToOneWayTarget(Tile2d(Conn, Blocked, 2, Blocked)))

    val rotated = tsg.rotatedCW
    Assert.assertEquals(Some(Tile2d(Blocked, 2, Blocked, Conn)), rotated.oneWayTile)
    Assert.assertEquals(Heading.S, rotated.oneWayHigherSideHeading.get)

  }

}
