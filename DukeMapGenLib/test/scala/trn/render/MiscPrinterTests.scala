package trn.render

import org.junit.{Assert, Test}
import trn.PointXY

class MiscPrinterTests {

  private def p(x: Int, y: Int): PointXY = new PointXY(x, y)

  @Test
  def testWithP2(): Unit = {

    Assert.assertEquals(Seq.empty, MiscPrinter.withP2(Seq.empty))

    Assert.assertEquals(
      Seq((p(0, 0), p(1, 1)), (p(1, 1), p(2, 2)), (p(2, 2), p(3, 3)), (p(3, 3), p(0, 0))),
      MiscPrinter.withP2(Seq(p(0, 0), p(1, 1), p(2, 2), p(3, 3)))
    )

  }

}
