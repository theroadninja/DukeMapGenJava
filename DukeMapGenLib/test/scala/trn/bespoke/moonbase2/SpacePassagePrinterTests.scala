package trn.bespoke.moonbase2

import org.junit.function.ThrowingRunnable
import org.junit.{Assert, Test}
import trn.PointXY
import trn.render.{Texture, WallAnchor}

class SpacePassagePrinterTests {

  private def p(x: Int, y: Int): PointXY = new PointXY(x, y)

  val tex = ElevatorPassageConfig.testDefaults()

  @Test
  def testElevatorControlPoints(): Unit = {


    // passage not wide enough
    val throw1: ThrowingRunnable = () => {
      SpacePassagePrinter.elevatorControlPoints(
        WallAnchor(p(0, 0), p(0, -10), 0, 0),
        WallAnchor(p(2048, -10), p(2048, -10), 0, 0),
        1024
      )
    }
    Assert.assertThrows(classOf[Exception], throw1)

    // the wall anchors are facing away from each other
    val throw2: ThrowingRunnable = () => {
      SpacePassagePrinter.elevatorControlPoints(
        WallAnchor(p(2048, 0), p(2048, -2048), 0, 0),
        WallAnchor(p(0, 0), p(0, -2048), 0, 0),
        1024
      )
    }
    Assert.assertThrows(classOf[Exception], throw2)

    // intersects
    val throw3: ThrowingRunnable = () => {
      SpacePassagePrinter.elevatorControlPoints(
        WallAnchor(p(0, -2048), p(1024, -2048), 0, 0),
        WallAnchor(p(0, 4096), p(1024, 4096), 0, 0),
        1024
      )
    }
    Assert.assertThrows(classOf[Exception], throw3)

    // horizontal one
    val result = SpacePassagePrinter.elevatorControlPoints(
      WallAnchor(p(0, 0), p(0, -2048), 0, 0),
      WallAnchor(p(2048, -2048), p(2048, 0), 0, 0),
      1024
    )
    Assert.assertEquals(p(512, -2048), result._1)
    Assert.assertEquals(p(512+1024, -2048), result._2)
    Assert.assertEquals(p(512, 0), result._4)
    Assert.assertEquals(p(512+1024, 0), result._3)
    Assert.assertEquals(512, result._5) // length from wallA to lift
    Assert.assertEquals(512, result._6) // length from wallA to lift

    val result2 = SpacePassagePrinter.elevatorControlPoints(
      WallAnchor(p(0, -2048), p(1024, -2048), 0, 0),
      WallAnchor(p(1024, 4096), p(0, 4096), 0, 0),
      1024
    )
    Assert.assertEquals(p(1024, -2048 + 2560), result2._1)
    Assert.assertEquals(p(1024, -2048 + 2560 + 1024), result2._2)
    Assert.assertEquals(p(0, -2048 + 2560), result2._4)
    Assert.assertEquals(p(0, -2048 + 2560 + 1024), result2._3)
  }

}
