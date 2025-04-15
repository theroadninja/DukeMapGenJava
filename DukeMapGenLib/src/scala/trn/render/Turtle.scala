package trn.render

import trn.{PointXY, LineSegmentXY, FVectorXY, Wall}

import scala.collection.mutable


case class TurtleOptions(gridSnap: Option[Int] = None){
  require(gridSnap.isEmpty || gridSnap.get % 8 == 0)
}

/**
  * A wall-printing turtle (in the future, might have a sector-printing turtle).
  *
  * TODO - ability to snap to grid
  *
  * TODO - consider renaming this to PointTurtle (as opposted to WallTurtle ...)
  */
class Turtle(
  startPoints: Seq[PointXY],
  var heading: FVectorXY,
  val options: TurtleOptions,
) {
  val points = mutable.ArrayBuffer[PointXY]()
  points.append(startPoints: _*)

  def withGridSnap(size: Int): Turtle = new Turtle(
    points, heading, options.copy(gridSnap = Some(size))
  )

  def currentPos: PointXY = points.last

  // remember:  most of the time you want to turn right (clockwise) because that is the handedness
  //     of sector wall-loops.
  def turnRightD(degrees: Double): Unit = {
    heading = heading.rotatedDegreesCW(degrees).normalized()
  }

  /** move forward on the current heading; then add a point */
  def forwardStamp(distance: Double): PointXY = {
    val delta = heading.multipliedBy(distance).toPointXY
    val newPoint = currentPos.add(delta)
    val newPoint2 = options.gridSnap.map(gridSize => newPoint.snappedToGrid(gridSize)).getOrElse(newPoint)
    points.append(newPoint2)
    newPoint2
  }

  def wallLoop(wp: WallPrefab = WallPrefab.Empty): Seq[Wall] = {
    if(points.head.equals(points.last)){
      // can't do this in a wall loop -- this is an error
      throw new Exception(s"Turtle start and end points are equal: ${points.head} == ${points.last}")
    }
    points.sliding(2).foreach { pair =>
      if(pair.head.equals(pair.last)){
        throw new Exception(s"Overlapping points: ${points.head} == ${points.last}")
      }
    }

    points.map(wp.create)
  }

}

object Turtle {

  // this is the smallest grid size in mapster32.  I think DOS Build has it at 32 (see BuildConstants)
  val SmallestGridSize = 8


  def apply(startPoints: Seq[PointXY]): Turtle = {
    require(startPoints.size > 1)
    val vector: FVectorXY = startPoints(0).vectorTo(startPoints(1)).toFVectorXY.normalized()
    new Turtle(startPoints, vector, TurtleOptions())
  }

  def apply(line: LineSegmentXY): Turtle = apply(Seq(line.getP1, line.getP2))

  def apply(line: LineSegmentXY, options: TurtleOptions): Turtle = {
    val startPoints = Seq(line.getP1, line.getP2)
    require(startPoints.size > 1)
    val vector: FVectorXY = startPoints(0).vectorTo(startPoints(1)).toFVectorXY.normalized()
    new Turtle(startPoints, vector, options)
  }
}
