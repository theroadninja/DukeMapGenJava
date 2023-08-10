package trn.prefab.experiments.hyperloop

import trn.PointXY
import trn.math.SnapAngle
import trn.prefab.experiments.hyperloop.HyperLoopParser.{measureDistToAnchor, measureWidth}
import trn.prefab.SectorGroup

object RingLayout {

  //
  // Types of Angles
  // (that can be transformed between using 90 degree rotations)
  //
  /** East, South, West, North */
  val AXIS = 0

  /** Southeast, Southwest, Northwest, Northeast */
  val DIAG = 1

  //
  // Headings (there can be infinite angles going up to infinity, but only 8 headings)
  // 1-based, in case I want to put them in hitags/lotags
  //
  val East = 1
  val SouthEast = 2
  val South = 3
  val SouthWest = 4
  val West = 5
  val NorthWest = 6
  val North = 7
  val NorthEast = 8

  val Headings = Seq(East, SouthEast, South, SouthWest, West, NorthWest, North, NorthEast)

  /**
    * @param angleIndex an index of polar angles, starting at 0 for east and going clockwise
    * @return the enum value for the compass angle, e.g. South, Northwest, etc
    */
  def indexToHeading(angleIndex: Int): Int = {
    require(angleIndex >= 0)
    Headings(angleIndex % 8)
  }

  def indexToAngleType(angleIndex: Int): Int = angleType(indexToHeading(angleIndex))

  val AxisAligned = Set(East, South, West, North)

  /**
    * Create a ring layout using paramaters measured from the given sector groups
    *
    * @param coreSg  a single-sector group with the shape that matches the area inside the inner ring
    * @param innerSg an east-facing "inner ring" sector group, with two Y-axis-aligned walls that can be used to measure
    *                the width of the inner ring (inner rings are allowed to have things that stick into the middle, so
    *                need to pass one that doesnt do that.
    * @param midSg   an east-facing "middle ring" sector group
    * @return
    */
  def fromSectorGroups(coreSg: SectorGroup, innerSg: SectorGroup, midSg: SectorGroup): RingLayout = {
    val centerToInnerEdgeOfMid = measureWidth(coreSg) / 2 + measureWidth(innerSg)
    new RingLayout(centerToInnerEdgeOfMid, measureDistToAnchor(midSg))
  }

  def fromHyperLoopPalette(pal: HyperLoopPalette): RingLayout = fromSectorGroups(pal.coreSizeGroup, pal.innerSizeGroup, pal.midSizeGroup)

  def clockwise(heading: Int): Int = {
    val h2 = heading + 1
    if (h2 > 8) {
      1
    } else {
      h2
    }
  }

  def anticlockwise(heading: Int): Int = {
    val h2 = heading - 1
    if (h2 < 1) {
      8
    } else {
      h2
    }
  }

  def axisAligned(heading: Int): Boolean = AxisAligned.contains(heading)

  def angleType(heading: Int): Int = if (axisAligned(heading)) {
    AXIS
  } else {
    DIAG
  }

  /**
    * Assuming you are starting with something facing East OR Southeat, return the number of 90 degree
    * rotations  to read that heading.
    *
    * @param destAngle destination angle you want to rotate to
    * @return a SnapAngle, the number of 90 degree rotations you need to make
    */
  def rotationToHeading(destHeading: Int): SnapAngle = destHeading match {
    // assumes you are starting from "East"
    case RingHeadings.East => SnapAngle(0)
    case RingHeadings.South => SnapAngle(1)
    case RingHeadings.West => SnapAngle(2)
    case RingHeadings.North => SnapAngle(3)
    // assumes you are starting from "Southeast"
    case RingHeadings.SouthEast => SnapAngle(0)
    case RingHeadings.SouthWest => SnapAngle(1)
    case RingHeadings.NorthWest => SnapAngle(2)
    case RingHeadings.NorthEast => SnapAngle(3)
  }

}

/**
  * A ring-based layout that uses 8 fixed angles matching standard compass headings.
  *
  * @param innerRadius     radius from the center to the inner wall of the middle ring
  * @param midWallToAnchor distance from the inner wall of the middle ring to the anchor
  *                        (of an axis-aligned mid ring SG)
  */
class RingLayout(
  innerRadius: Int,
  midWallToAnchor: Int,
) {
  // this layout has exactly 8 discrete angles that add up to 360 degrees
  val anglesPer360: Int = 8

  val radius = innerRadius + midWallToAnchor
  val midRingAnchors = Map(
    RingHeadings.East -> new PointXY(radius, 0),
    RingHeadings.SouthEast -> new PointXY(radius, radius),
    RingHeadings.South -> new PointXY(0, radius),
    RingHeadings.SouthWest -> new PointXY(-radius, radius),
    RingHeadings.West -> new PointXY(-radius, 0),
    RingHeadings.NorthWest -> new PointXY(-radius, -radius),
    RingHeadings.North -> new PointXY(0, -radius),
    RingHeadings.NorthEast -> new PointXY(radius, -radius),
  )

}