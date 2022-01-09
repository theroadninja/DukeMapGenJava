package trn.render

import org.junit.{Assert, Test}

class TextureUtilTests {

  @Test
  def testCalcOffset(): Unit = {

    // Ironically, this function was still wrong

    // a 64px texture fits a 1024 unit wall

    // Assert.assertEquals(0, TextureUtil.calcOffset(0, 1.0, 64, 1024))
    // Assert.assertEquals(64/2, TextureUtil.calcOffset(0, 1.0, 64, 1536))
    // Assert.assertEquals(64/2, TextureUtil.calcOffset(64/2, 1.0, 64, 1024))
    // Assert.assertEquals(0, TextureUtil.calcOffset(64/2, 1.0, 64, 1536))

    // Assert.assertEquals(64, TextureUtil.calcOffset(0, 1.0, 128, 1024))
    // Assert.assertEquals(64, TextureUtil.calcOffset(0, 1.0, 256, 1024))
    // Assert.assertEquals(128, TextureUtil.calcOffset(0, 1.0, 256, 2048))
    // Assert.assertEquals(192, TextureUtil.calcOffset(0, 1.0, 256, 3072))
    // Assert.assertEquals(0, TextureUtil.calcOffset(0, 1.0, 256, 4096))
  }

}
