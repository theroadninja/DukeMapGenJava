package trn.bespoke.moonbase2

import org.junit.{Assert, Test}
import trn.RandomX

class LoungePlanner2Tests {

  @Test
  def testSplit(): Unit = {
    Assert.assertEquals((0, 0), LoungePlanner2.split(0))
    Assert.assertEquals((0, 1), LoungePlanner2.split(1))
    Assert.assertEquals((2, 2), LoungePlanner2.split(4))
    Assert.assertEquals((2, 3), LoungePlanner2.split(5))
  }

  @Test
  def testPlanSmall(): Unit = {
    val r = RandomX()
    (0 to 2048).foreach { i =>
      // mostly just make sure the require()s dont trip
      val results = LoungePlanner2.planSmall(r, None, Some(i), Set.empty)
      Assert.assertEquals(1, results.length)
    }
  }


  @Test
  def testPlanChair(): Unit = {
    // mostly just make sure the require()s dont trip
    (1024 to 4608).foreach { i =>
      LoungePlanner2.planChairGroup(i)
    }
  }

  @Test
  def testPlan(): Unit = {
    // mostly just make sure the require()s dont trip
    val r = RandomX()
    (0 to 32768).foreach { i =>
      LoungePlanner2.planWall(i, Set.empty, r)
    }
  }

}
