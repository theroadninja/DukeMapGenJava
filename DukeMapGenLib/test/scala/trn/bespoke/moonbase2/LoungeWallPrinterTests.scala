package trn.bespoke.moonbase2

import org.junit.{Assert, Test}
import trn.PointXY

class LoungeWallPrinterTests {

  private def p(x: Int, y: Int): PointXY = new PointXY(x, y)

  @Test
  def testChairControlPoints(): Unit = {
    val results2 = LoungeWallPrinter.chairControlPoints(p(0, 0), p(1000000, 0), 2)

    Assert.assertEquals(Seq(p(0, 0), p(0, 128), p(0, 384), p(512, 384), p(512, 128), p(512, 0)), results2)
  }

  @Test
  def testMedCabinetControlPoints(): Unit = {
    val p0 = p(0, 0)
    val results = LoungeWallPrinter.medCabinetControlPoints(p0, new PointXY(4096, 0))
    Assert.assertEquals(
      Seq(p(128, 0), p(128, -32), p(128, -64), p(128, -256), p(640, -256), p(640, -64), p(640, -32), p(640, 0), p(768, 0)),
      results
    )
  }

  @Test
  def testSecurityScreenCtlPoints(): Unit = {
    val results = LoungeWallPrinter.securityScreenCtrlPoints(p(0, 0), p(4096, 0))
    Assert.assertEquals(
      Seq(p(128, 0), p(128, -32), p(128, -64), p(640, -64), p(640, -32), p(640, 0), p(768, 0)),
      results
    )
  }
}
