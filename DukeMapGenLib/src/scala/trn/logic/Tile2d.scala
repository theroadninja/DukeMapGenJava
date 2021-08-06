package trn.logic

import trn.math.{RotatesCW, SnapAngle}
import trn.prefab.Heading

import scala.collection.JavaConverters._

/**
  * Tracks the orientation and connections of a part of a map, but not its position.  Its like a free floating puzzle
  * piece.
  *
  * Represents a sector group as if it is a square (4 sided) puzzle piece where each side can have either a connection
  * or no connection.  Meant to be used in simple logic systems where sector groups are arranged in a grid and need
  * to be rotated so that their connections face each other.
  *
  * This doesnt try to handle Z.  There could be a Tile3d, although you can't meaningfully rotate around the X axis.
  *
  * This is meant to partially replace prefab.grid2d.GridPiece
  *
  * @param e status of east side:  1 for conn, 0 for no conn, -1 for unknown/optional
  * @param s status of east side:  1 for conn, 0 for no conn, -1 for unknown/optional
  * @param w status of east side:  1 for conn, 0 for no conn, -1 for unknown/optional
  * @param n status of east side:  1 for conn, 0 for no conn, -1 for unknown/optional
  */
case class Tile2d(e: Int, s: Int, w: Int, n: Int) extends RotatesCW[Tile2d] {

  /**
    * @param heading
    * @return the conn/no conn/unknown value for the side, as specified by the heading.  For example, side(East) would
    *         return the value for the east (right) side of the tile.
    */
  def side(heading: Int): Int = heading match {
    case Heading.E => e
    case Heading.W => w
    case Heading.N => n
    case Heading.S => s
    case _ => ???
  }

  def rotatedCW: Tile2d = Tile2d(e=n, s=e, w=s, n=w)

  // Unnecessary?
  // def rotatedToMatch(other: Tile2d): Option[Tile2d] = { // TODO return number of rotations to make it match
  //   // TODO invent a stupid angle case class that measures an angle delta in number of rotations?
  //   val r90 = this.rotatedCW
  //   val r180 = r90.rotatedCW
  //   val r270 = r180.rotatedCW
  //   Seq(this, r90, r180, r270).find(this2 => this2.matches(other))
  // }
  def couldMatch(other: Tile2d): Boolean = rotationTo(other).isDefined

  /**
    *
    * @param heading a Heading value that indicates which side (e.g. Heading.E for e)
    * @param value the value to set (Conn, Blocked, Wildcard)
    * @return
    */
  def withSide(heading: Int, value: Int): Tile2d = {
    // require(Tile2d.ValidSides.contains(value))
    heading match {
      case Heading.E => Tile2d(value, s, w, n)
      case Heading.S => Tile2d(e, value, w, n)
      case Heading.W => Tile2d(e, s, value, n)
      case Heading.N => Tile2d(e, s, w, value)
      case _ => ???
    }
  }

  /** @returns the min number of times to rotate this tile clockwise until it matches the other tile */
  def rotationTo(other: Tile2d): Option[SnapAngle] = SnapAngle.rotateUntil(this){ t => t.matches(other) }

  def matches(other: Tile2d): Boolean = {
    ! Heading.all.asScala.exists(h => ! Tile2d.matches(side(h), other.side(h)))
  }

  // GridPiece had this...is it necessary?
  // final def gridPieceType: Int = sidesWithConnectors match {
  //   case 0 => Tile2d.Orphan
  //   case 1 => Tile2d.Single
  //   case 2 => {
  //     if((side(Heading.E) == 1 && side(Heading.W) == 1) || (side(Heading.N) == 1 && side(Heading.S) == 1)){
  //       Tile2d.Straight
  //     }else{
  //       Tile2d.Corner
  //     }
  //   }
  //   case 3 => Tile2d.TJunction
  //   case 4 => Tile2d.Plus
  //   case _ => throw new RuntimeException
  // }
}

object Tile2d {
  /** A connection on that side is unknown or optional */
  val Wildcard = -1

  /** there is no connection or there can be no connection on that side */
  val Blocked = 0

  /** there is or must be a connection on that side */
  val Conn = 1

  val ValidSides = Seq(Wildcard, Blocked, Conn)

  // GridPiece uses these to classify tiles (not sure if this is helpful though)
  // val Orphan = 0
  // val Single = 1
  // val Corner = 2
  // val Straight = 3
  // val TJunction = 4
  // val Plus = 5

  def matches(side1: Int, side2: Int): Boolean = {
    if(side1 == Wildcard || side2 == Wildcard){
      true
    }else{
      side1 == side2
    }
  }

  def apply(e: Boolean, s: Boolean, w: Boolean, n: Boolean): Tile2d = Tile2d(
    if(e){ Conn }else{ Blocked },
    if(s){ Conn }else{ Blocked },
    if(w){ Conn }else{ Blocked },
    if(n){ Conn }else{ Blocked }
  )

  def apply(): Tile2d = Tile2d(Wildcard, Wildcard, Wildcard, Wildcard)

  def apply(default: Int): Tile2d = Tile2d(default, default, default, default)



}
