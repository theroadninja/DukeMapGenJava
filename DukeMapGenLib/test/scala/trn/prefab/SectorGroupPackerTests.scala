package trn.prefab

import org.junit.{Assert, Test}
import trn.PointXY

class SectorGroupPackerTests {

  val topLeft = new PointXY(100, 200)
  val bottomRight = new PointXY(800, 400)

  val bb100 = new BoundingBox(-2000, -2000, 100-2000, 100-2000)

  @Test
  def packingTests(): Unit = {
    val packer = new SimpleSectorGroupPacker(topLeft, bottomRight, 0)
    Assert.assertTrue(packer.canFit(BoundingBox(-100, -100, 0, 0)))
    Assert.assertTrue(packer.canFit(BoundingBox(-100, -100, 600, 0)))
    Assert.assertFalse(packer.canFit(BoundingBox(-100, -100, 601, 0)))
    Assert.assertTrue(packer.canFit(BoundingBox(-100, 0, 600, 200)))
    Assert.assertFalse(packer.canFit(BoundingBox(-100, 0, 600, 201)))
    for(_ <- 0 until 7){
      Assert.assertTrue(packer.canFit(bb100))
      packer.reserveArea(bb100)
    }
    Assert.assertFalse(packer.canFit(bb100))
  }

  @Test
  def packingTestsWithMargin(): Unit = {
    val packer = new SimpleSectorGroupPacker(topLeft, bottomRight, 100)
    for(_ <- 0 until 4){ // it can fit 4 because the margin doest have to be in bounds
      Assert.assertTrue(packer.canFit(bb100))
      packer.reserveArea(bb100)
    }
    Assert.assertFalse(packer.canFit(bb100))

  }

  @Test(expected=classOf[Exception])
  def badPackTest(): Unit = {
    val packer = new SimpleSectorGroupPacker(topLeft, bottomRight, 50)
    packer.reserveArea(bb100)
    packer.reserveArea(bb100)
    packer.reserveArea(bb100)
    packer.reserveArea(bb100)
    packer.reserveArea(bb100)
    packer.reserveArea(bb100)
    packer.reserveArea(bb100)
    packer.reserveArea(bb100)
  }

}
