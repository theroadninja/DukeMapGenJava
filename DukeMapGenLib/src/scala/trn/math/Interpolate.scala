package trn.math

import trn.PointXY

/**
  * @param p a point on the bezier curve
  * @param tangent the tangent(x,y), a vector at that point
  */
case class PointAndTangent(p: PointXY, tangent: (Double, Double))

/**
  * Integer interpolation.  Also integer Bezier curves.
  */
object Interpolate {


  /** this interpolate preserves doubles to prevent errors from accumulating */
  private[math] def interp(x0: Double, y0: Double, x1: Double, y1: Double, t: Double): (Double, Double) = {
    // val x0 = p0.x.toDouble
    //val x1 = p1.x.toDouble
    // val y0 = p0.y.toDouble
    //val y1 = p1.y.toDouble
    (t * (x1 - x0) + x0, t * (y1 - y0) + y0)
  }

  /** integer interpolate */
  def interpInt(p0: PointXY, p1: PointXY, t: Double): PointXY = {
    val (x, y) = interp(p0.x.toDouble, p0.y.toDouble, p1.x.toDouble, p1.y.toDouble, t)
    new PointXY(
      Math.round(x).toInt,
      Math.round(y).toInt
    )
  }

  /**
    *
    * @param p0
    * @param p1
    * @param count total number of points, including the ends
    * @return
    */
  def linear(p0: PointXY, p1: PointXY, count: Int): Seq[PointXY] = {
    if(count < 2) throw new IllegalArgumentException("count must be >= 2")
    val stepSize: Double = 1.0 / (count.toDouble - 1.0) // e.g. with 5 points, there are 4 steps between them
    (0 until count).map(i => i * stepSize).map(t => interpInt(p0, p1, t))
  }

  def linear(start: Int, end: Int, count: Int): Seq[Int] = {
    linear(new PointXY(start, 0), new PointXY(end, 0), count).map(_.x)
  }


  private def cubicAtT(p0: PointXY, p1: PointXY, p2: PointXY, p3: PointXY, t: Double): (Double, Double) = {
    // linear
    val (l0x, l0y) = interp(p0.x, p0.y, p1.x, p1.y, t)
    val (l1x, l1y) = interp(p1.x, p1.y, p2.x, p2.y, t)
    val (l2x, l2y) = interp(p2.x, p2.y, p3.x, p3.y, t)
    // quadratic
    val (q0x, q0y) = interp(l0x, l0y, l1x, l1y, t)
    val (q1x, q1y) = interp(l1x, l1y, l2x, l2y, t)
    // cubic
    val (cx, cy) = interp(q0x, q0y, q1x, q1y, t)
    (cx, cy)
    // new PointXY(Math.round(cx).toInt, Math.round(cy).toInt)
  }
  /**
    * Interpolates a cubic bezier curve using segments p0-p1 and p2-p3.
    */
  def cubic(p0: PointXY, p1: PointXY, p2: PointXY, p3: PointXY, count: Int): Seq[PointXY] = {
    if(count < 2) throw new IllegalArgumentException("count must be >= 2")
    val stepSize: Double = 1.0 / (count.toDouble - 1.0) // e.g. with 5 points, there are 4 steps between them
    (0 until count).map(i => i * stepSize).map{ t =>
      val (cx, cy) = cubicAtT(p0, p1, p2, p3, t)
      new PointXY(Math.round(cx).toInt, Math.round(cy).toInt)
    }
  }

  // TODO - migrate to using FPointXY, which i created for this stuff
  def normalize(x: Double, y: Double): (Double, Double) = {
    val length: Double = Math.sqrt(x*x + y*y)
    (x/length, y/length)
  }

  def toPoint(x: Double, y: Double): PointXY = new PointXY(Math.round(x).toInt, Math.round(y).toInt)

  def cubicWithTangents(p0: PointXY, p1: PointXY, p2: PointXY, p3: PointXY, count: Int): Seq[PointAndTangent] = {
    val Epsilon = 0.001
    if(count < 2) throw new IllegalArgumentException("count must be >= 2")
    val stepSize: Double = 1.0 / (count.toDouble - 1.0) // e.g. with 5 points, there are 4 steps between them
    (0 until count).map(i => i * stepSize).map{ t =>
      val (cx, cy) = cubicAtT(p0, p1, p2, p3, t)
      val (tanx, tany) = cubicAtT(p0, p1, p2, p3, t + Epsilon)

      val tangent = normalize(tanx - cx, tany - cy)
      println(tangent)
      PointAndTangent(toPoint(cx, cy), tangent)
    }
  }

}
