package trn.prefab

import org.junit.Assert
import org.junit.Test
import trn.duke.{TextureFont, TextureList}

class AutoTextTests {

  @Test
  def testSimpleFont(): Unit = {
    val tex1 = new TextureFont(10, "abc")
    Assert.assertFalse(tex1.contains(9))
    Assert.assertTrue(tex1.contains(10))
    Assert.assertTrue(tex1.contains(11))
    Assert.assertTrue(tex1.contains(12))
    Seq(13, 68, 69, 70, 71, 72).foreach(t => Assert.assertFalse(tex1.contains(t)))
    Assert.assertEquals(10, tex1.textureFor("a"))
    Assert.assertEquals(11, tex1.textureFor("b"))
    Assert.assertEquals(12, tex1.textureFor("c"))

    val tex2 = tex1.addedTo(new TextureFont(69, "!@#"))
    Seq(10, 11, 12, 69, 70, 71).foreach(t => Assert.assertTrue(tex2.contains(t)))
    Seq(8, 9, 13, 14, 67, 68, 72, 73).foreach(t => Assert.assertFalse(tex2.contains(t)))
    Assert.assertEquals(69, tex2.textureFor(("!")))
    Assert.assertEquals(70, tex2.textureFor(("@")))
    Assert.assertEquals(71, tex2.textureFor(("#")))
  }

  @Test
  def testTextureDetection(): Unit = {
    Assert.assertTrue(TextureList.FONT_BIGRED.contains(2930))
    Assert.assertTrue(TextureList.isFontTex(2938))
  }

}
