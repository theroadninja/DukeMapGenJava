package trn.math

/**
  * Describes an angle as a count of 90 degree, "clockwise" (Build editor POV) rotations.
  *
  * Long Versoin:  Describes an angle between two angles A and B in terms of the number of 90 degree clockwise rotations
  * needed to get from A to B, where "clockwise" means clockwise from the point of view of someone using the Build
  * Editor, or Mapster32 (which might be the opposite of clockwise according to a more conventional math definiton).
  *
  * This is intented to be used to measure rotations need to line things up, and not really for specifying
  * absolute orientations (so defining where angle 0 is, is outside the scope of this class).
  *
  * Why am I coding it this way?
  * 1. in Build, +y points down, which challenges my preconception of "clockwise" and makes my head hurt when I tried to
  * reason about trig
  * 2. in Build, everything is integers, which means rotating in 90 degree rotations is flawless while any other angle
  * is lossy.
  *
  * See also trn.AngleUtil
  */
case class SnapAngle(cwCount: Int) {
  def +(other: SnapAngle): SnapAngle = SnapAngle(cwCount + other.cwCount)
  def -(other: SnapAngle): SnapAngle = SnapAngle(cwCount - other.cwCount)
}

object SnapAngle {
  def apply(cwCount: Int): SnapAngle = new SnapAngle(modulo(cwCount, 4))

  /**
    * Scala % seems to be a remainder operator, but I want:  -1 % 4 == 3
    */
  // private[math] def modulo(i: Int, j: Int): Int = i - (i / j) * j
  private[math] def modulo(i: Int, j: Int): Int = if(i < 0){
    // floor(-0.25) == -1  so floor(i/j) gets you the "lowest" multiple of j that is less than i
    i - Math.floor(i.toFloat / j.toFloat).toInt * j
  }else{
    i % j
  }

}
