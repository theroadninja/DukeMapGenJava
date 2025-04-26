package trn.logic

/**
  * This is a point in 3 dimensional "logical" space.  It not meant to track a location in actual map space,
  * (see PointXY or PointXYZ for that)
  * but instead to be used in logical "grid" systems to decide where elements go relative to each other.
  *
  * NOTE:  positive Y is "south" as in Build, but positive Z goes "up" (in contrast to Build, where positive Z goes
  * down).  I'm doing this b/c build coordinate system the z axis is bit shifted anyway.
  */
case class Point3d(x: Int, y: Int, z: Int) extends Ordered[Point3d] {
  def compare(other: Point3d): Int ={
    Seq(x.compare(other.x), y.compare(other.y), z.compare(other.z)).find(_ != 0).getOrElse(0)
  }

  def n: Point3d = Point3d(x, y-1, z)
  def s: Point3d= Point3d(x, y+1, z)
  def e: Point3d = Point3d(x+1, y, z)
  def w: Point3d = Point3d(x-1, y, z)
  def u: Point3d = Point3d(x, y, z+1)
  def d: Point3d = Point3d(x, y, z-1)

  // TODO what about diagonals?
  def adj: Seq[Point3d] = Seq(n, s, e, w, u, d)

  def +(heading: Int): Point3d = heading match {
    case Point3d.EAST => e
    case Point3d.SOUTH => s
    case Point3d.WEST => w
    case Point3d.NORTH => n
  }

  def manhattanDistance(other: Point3d): Int = {
    Math.abs(x - other.x) + Math.abs(y - other.y) + Math.abs(z - other.z)
  }

}

object Point3d {
  val Zero = Point3d(0, 0, 0)


  /** TODO DRY with trn.prefab.Heading (that heading probably needs to be moved to trn.logic ... ) */
  val EAST = 0
  val SOUTH = 1
  val WEST = 2
  val NORTH = 3

}
