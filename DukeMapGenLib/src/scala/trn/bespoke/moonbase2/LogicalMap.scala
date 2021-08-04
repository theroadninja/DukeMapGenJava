package trn.bespoke.moonbase2

import scala.collection.JavaConverters._
import scala.collection.mutable
import trn.logic.{Point3d, Tile2d}
import trn.prefab.Heading

object Edge {
  def sorted(a: Point3d, b: Point3d): Edge = {
    val p = Seq(a, b).sorted
    Edge(p(0), p(1))
  }
}
case class Edge(p1: Point3d, p2: Point3d) {
  def isHorizontal: Boolean = p1.y == p2.y && p1.x != p2.x
  def isVertical: Boolean = p1.x == p2.x && p1.y != p2.y
}



object LogicalMap {
  def apply[V, E](): LogicalMap[V, E] = new LogicalMap
}

class LogicalMap[V, E] {
  val nodes = mutable.Map[Point3d, V]()
  val edges = mutable.Map[Edge, E]()

  def get(p: Point3d): Option[V] = nodes.get(p)

  def contains(p: Point3d): Boolean = nodes.contains(p)

  def center: Point3d = {
    def midpoint(values: Iterable[Int]) = {
      val minval = values.min
      minval + (values.max - minval) / 2
    }
    val xs = nodes.keys.map(_.x)
    val ys = nodes.keys.map(_.y)
    val zs = nodes.keys.map(_.z)
    Point3d(midpoint(xs), midpoint(ys), midpoint(zs))
  }

  def emptyAdj(p: Point3d): Seq[Point3d] = {
    p.adj.filterNot(contains)
  }

  def put(p: Point3d, value: V, overwrite: Boolean = false): Unit = {
    require(overwrite || !nodes.contains(p))
    nodes.put(p, value)
  }

  def putEdge(a: Point3d, b: Point3d, edgeValue: E): Unit = {
    require(nodes.isDefinedAt(a) && nodes.isDefinedAt(b))
//    val p = Seq(a, b).sorted
//    edges.put(Edge(p(0), p(1)), edgeValue)
    edges.put(Edge.sorted(a, b), edgeValue)
  }

  def containsEdge(a: Point3d, b: Point3d): Boolean = {
//    val p = Seq(a, b).sorted
//    edges.get(Edge(p(0), p(1))).isDefined
    edges.get(Edge.sorted(a, b)).isDefined
  }

  /** returns all edges that include this point */
  def adjacentEdges(p: Point3d): Map[Int, Edge] = { // TODO doesnt handle Z ...
    def edge(heading: Int): Edge = heading match {
      case Heading.N => Edge.sorted(p, p.n)
      case Heading.S => Edge.sorted(p, p.s)
      case Heading.E => Edge.sorted(p, p.e)
      case Heading.W => Edge.sorted(p, p.w)
      case _ => throw new Exception(s"invalid heading: ${heading}")
    }
    Heading.all.asScala.toSeq.map(_.intValue).flatMap { heading =>
      val e = edge(heading)
      if(edges.contains(e)){
        Some(heading -> e)
      }else{
        None
      }
    }.toMap
  }

  /**
    * Like adjacentEdges but returns them as a tile.
    * @param p
    * @param blankVal - value to use when there is no edge on that side
    * @return a Tile2d describing the point `p` in terms of its edges.
    */
  def getTile(p: Point3d, blankVal: Int = Tile2d.Wildcard): Tile2d = {
    adjacentEdges(p).keys.foldLeft(Tile2d(blankVal)) { (tile, heading) => tile.withSide(heading, Tile2d.Conn)}
  }


  override def toString: String = {
    if(nodes.isEmpty){
      return ""
    }

    def getxy(x: Int, y: Int): Option[V] ={
      val k = nodes.keys.filter(p => p.x == x && p.y == y).toSeq.sortBy(_.z).headOption
      k.flatMap(p => nodes.get(p))
    }

    // TODO print edges
    // remember, positive Y goes down
    val width = nodes.values.map(_.toString.length).max
    val xs = nodes.keys.map(_.x)
    val ys = nodes.keys.map(_.y)
    val lines = (ys.min to ys.max).map{ y =>
      val nodeline = (xs.min to xs.max).map{ x =>
        val node: String = getxy(x, y).map(_.toString).getOrElse("")
        val s = node.padTo(width, ' ')

        val rightEdge = if(containsEdge(Point3d(x,y,0), Point3d(x+1,y,0))){ "-" }else{ "" }
        s + rightEdge.padTo(width, ' ')
      }.mkString("")
      val verticalEdges = (xs.min to xs.max).map { x =>
        val e = if (containsEdge(Point3d(x,y, 0), Point3d(x,y+1, 0))){ "|" }else{""}
        e.padTo(width, ' ') + "".padTo(width, ' ')
      }.mkString("")

      s"${nodeline}\n${verticalEdges}"
    }
    lines.mkString("\n")
  }
}