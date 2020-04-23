package trn.prefab.grid2d

import trn.prefab.{Heading, MapWriter, RedwallConnector, SectorGroup}

import trn.FuncImplicits._
import scala.collection.JavaConverters._

object Side {
  /** dont know if it has a connection because nothing has been placed there */
  val Unknown = -1

  /** there is definitely no connection on that side */
  val Blocked = 0

  /** there is a connection on that side */
  val Conn = 1

}

object GridPiece {

  val Orphan = 0
  val Single = 1
  val Corner = 2
  val Straight = 3
  val TJunction = 4
  val Plus = 5

  def matches(side1: Int, side2: Int): Boolean = {
    if(side1 == Side.Unknown || side2 == Side.Unknown){
      true
    }else{
      side1 == side2
    }
  }

  def withBlockedSides(headings: Seq[Int]): SimpleGridPiece = SimpleGridPiece(
    if(headings.contains(Heading.E)){ Side.Blocked }else{ Side.Unknown },
    if(headings.contains(Heading.S)){ Side.Blocked }else{ Side.Unknown },
    if(headings.contains(Heading.W)){ Side.Blocked }else{ Side.Unknown },
    if(headings.contains(Heading.N)){ Side.Blocked }else{ Side.Unknown },
  )

}
/** Represents a square-fitting sector group that fits in a 2d layout grid */
trait GridPiece {
  def side(heading: Int): Int

  def rotatedCW: GridPiece

  def getSg: Option[SectorGroup] = None

  /** matches perfectly; no rotation needed */
  final def matches(other: GridPiece): Boolean = {
    !Heading.all.asScala.exists(h => !GridPiece.matches(side(h), other.side(h)))
  }

  /** could match after a rotation */
  final def couldMatch(other: GridPiece): Boolean = {
    // val r90 = other.rotatedCW
    // val r180 = r90.rotatedCW
    // val r270 = r180.rotatedCW
    // Seq(other, r90, r180, r270).exists(other2 => matches(other2))
    rotateToMatch(other).isDefined
  }

  final def rotateToMatch(other: GridPiece): Option[GridPiece] = {
    val r90 = this.rotatedCW
    val r180 = r90.rotatedCW
    val r270 = r180.rotatedCW
    Seq(this, r90, r180, r270).find(this2 => this2.matches(other))
  }

  /** @return how many sides have connectors */
  final def sidesWithConnectors: Int = Heading.all.asScala.map(side(_)).count(_ == 1)

  final def gridPieceType: Int = sidesWithConnectors match {
    case 0 => GridPiece.Orphan
    case 1 => GridPiece.Single
    case 2 => {
      if((side(Heading.E) == 1 && side(Heading.W) == 1) || (side(Heading.N) == 1 && side(Heading.S) == 1)){
        GridPiece.Straight
      }else{
        GridPiece.Corner
      }
    }
    case 3 => GridPiece.TJunction
    case 4 => GridPiece.Plus
    case _ => throw new RuntimeException
  }

}

/** abstract version of a grid piece that we can use for matching */
case class SimpleGridPiece(e: Int, s: Int, w: Int, n: Int) extends GridPiece {
  override def side(heading: Int): Int = heading match {
    case Heading.E => e
    case Heading.W => w
    case Heading.N => n
    case Heading.S => s
    case _ => ???
  }

  override def rotatedCW: GridPiece = new SimpleGridPiece(n, e, s, w)
}

class SectorGroupPiece(val sg: SectorGroup) extends GridPiece {
  private val sides: Map[Int, Option[RedwallConnector]] = Heading.all.asScala.map{ h =>
    (h.toInt, MapWriter.farthestConn(sg.allRedwallConnectors, h))
  }.toMap

  override def getSg: Option[SectorGroup] = Some(sg)

  override def side(heading: Int): Int = sides(heading).map(_ => Side.Conn).getOrElse(Side.Blocked)

  override def rotatedCW: SectorGroupPiece = new SectorGroupPiece(sg.rotateCW)

  /**
    * Calculates the "width" between the farthest east connector and the farthest west connector, though only if the
    * east connector is actually farther east than the west one.
    */
  lazy val width: Option[Int] = {
    val e = sides(Heading.E).map(_.getAnchorPoint.x)
    val w = sides(Heading.W).map(_.getAnchorPoint.x)
    e.flatMap(x2 => w.map(x1 => x2 - x1)).filter(_ > 0)
  }

  lazy val height: Option[Int] = {
    val n = sides(Heading.N).map(_.getAnchorPoint.y)
    val s = sides(Heading.S).map(_.getAnchorPoint.y)
    s.flatMap(y2 => n.map(y1 => y2 - y1)).filter(_ > 0)
  }

  /**
    * @returns the largest length between and eastern and western, or a northern and southern, connector
    */
  def cellSize: Option[Int] = {
    val lengths: Seq[Int] = Seq(width, height).collect { case Some(i) => i }
    lengths.maxOption // cant return bounding box because we might not have both axis' of connectors...
  }


}
