package trn.prefab

import scala.collection.mutable.ListBuffer
import org.junit.Assert
import org.junit.Test
import org.junit.Before
import trn.{PointXY, PointXYZ}

class BoundingBoxTests {

  private def b(xmin: Int, ymin: Int, xmax: Int, ymax: Int): BoundingBox = BoundingBox(xmin, ymin, xmax, ymax)

  private def p(x: Int, y: Int): PointXY = new PointXY(x, y)

  private def P(x: Int, y: Int, z: Int): PointXYZ = new PointXYZ(x, y, z)

  @Test
  def testBoundingBox: Unit = {

    BoundingBox(0, 0, 0, 0) // this should be allowed
    Assert.assertEquals(p(0, 0), BoundingBox(0, 0, 0, 0).center)

    val bb = BoundingBox(0, 0, 10, 10)

    Assert.assertEquals(BoundingBox(0, 0, 10, 10), BoundingBox(0, 0, 10, 10).add(5, 5))

    // x min
    Assert.assertEquals(BoundingBox(4, 0, 10, 10), BoundingBox(4, 0, 10, 10).add(5, 5))
    Assert.assertEquals(BoundingBox(5, 0, 10, 10), BoundingBox(5, 0, 10, 10).add(5, 5))
    Assert.assertEquals(BoundingBox(5, 0, 10, 10), BoundingBox(6, 0, 10, 10).add(5, 5))

    // x max
    Assert.assertEquals(BoundingBox(0, 0, 10, 10), BoundingBox(0, 0, 10, 10).add(9, 5))
    Assert.assertEquals(BoundingBox(0, 0, 10, 10), BoundingBox(0, 0, 10, 10).add(10, 5))
    Assert.assertEquals(BoundingBox(0, 0, 11, 10), BoundingBox(0, 0, 10, 10).add(11, 5))

    // y min
    Assert.assertEquals(BoundingBox(0, 4, 10, 10), BoundingBox(0, 4, 10, 10).add(5, 5))
    Assert.assertEquals(BoundingBox(0, 5, 10, 10), BoundingBox(0, 5, 10, 10).add(5, 5))
    Assert.assertEquals(BoundingBox(0, 5, 10, 10), BoundingBox(0, 6, 10, 10).add(5, 5))

    // y max
    Assert.assertEquals(BoundingBox(0, 0, 10, 10), BoundingBox(0, 0, 10, 10).add(5, 9))
    Assert.assertEquals(BoundingBox(0, 0, 10, 10), BoundingBox(0, 0, 10, 10).add(5, 10))
    Assert.assertEquals(BoundingBox(0, 0, 10, 11), BoundingBox(0, 0, 10, 10).add(5, 11))

    // x min, y max
    Assert.assertEquals(BoundingBox(-1, 0, 10, 11), BoundingBox(0, 0, 10, 10).add(-1, 11))

    // x max, y min
    Assert.assertEquals(BoundingBox(0, -2, 12, 10), BoundingBox(0, 0, 10, 10).add(12, -2))
  }

  @Test
  def testSameShape: Unit = {
    Assert.assertTrue(b(0, 0, 0, 0).sameShape(b(1, 1, 1, 1)))
    Assert.assertTrue(b(0, 0, 10, 10).sameShape(b(10, 10, 20, 20)))
    Assert.assertTrue(b(0, 0, 10, 20).sameShape(b(15, 10, 25, 30)))
    Assert.assertFalse(b(0, 0, 10, 20).sameShape(b(15, 10, 30, 25)))

  }

  @Test
  def testArea: Unit = {
    for(i <- -100 to 100){
      Assert.assertEquals(0, b(i, i, i, i).area)
      Assert.assertEquals(0, b(i, 5, i, 5).area)
      Assert.assertEquals(0, b(-2, i, -2, i).area)
    }
    Assert.assertEquals(25, b(0, 0, 5, 5).area)
    Assert.assertEquals( 5, b(0, 0, 1, 5).area)
    Assert.assertEquals( 5, b(0, -5, 1,0).area)
    Assert.assertEquals( 5, b(0, 0, 5, 1).area)
    Assert.assertEquals(25, b(-2, 0, 3, 5).area)
    Assert.assertEquals(32, b(-1, 0, 7, 4).area)
  }

