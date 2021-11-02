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
  def testSecurityScreenCtrlPoints(): Unit = {
    val results = LoungeWallPrinter.securityScreenCtrlPoints(p(0, 0), p(4096, 0))
    Assert.assertEquals(
      Seq(p(128, 0), p(128, -32), p(128, -64), p(640, -64), p(640, -32), p(640, 0), p(768, 0)),
      results
    )
  }

  @Test
  def testWaterFountainCtrlPoints(): Unit = {
    val results = LoungeWallPrinter.waterFountainCtrlPoints(p(0, 0), p(4096, 0))
    Assert.assertEquals(
      Seq(p(192, 0), p(192, -192), p(576, -192), p(576, 0), p(768, 0)),
      results
    )
  }

  @Test
  def testBulkheadCtrlPoints(): Unit = {
    val results = LoungeWallPrinter.bulkheadCtlPoints(p(0, 0), p(4096, 0), 2048)
    Assert.assertEquals(Seq(p(256, 0), p(704, 448), p(1344, 448), p(1792, 0), p(2048, 0)), results)

    val results2 = LoungeWallPrinter.bulkheadCtlPoints(p(0, 0), p(4096, 0), 3072)
    Assert.assertEquals(Seq(p(256, 0), p(704, 448), p(1344 + 1024, 448), p(1792 + 1024, 0), p(2048 + 1024, 0)), results2)
  }

  @Test
  def testWindowCtrlPoints(): Unit = {
    val results = LoungeWallPrinter.windowCtrlPoints(p(0, 0), p(4096, 0), 384)
    Assert.assertEquals(Seq(p(64, 0), p(320, 0), p(32, -128), p(64,-128), p(320,-128), p(352,-128), p(192,-160), p(192,-192), p(384, 0)), results)
  }

  @Test
  def testFanCtrlPoints(): Unit = {
    val results = LoungeWallPrinter.fanCtrlPoints(p(0, 0), p(4096, 0), 2048)
    Assert.assertEquals(Seq(p(256, 0), p(1792, 0), p(1792, 128), p(256, 128), p(2048, 0)), results)
  }
}
