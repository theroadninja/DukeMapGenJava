package trn.math

import trn.prefab.Heading

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
case class SnapAngle(cwCount: Int) extends RotatesCW[SnapAngle] {
  def +(other: SnapAngle): SnapAngle = SnapAngle(cwCount + other.cwCount)
  def -(other: SnapAngle): SnapAngle = SnapAngle(cwCount - other.cwCount)

  override def rotatedCW: SnapAngle = SnapAngle(cwCount + 1)

  def *[T <: RotatesCW[T]](other: T): T = if(cwCount == 0) {
    other
  }else{
    var result = other
    (0 until cwCount).foreach { _ =>
      result = result.rotatedCW
    }
    result
  }

  def rotate[T <: RotatesCW[T]](thingToRotate: T): T = this * thingToRotate

  def rotateHeading(heading: Int): Int = {
    var result = heading
    (0 until cwCount).foreach { _ =>
      result = Heading.rotateCW(result)
    }
    result
  }
}

object SnapAngle {
  /**
    * Utility function to rotate something until a predicate is true, or no rotation satisfy the condition.
    * @param r the thing to rotate
    * @param predicate a  function that returns true if the rotates satisfies the condition we are looking for
    * @return The angle of rotation requires to make predicate() true, or None if no such angle exists.
    *
    * Example:
    *
    * val rotateMe = Tile(...) // tile extends RotatesCW
    * val angle: Option[SnapAngle] = SnapAngle.rotateUntil(rotateMe){ tile =>  tile.isWhatever() }
    *
    * TODO this should be called getRotationsUntil
    */
  def rotateUntil[T <: RotatesCW[T]](r: T)(predicate: T => Boolean): Option[SnapAngle] = {
    lazy val r90 = r.rotatedCW
    lazy val r180 = r90.rotatedCW
    lazy val r270 = r180.rotatedCW
    if(predicate(r)){
      Some(SnapAngle(0))
    }else if(predicate(r90)){
      Some(SnapAngle(1))
    }else if(predicate(r180)){
      Some(SnapAngle(2))
    }else if(predicate(r270)){
      Some(SnapAngle(3))
    }else{
      None
    }
  }

  def rotateUntil2[T <: RotatesCW[T]](r: T)(predicate: T => Boolean): Option[(SnapAngle, T)] = {
    lazy val r90 = r.rotatedCW
    lazy val r180 = r90.rotatedCW
    lazy val r270 = r180.rotatedCW
    if (predicate(r)) {
      Some(SnapAngle(0), r)
    } else if (predicate(r90)) {
      Some(SnapAngle(1), r90)
    } else if (predicate(r180)) {
      Some(SnapAngle(2), r180)
    } else if (predicate(r270)) {
      Some(SnapAngle(3), r270)
    } else {
      None
    }

  }

  def apply(cwCount: Int): SnapAngle = new SnapAngle(modulo(cwCount, 4))

  /**
    * @param headingA
    * @param headingB
    * @return snap angle to rotate from heading a to heading b
    */
  def angleFromAtoB(headingA: Int, headingB: Int): SnapAngle = {
    (headingA, headingB) match {
      case (a: Int, b: Int) if a == b => SnapAngle(0)
      case (Heading.E, b: Int) => SnapAngle(b)
      case (Heading.S, Heading.W) => SnapAngle(1)
      case (Heading.S, Heading.N) => SnapAngle(2)
      case (Heading.S, Heading.E) => SnapAngle(3)
      case (Heading.W, Heading.N) => SnapAngle(1)
      case (Heading.W, Heading.E) => SnapAngle(2)
      case (Heading.W, Heading.S) => SnapAngle(3)
      case (Heading.N, Heading.E) => SnapAngle(1)
      case (Heading.N, Heading.S) => SnapAngle(2)
      case (Heading.N, Heading.W) => SnapAngle(3)
      case _ => throw new RuntimeException(s"invalid heading(s): ${headingA}, ${headingB}")
    }
  }

  /** Scala % seems to be a remainder operator, but I want:  -1 % 4 == 3 */
  private[math] def modulo(i: Int, j: Int): Int = if(i < 0){
    // floor(-0.25) == -1  so floor(i/j) gets you the "lowest" multiple of j that is less than i
    i - Math.floor(i.toFloat / j.toFloat).toInt * j
  }else{
    i % j
  }

}
