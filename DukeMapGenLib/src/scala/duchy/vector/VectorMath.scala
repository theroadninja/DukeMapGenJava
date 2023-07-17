package duchy.vector

import trn.PointXY

/**
  * Meant to replace MapUtil.java and some methods in PointXY.java
  */
object VectorMath {


  /**
    * Given:
    * a point `a` and a vector `b` from `a` (which describes a line segment from a to a+b or ray from a)
    * a point `c` and a vector `d` from `c` (which describes a line segment from c to c+d or ray from c)
    * Calculate:
    * the factors `t` and `u` where:   a + tb == c + ud
    *
    * @param pointA
    * @param vectorB
    * @param pointC
    * @param vectorD
    * @return
    */
  def intersectionFactors(
    pointA: PointXY,
    vectorB: PointXY,
    pointC: PointXY,
    vectorD: PointXY,
  ): Option[(Double, Double)] = {
    val a = pointA
    val b = vectorB
    val c = pointC
    val d = vectorD

    val bd: Int = b.crossProduct2d(d)
    if (0 == bd) {
      return None // they are parallel
    }
    val ca: PointXY = c.subtractedBy(a)

    // t is the factor multiplied against a+b// t is the factor multiplied against a+b
    val t: Double = ca.crossProduct2d(d) / bd.toDouble

    // u is the factor multiplied against c+d// u is the factor multiplied against c+d
    val u: Double = ca.crossProduct2d(b) / bd.toDouble // -bxd = dxb

    Some((t, u))
  }

  def intersection(line1: Line2D, line2: Line2D): Option[PointXY] = {

    // factor is the t or u from the insersection calculation
    def containsPoint(line: Line2D, factor: Double): Boolean = line.lineType match {
      case Line2D.LINE => true
      case Line2D.RAY => 0 <= factor
      case Line2D.SEGMENT => 0 <= factor && factor <= 1.0
    }

    intersectionFactors(line1.a, line1.b, line2.a, line2.b).collect {
      case (t, u) if(containsPoint(line1, t) && containsPoint(line2, u)) => {
        line1.a.toF.add(line1.b.toF.multipliedBy(t)).toPointXY
      }
    }
  }



}
