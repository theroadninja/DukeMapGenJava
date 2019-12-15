package trn

object MathUtil {

  /**
    * compares two 1-dimensional line segments, A and B, for overlap
    * if their edges touch, that does not count
    *
    * if one of the lines has zero length, that also does not count
    *
    * @param a0 end point of line segment A
    * @param a1 other end point of line segment A
    * @param b0 end point of line segment B
    * @param b1 other end point of line segment B
    * @return
    */
  def overlaps(a0: Int, a1: Int, b0: Int, b1: Int): Boolean = {
    if(Math.abs(a1 - a0) == 0 || Math.abs(b1 - b0) == 0) {
      false // of of them has zero length
    }else {
      val a = Seq(a0, a1).sorted
      val b = Seq(b0, b1).sorted
      if(b(0) >= a(1) || a(0) >= b(1)) {
        false
      }else {
        true
      }
    }
    // bad method 1
    // def ie(x: Seq[Int], y: Int): Boolean = x(0) < y && y < x(1) // inside, exclusive
    // ie(a, b0) || ie(a, b1) || ie(b, a0) || ie(b, a1)

    // bad method 2
    //val c1 = Math.max(a(0), b(0))
    //val c2 = Math.min(a(1), b(1))
    //return Math.abs(c2 - c1) > 0
  }

}