  @Test
  def testFitsInside: Unit = {

    Assert.assertEquals(false, BoundingBox(0, 0, 10, 10).fitsInside(9, 9))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 10).fitsInside(9, 10))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 10).fitsInside(10, 9))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 10).fitsInside(9, 100))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 10).fitsInside(100, 9))

    Assert.assertEquals(true, BoundingBox(0, 0, 10, 10).fitsInside(10, 10))
    Assert.assertEquals(true, BoundingBox(0, 0, 10, 10).fitsInside(11, 10))
    Assert.assertEquals(true, BoundingBox(0, 0, 10, 10).fitsInside(10, 11))

    Assert.assertEquals(false, BoundingBox(0, 0, 10, 20).fitsInside(10, 10))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 20).fitsInside(15, 15))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 20).fitsInside(20, 15))
    Assert.assertEquals(false, BoundingBox(0, 0, 10, 20).fitsInside(21, 15))

    Assert.assertEquals(true, BoundingBox(0, 0, 10, 20).fitsInside(20, 20))
    Assert.assertEquals(true, BoundingBox(0, 0, 10, 20).fitsInside(21, 21))
  }


  @Test
  def testIsInsideInclusive: Unit = {
    Assert.assertTrue(b(0, 0, 1, 1).isInsideInclusive(b(0, 0, 1, 1)))
    Assert.assertTrue(b(1, 1, 2, 2).isInsideInclusive(b(1, 1, 2, 2)))
    Assert.assertTrue(b(5, 1, 100, 1).isInsideInclusive(b(5, 1, 100, 1)))

    Assert.assertTrue(b(0, -5, 10, 20).isInsideInclusive(b(0, -5, 10, 20)))
    Assert.assertTrue(b(0, -5,  9, 20).isInsideInclusive(b(0, -5, 10, 20)))
    Assert.assertTrue(b(0, -5, 10, 19).isInsideInclusive(b(0, -5, 10, 20)))
    Assert.assertTrue(b(1, -5, 10, 20).isInsideInclusive(b(0, -5, 10, 20)))
    Assert.assertTrue(b(0, -4, 10, 20).isInsideInclusive(b(0, -5, 10, 20)))

    Assert.assertFalse(b( 0,  0, 11, 20).isInsideInclusive(b(0, 0, 10, 20)))
    Assert.assertFalse(b( 0,  0, 10, 21).isInsideInclusive(b(0, 0, 10, 20)))
    Assert.assertFalse(b(-1,  0, 10, 20).isInsideInclusive(b(0, 0, 10, 20)))
    Assert.assertFalse(b( 0, -1, 10, 20).isInsideInclusive(b(0, 0, 10, 20)))

    Assert.assertTrue(b(-5, -5, 5, 5).isInsideInclusive(b(-20, -5, 5, 100)))
    Assert.assertFalse(b(-5, -5, 5, 5).isInsideInclusive(b(0, 0, 10, 10)))

    Assert.assertTrue(b(-5, -5, 0, 0).isInsideInclusive(b(-5, -5, 5, 5)))
    Assert.assertFalse(b(-5, -6, 0, 0).isInsideInclusive(b(-500, -5, 5, 5)))
    Assert.assertFalse(b(-6, -5, 0, 0).isInsideInclusive(b(-5, -500, 5, 5)))

    Assert.assertTrue(b(-5,  0, 0, 5).isInsideInclusive(b(-5, -5, 5, 5)))
    Assert.assertTrue(b( 0,  0, 5, 5).isInsideInclusive(b(-5, -5, 5, 5)))
    Assert.assertTrue(b( 0, -5, 5, 0).isInsideInclusive(b(-5, -5, 5, 5)))
  }

  @Test
  def testContains: Unit = {

    def containsAll(b: BoundingBox, points: PointXY*): Boolean = {
      points.foldLeft(true)(_ && b.contains(_))
    }
    def containsNone(b: BoundingBox, points: PointXY*): Boolean = {
      ! points.foldLeft(false)(_ || b.contains(_))
    }

    Assert.assertTrue(containsAll(b(10, 20, 30, 40),
      p(10, 20), p(15, 20), p(30, 20), p(30, 25), p(30, 30), p(30, 35), p(30, 40), p(15, 30), p(10, 40), p(10, 25),
      p(11, 21), p(15, 21), p(29, 21), p(29, 25), p(29, 30), p(29, 35), p(29, 39), p(15, 30), p(11, 39), p(11, 25),
      p(15, 31)
    ))
    Assert.assertTrue(containsNone(b(10, 20, 30, 40),
      p(9, 20), p(9, 19), p(10, 19), p(20, 19), p(30, 19), p(31, 19), p(31, 20), p(31, 40), p(30, 41), p(29, 41),
      p(10, 41), p(9, 41), p(9, 40), p(9, 15)
    ))

    Assert.assertTrue(containsAll(b(-60, -200, -30, -100),
      p(-60, -200), p(-60, -100), p(-30, -200), p(-30, -100), p(-50, -148)
    ))
    Assert.assertTrue(containsNone(b(-60, -200, -30, -100),
      p(60, 200), p(60, 100), p(30, 200), p(30, 100), p(50, 148),
      p(0, 0),
    ))
  }

  @Test
  def testPoints: Unit = {
    Assert.assertEquals(b(0, 0, 0, 0).points, Set(p(0, 0), p(0, 0), p(0, 0), p(0, 0)))
    Assert.assertEquals(b(1, 2, 3, 4).points, Set(p(1, 2), p(1, 4), p(3, 2), p(3, 4)))
    Assert.assertEquals(b(-5, -5, 10, 100).points, Set(p(-5, -5), p(-5, 100), p(10, -5), p(10, 100)))
  }

  @Test
  def testContainsAny: Unit = {
    // TODO - add scala test libs to this project, and use fancy scala test shit to access this as a private method
    Assert.assertTrue(b(0, 0, 0, 0).containsAny(p(0, 0)))
    Assert.assertFalse(b(0, 0, 0, 0).containsAny(p(1, 0)))
    Assert.assertFalse(b(0, 0, 0, 0).containsAny(p(1, 0), p(0, 1)))
    Assert.assertTrue(b(0, 0, 0, 0).containsAny(p(0, 0), p(0, 0)))
    Assert.assertTrue(b(0, 0, 0, 0).containsAny(p(0, 0), p(0, 1)))
  }

  @Test
  def testCenter: Unit = {
    Assert.assertEquals(p(15, 32), b(10, 0, 21, 64).center)
  }

  @Test
  def testIntersect: Unit = {
    Assert.assertEquals(b(0, 0, 0, 0).intersect(b(0, 0, 0, 0)), Some(b(0, 0, 0, 0)))
    Assert.assertEquals(b(0, 0, 0, 0).intersect(b(1, 1, 1, 1)), None)

    // boxes are equivalent
    Assert.assertEquals(b(0, 1, 10, 11).intersect(b(0, 1, 10, 11)), Some(b(0, 1, 10, 11)))
    Assert.assertEquals(b(0, 1, 10, 1).intersect(b(0, 1, 10, 1)), Some(b(0, 1, 10, 1)))
    Assert.assertEquals(b(0, 0, 10, 11).intersect(b(0, 0, 10, 11)), Some(b(0, 0, 10, 11)))
    Assert.assertEquals(b(-100, 1, 10, 11).intersect(b(-100, 1, 10, 11)), Some(b(-100, 1, 10, 11)))
    Assert.assertEquals(b(-50, -51, -10, -11).intersect(b(-50, -51, -10, -11)), Some(b(-50, -51, -10, -11)))
    Assert.assertEquals(b(-50, 11, -10, 52).intersect(b(-50, 11, -10, 52)), Some(b(-50, 11, -10, 52)))

    // lines
    Assert.assertEquals(b(2, 2, 40, 2).intersect(b(5, 0, 5, 100)), Some(b(5, 2, 5, 2)))
    Assert.assertEquals(b(2, 2, 40, 2).intersect(b(20, 2, 50, 2)), Some(b(20, 2, 40, 2)))
    Assert.assertEquals(b(2, 2, 40, 2).intersect(b(20, 3, 50, 3)), None)
    Assert.assertEquals(b(-50, 6, -50, 20).intersect(b(-50, 12, -50, 100)), Some(b(-50, 12, -50, 20)))
    Assert.assertEquals(b(-50, 6, -50, 20).intersect(b(-49, 12, -49, 100)), None)

    // one box inside the other
    Assert.assertEquals(b(0, 0, 100, 100).intersect(b(40, 40, 60, 60)), Some(b(40, 40, 60, 60)))
    Assert.assertEquals(b(0, 0, 100, 100).intersect(b(0, 40, 20, 60)), Some(b(0, 40, 20, 60)))
    Assert.assertEquals(b(0, 0, 100, 100).intersect(b(40, 40, 100, 60)), Some(b(40, 40, 100, 60)))
    Assert.assertEquals(b(0, 0, 100, 100).intersect(b(90, 90, 100, 100)), Some(b(90, 90, 100, 100)))

    // corner overlaps
    Assert.assertEquals(b(-12, -10, 10, 11).intersect(b(0, 0, 20, 20)), Some(b(0, 0, 10, 11)))
    Assert.assertEquals(b(-12, -10, 10, 11).intersect(b(11, 12, 20, 20)), None)
    Assert.assertEquals(b(-12, -10, 10, 11).intersect(b(0, -20, 20, 0)), Some(b(0, -10, 10, 0)))
    Assert.assertEquals(b(-12, -10, 10, 11).intersect(b(-20, -20, 0, 0)), Some(b(-12, -10, 0, 0)))
    Assert.assertEquals(b(-12, -10, 10, 11).intersect(b(-20, 0, 0, 20)), Some(b(-12, 0, 0, 11)))
    Assert.assertEquals(b(-12, -10, 10, 11).intersect(b(10, 11, 20, 20)), Some(b(10, 11, 10, 11)))
    Assert.assertEquals(b(-12, -10, 10, 11).intersect(b(10, -20, 20, -10)), Some(b(10, -10, 10, -10)))
    Assert.assertEquals(b(-12, -10, 10, 11).intersect(b(-20, -20, -12, -10)), Some(b(-12, -10, -12, -10)))
    Assert.assertEquals(b(-12, -10, 10, 11).intersect(b(-20, 11, -12, 20)), Some(b(-12, 11, -12, 11)))

    // side overlaps
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(-15, 15, 15, 100)), Some(b(-15, 15, 15, 20))) // TOP
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(-20, 15, 20, 100)), Some(b(-20, 15, 20, 20)))
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(-25, 15, 25, 100)), Some(b(-20, 15, 20, 20)))
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(-15, -20, 15, 100)), Some(b(-15, -20, 15, 20)))
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(-15, 20, 15, 100)), Some(b(-15, 20, 15, 20)))
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(-15, 21, 15, 100)), None)
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(1, -5, 100, 10)), Some(b(1, -5, 20, 10))) // RIGHT
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(20, -5, 100, 10)), Some(b(20, -5, 20, 10)))
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(21, -5, 100, 10)), None)
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(-15, -100, 15, -4)), Some(b(-15, -20, 15, -4))) // BOTTOM
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(-15, -100, 15, -20)), Some(b(-15, -20, 15, -20)))
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(-15, -100, 15, -21)), None)
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(-100, -15, -13, 5)), Some(b(-20, -15, -13, 5))) // LEFT
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(-100, -15, -20, 5)), Some(b(-20, -15, -20, 5)))
    Assert.assertEquals(b(-20, -20, 20, 20).intersect(b(-100, -15, -21, 5)), None)

    // cross
    Assert.assertEquals(b(-10, -100, 10, 100).intersect(b(-100, -5, 100, 5)), Some(b(-10, -5, 10, 5)))
    Assert.assertEquals(b(-10, -100, 10, 100).intersect(b(-100, -5, 100, 5)), Some(b(-10, -5, 10, 5)))
    Assert.assertEquals(b(-10, -5, 10, 5).intersect(b(-10, -5, 10, 5)), Some(b(-10, -5, 10, 5)))
    Assert.assertEquals(b(-10, -5, 10, 5).intersect(b(80, -5, 100, 5)), None)

    // in a line, overlapping ends
    Assert.assertEquals(b(-100, -10, 5, 10).intersect(b(-6, -10, 100, 10)), Some(b(-6, -10, 5, 10))) // HORIZ
    Assert.assertEquals(b(-100, -10, 5, 10).intersect(b(5, -10, 100, 10)), Some(b(5, -10, 5, 10)))
    Assert.assertEquals(b(-100, -10, 5, 10).intersect(b(11, -10, 100, 10)), None)
    Assert.assertEquals(b(-10, -100, 10, 15).intersect(b(-10, 1, 10, 100)), Some(b(-10, 1, 10, 15))) // VERT
    Assert.assertEquals(b(-10, -100, 10, 15).intersect(b(-10, 15, 10, 100)), Some(b(-10, 15, 10, 15)))
    Assert.assertEquals(b(-10, -100, 10, 15).intersect(b(-10, 16, 10, 100)), None)

    // some more offsets
    Assert.assertEquals(b(-10, -10, 10, 10).intersect(b(-50, 10, 5, 20)), Some(b(-10, 10, 5, 10)))
    Assert.assertEquals(b(-10, -10, 10, 10).intersect(b(-5, 10, 20, 20)), Some(b(-5, 10, 10, 10)))
    Assert.assertEquals(b(-10, -10, 10, 10).intersect(b(-5, 11, 20, 20)), None)
    Assert.assertEquals(b(-10, -10, 10, 10).intersect(b(10, -50, 30, -2)), Some(b(10, -10, 10, -2)))
    Assert.assertEquals(b(-10, -10, 10, 10).intersect(b(10, 1, 30, 60)), Some(b(10, 1, 10, 10)))
    Assert.assertEquals(b(-10, -10, 10, 10).intersect(b(11, 1, 30, 60)), None)

    // corners
    Assert.assertEquals(b(-10, -50, 0, 10).intersect(b(-10, 0, 60, 10)), Some(b(-10, 0, 0, 10)))
    Assert.assertEquals(b(-10, -50, 0, 10).intersect(b(-80, 0, 0, 10)),  Some(b(-10, 0, 0, 10)))
    Assert.assertEquals(b(-10, 0, 0, 90).intersect(b(-10, 0, 60, 10)),   Some(b(-10, 0, 0, 10))) // L
    Assert.assertEquals(b(-10, 0, 0, 90).intersect(b(-100, 0, 10, 10)),  Some(b(-10, 0, 0, 10))) // _|
    // --
    Assert.assertEquals(b(-10, -50, 0, 10).intersect(b(-6, 0, 60, 10)), Some(b(-6, 0, 0, 10)))

    // some more false cases
    Assert.assertEquals(b(-20, -20, -5, -5).intersect(b(1, 1, 2, 3)), None)
    Assert.assertEquals(b(0, 0, 5, 5).intersect(b(8, 0, 20, 3)), None)
  }


  @Test
  def testMerge: Unit = {
    val seq0 = Seq(b(0, 0, 20, 20))
    Assert.assertEquals(seq0, BoundingBox.merge(seq0, b(0, 0, 20, 20))) // merge with same
    Assert.assertEquals(seq0, BoundingBox.merge(seq0, b(0, 0, 10, 10))) // merge with smaller
    Assert.assertEquals(Seq(b(0, 0, 30, 30)), BoundingBox.merge(seq0, b(0, 0, 30, 30))) // merge with larger

    val seq1 = Seq(b(0, 0, 10, 10))
    Assert.assertEquals(Seq(b(0, 0, 20, 20)), BoundingBox.merge(seq1, b(0, 0, 20, 20))) // merge with larger
    Assert.assertEquals(Seq(b(0, 0, 10, 10), b(10, 0, 20, 10)), BoundingBox.merge(seq1, b(10, 0, 20, 10)))

    val seq2 = Seq(b(10, 0, 20, 10))
    Assert.assertEquals(Seq(b(0, 0, 20, 20)), BoundingBox.merge(seq2, b(0, 0, 20, 20)))

    val seq3 = Seq(b(0, 0, 10, 10), b(10, 0, 20, 0), b(5, 5, 6, 6)) // note: this seq isn't fully merged
    Assert.assertEquals(seq3, BoundingBox.merge(seq3, b(0, 0, 2, 1))) // gets eaten by the first one
    Assert.assertEquals(Seq(b(0, 0, 10, 10), b(10, 0, 20, 0)), BoundingBox.merge(seq3, b(0, 0, 10, 10))) // eats 5,6
    Assert.assertEquals(Seq(b(0, 0, 20, 20)), BoundingBox.merge(seq3, b(0, 0, 20, 20)))

    Assert.assertEquals(Seq(b(0, 0, 10, 10), b(10, 0, 20, 0)), seq3.foldLeft(Seq.empty[BoundingBox])(BoundingBox.merge))
  }

  @Test
  def testNonZeroOverlap: Unit = {
    def nz :(Seq[BoundingBox], Seq[BoundingBox]) => Boolean = BoundingBox.nonZeroOverlap _
    val a0 = b(0, 0, 10, 10)
    val a1 = b(5, 5, 15, 15)

    Assert.assertTrue(nz(Seq(b(0, 0, 1, 1)), Seq(b(0, 0, 1, 1))))
    Assert.assertFalse(nz(Seq(b(0, 0, 1, 1)), Seq(b(1, 0, 1, 2))))

    Assert.assertTrue(nz(Seq(a0), Seq(a1)))

    // intersections within the group dont count
    Assert.assertTrue(nz(Seq(a0, a1), Seq(a0, a1)))
    Assert.assertFalse(nz(Seq(a0, a1), Seq(a0, a1).map(_.translate(new PointXY(15, 0)))))


    Assert.assertFalse(nz(
      Seq(b(0, 0, 50, 20), b(0, 20, 16, 40)),
      Seq(b(16, 25, 22, 32), b(100, 0, 120, 10))
    ))
    Assert.assertTrue(nz(
      Seq(b(0, 0, 50, 20), b(0, 20, 16, 40)),
      Seq(b(16-1, 25, 22, 32), b(100, 0, 120, 10))
    ))

  }

  @Test
  def testAlternateConstructors: Unit = {
    Assert.assertEquals(b(0, 0, 0, 0), BoundingBox(p(0, 0)))
    Assert.assertNotEquals(b(0, 0, 0, 0), BoundingBox(p(0, 1)))

    Assert.assertEquals(b(1, 0, 1, 0), BoundingBox(p(1, 0)))
    Assert.assertEquals(b(1, 2, 1, 2), BoundingBox(p(1, 2)))
    Assert.assertEquals(b(-1, 2, -1, 2), BoundingBox(p(-1, 2)))

    Assert.assertEquals(b(0, 0, 0, 0), BoundingBox(Seq(p(0, 0))))
    Assert.assertEquals(b(0, 0, 1, 0), BoundingBox(Seq(p(0, 0), p(1, 0))))
    Assert.assertEquals(b(-1, 0, 1, 5), BoundingBox(Seq(p(0, 0), p(1, 0), p(-1, 5))))
    Assert.assertEquals(b(-1, 0, 1, 5), BoundingBox(Seq(p(0, 0), p(1, 0), p(-1, 5), p(1, 1))))
  }

  @Test
  def testTransform: Unit = {
    Assert.assertEquals(b(10, 5, 20, 15), b(0, 0, 10, 10).transform(Matrix2D.translate(10, 5)))
    Assert.assertEquals(b(2, 15, 40, 300), b(1, 5, 20, 100).transform(Matrix2D.scale(2, 3)))
  }

  @Test
  def testCenterXYZ(): Unit = {
    Assert.assertEquals(
      P(4, 4, 4),
      BoundingBox.centerXYZ(P(0, 0, 0), P(8, 8, 8)),
    )
    Assert.assertEquals(
      P(4, 4, 4),
      BoundingBox.centerXYZ(P(8, 8, 8), P(0, 0, 0)),
    )
    Assert.assertEquals(
      P(0, 40, -8),
      BoundingBox.centerXYZ(P(-8, 100, -4), P(8, -20, -12)),
    )
    Assert.assertEquals(
      P(0, 40, -8),
      BoundingBox.centerXYZ(P(8, -20, -12), P(-8, 100, -4))
    )
  }
}
