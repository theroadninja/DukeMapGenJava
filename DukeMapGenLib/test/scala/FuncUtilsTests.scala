import org.junit.{Assert, Test}
import trn.FuncUtils

import trn.FuncImplicits._

class FuncUtilsTests {

  @Test
  def histogramTests(): Unit = {

    Assert.assertEquals(
      FuncUtils.histogram(Seq(), (i: Int) => i),
      Map()
    )

    Assert.assertEquals(
      FuncUtils.histogram(Seq(1,2,3), (i: Int) => i),
      Map(
        1 -> Seq(1),
        2 -> Seq(2),
        3 -> Seq(3)
      )
    )

    Assert.assertEquals(
      FuncUtils.histogram(Seq(1,2,3)),
      Map(
        1 -> Seq(1),
        2 -> Seq(2),
        3 -> Seq(3)
      )
    )

    Assert.assertEquals(
      FuncUtils.histogram(Seq(1,2,2,3,3,3), (i: Int) => i),
      Map(
        1 -> Seq(1),
        2 -> Seq(2,2),
        3 -> Seq(3,3,3)
      )
    )

    Assert.assertEquals(
      FuncUtils.histogram(Seq(1,2,2,3,3,3), (i: Int) => i + 1),
      Map(
        2 -> Seq(1),
        3 -> Seq(2,2),
        4 -> Seq(3,3,3)
      )
    )

    Assert.assertEquals(
      FuncUtils.histogram(Seq(1,2,2,3,3,3), (i: Int) => i.toString),
      Map(
        "1" -> Seq(1),
        "2" -> Seq(2,2),
        "3" -> Seq(3,3,3)
      )
    )

  }

  @Test
  def histogramImplicitTests(): Unit = {

    Assert.assertEquals(
      Seq(1,2,3).histogram((i: Int) => i),
      Map(
        1 -> Seq(1),
        2 -> Seq(2),
        3 -> Seq(3)
      )
    )

    Assert.assertEquals(
      Set(1,2,3).histogram((i: Int) => i),
      Map(
        1 -> Seq(1),
        2 -> Seq(2),
        3 -> Seq(3)
      )
    )

    Assert.assertEquals(
      Seq(1, 2, 3).map(_ * 2).histogram(),
      Map(
        2 -> Seq(2),
        4 -> Seq(4),
        6 -> Seq(6),
      )
    )


  }

  @Test
  def duplicateTests(): Unit = {
    Assert.assertEquals(Seq(), Seq(1,2,3,4,5).duplicates)
    Assert.assertEquals(Seq(1), Seq(1,1,2,3).duplicates)
    Assert.assertEquals(Seq(1), List(1,1,2,3).duplicates)
    Assert.assertEquals(Seq(1,2,3), List(1,1,2,2,3,3).duplicates.toSeq.sorted)
    Assert.assertEquals(Seq("1"), Seq("1", "1", "2", "3").duplicates)
  }


}
