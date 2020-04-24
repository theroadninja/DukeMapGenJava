package trn.prefab.grid2d

import org.junit.{Assert, Test}
import trn.prefab.{Heading, MapWriter, RedwallConnector, SectorGroup}
import trn.FuncImplicits._
import trn.prefab.experiments.{Cell2D, GridUtil, TilePainter}

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

  // TODO - get rid of this
  def withBlockedSides(headings: Seq[Int]): SimpleGridPiece = SimpleGridPiece(
    if(headings.contains(Heading.E)){ Side.Blocked }else{ Side.Unknown },
    if(headings.contains(Heading.S)){ Side.Blocked }else{ Side.Unknown },
    if(headings.contains(Heading.W)){ Side.Blocked }else{ Side.Unknown },
    if(headings.contains(Heading.N)){ Side.Blocked }else{ Side.Unknown },
  )

  /**
    * @return true if the pieces are next to each other and both have definite connections pointed at each other.
    */
  def connected(p1: GridPiece, p1loc: Cell2D, p2: GridPiece, p2loc: Cell2D): Boolean = {
    if(p1loc.x != p2loc.x && p1loc.y != p2loc.y){
      false
    }else if(p1loc.x + 1 == p2loc.x){
      p1.isConn(Heading.E) && p2.isConn(Heading.W)
    }else if(p1loc.x == p2loc.x + 1){
      p1.isConn(Heading.W) && p2.isConn(Heading.E)
    }else if(p1loc.y + 1 == p2loc.y){
      p1.isConn(Heading.S) && p2.isConn(Heading.N)
    }else if(p1loc.y == p2loc.y + 1){
      p1.isConn(Heading.N) && p2.isConn(Heading.S)
    }else{
      false
    }
  }

  /**
    * calculate grid pieces that match piece1 and 2 exactly except they are also connected.
    */
  def connectedMatchPiecees(piece1: GridPiece, p1loc: Cell2D, piece2: GridPiece, p2loc: Cell2D): Option[(GridPiece, GridPiece)] = {
    if(p1loc.x != p2loc.x && p1loc.y != p2loc.y){
      None
    }else if(p1loc.x + 1 == p2loc.x){
      Some((SimpleGridPiece.copyOf(piece1).copy(e = Side.Conn), SimpleGridPiece.copyOf(piece2).copy(w = Side.Conn)))
    }else if(p1loc.x == p2loc.x + 1){
      Some((SimpleGridPiece.copyOf(piece1).copy(w = Side.Conn), SimpleGridPiece.copyOf(piece2).copy(e = Side.Conn)))
    }else if(p1loc.y + 1 == p2loc.y){
      Some((SimpleGridPiece.copyOf(piece1).copy(s = Side.Conn), SimpleGridPiece.copyOf(piece2).copy(n = Side.Conn)))
    }else if(p1loc.y == p2loc.y + 1){
      Some((SimpleGridPiece.copyOf(piece1).copy(n = Side.Conn), SimpleGridPiece.copyOf(piece2).copy(s = Side.Conn)))
    }else{
      None
    }
  }

}
/** Represents a square-fitting sector group that fits in a 2d layout grid */
trait GridPiece {
  def side(heading: Int): Int

  final def isConn(heading: Int): Boolean = side(heading) == Side.Conn

  def rotatedCW: GridPiece

  def getSg: Option[SectorGroup] = None

  /** matches perfectly; no rotation needed */
  final def matches(other: GridPiece): Boolean = {
    !Heading.all.asScala.exists(h => !GridPiece.matches(side(h), other.side(h)))
  }

  /** could match after a rotation */
  final def couldMatch(other: GridPiece): Boolean = {
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

  /** @return the number of sides marked with Conn OR Unknown (only relevant for match tiles */
  final def maxConnectors: Int = Heading.all.asScala.map(side(_)).count(s => s != Side.Blocked)

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

object SimpleGridPiece {
  def copyOf(p: GridPiece): SimpleGridPiece = {
    SimpleGridPiece(p.side(Heading.E), p.side(Heading.S), p.side(Heading.W), p.side(Heading.N))
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
