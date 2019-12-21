package trn.prefab

import trn.{Sprite, Map => DMap}
import trn.MapImplicits._
import trn.prefab.hypercube.GridCell

import scala.collection.mutable

object SectorGroupHints {
  val HintTex = 355 // construction sprite
  val HintPalette = 2

  lazy val Empty = SectorGroupHints(None, Seq(), HypercubeEdge(), 1)

  //
  // LOTAG VALUES:  General
  //
  val LtMaxCopies = 5000
  // I think the max value of a lotag (unsigned short) is 32767


  //
  // LOTAG VALUES:  Algo-specific Hints
  //

  val HypercubeGridX = 6000
  val HypercubeGridY = 6001
  val HypercubeGridZ = 6002
  val HypercubeGridW = 6003

  // sector group with this marker must be on the edge of the cube (in the XY plane only)
  val HyperCubeEdgeXY = 6004 // and hitag 1 means rotate so sprite points at edge

  // manually specify the size, in rooms, of this room in the Z dimension (starting from the anchor)
  // hitag:  0 is ignore, 1 is normal, 2 means two rooms high, etc
  // NOTE:  this is the TOTAL height
  val HypercubeRoomHeightZ = 6005

  private val AllLotags = Seq(LtMaxCopies, 6000, 6001, 6002, 6003)


  def isHint(s: Sprite): Boolean = s.getTex == HintTex && s.getPal == HintPalette && s.getLotag > 0

  def checkValid(s: Sprite): Unit = {
    if(isHint(s)){
      SpriteLogicException.requireSprite(AllLotags.contains(s.getLotag), "invalid hint sprite", s)
    }
  }

  def apply(sprites: Seq[Sprite]): SectorGroupHints = {
    var maxCopies: Option[Int] = None

    //var hypergridEdge = HypercubeEdge()
    var xyEdgeOnly: Boolean = false
    val hypergridEdgeAngles = mutable.Buffer[Int]()

    val hypergridHints: mutable.Buffer[HypercubeGridHint] = mutable.Buffer()
    var roomHeight: Int = 1
    sprites.filter(isHint).foreach { _ match {
      case s: Sprite if s.getLotag == LtMaxCopies => maxCopies = Some(s.getHiTag)
      //case s: Sprite if s.getLotag >= HypercubeGridX && s.getLotag <= HypercubeGridW => {
      case s: Sprite if s.getLotag == HyperCubeEdgeXY => {
        xyEdgeOnly = true
        hypergridEdgeAngles ++= HypercubeEdge.heading(s)
        //hypergridEdge = HypercubeEdge(s)
      }
      case s: Sprite if HypercubeGridHint.isGridHint(s) => {
        hypergridHints.append(HypercubeGridHint(s.getLotag, s.getHiTag))
      }
      case s: Sprite if s.getLotag == HypercubeRoomHeightZ && s.getHiTag > 1 => roomHeight = s.getHiTag
      case s: Sprite => throw new SpriteLogicException(s"invalid hint sprite (lotag=${s.getLotag}")
    } }
    SectorGroupHints(maxCopies, hypergridHints, HypercubeEdge(xyEdgeOnly, hypergridEdgeAngles), roomHeight)
  }

  def apply(map: DMap): SectorGroupHints = apply(map.allSprites)
}


object HypercubeEdge {

  def heading(s: Sprite): Option[Int] = {
    require(SectorGroupHints.isHint(s))
    if(s.getHiTag == 1){
      val h = Option(Heading.fromDukeAngle(s.getAngle))
      SpriteLogicException.throwIfSprite(h.isEmpty, "invalid sprite angle for edge rotation hint", s)
      h.map(_.toInt)
    }else{
      None
    }
  }

  // def apply(s: Sprite): HypercubeEdge = {
  //   require(SectorGroupHints.isHint(s))
  //   val heading = if(s.getHiTag == 1){
  //     val h = Option(Heading.fromDukeAngle(s.getAngle))
  //     SpriteLogicException.throwIfSprite(h.isEmpty, "invalid sprite angle for edge rotation hint", s)
  //     h.map(_.toInt)
  //   }else{
  //     None
  //   }
  //   HypercubeEdge(
  //     true,
  //     heading.toSeq
  //   )
  // }
}
case class HypercubeEdge(xyEdgeOnly: Boolean = false, xyEdgeAngle: Seq[Int] = Seq.empty)


object PartialCell {
  lazy val EMPTY = new PartialCell(Map())

  def apply(gh: HypercubeGridHint): PartialCell = new PartialCell(Map(gh.axis -> gh.coord))

  def apply(x: Int, y: Int, z: Int, w: Int): PartialCell = new PartialCell(Map(
    SectorGroupHints.HypercubeGridX -> x,
    SectorGroupHints.HypercubeGridY -> y,
    SectorGroupHints.HypercubeGridZ -> z,
    SectorGroupHints.HypercubeGridW -> w
  ))

  def apply(t: (Int, Int, Int, Int)): PartialCell = apply(t._1, t._2, t._3, t._4)

  private def conflict(c1: Option[Int], c2: Option[Int]): Boolean = (c1, c2) match {
    case (Some(c1), Some(c2)) if c1 != c2 => true
    case _ => false
  }
}
case class PartialCell(axisToCoord: Map[Int, Int]) {

  // does not allow conflicts
  def merged(other: PartialCell): PartialCell = {
    require(axisToCoord.keySet.intersect(other.axisToCoord.keySet).size == 0)
    PartialCell((axisToCoord.toSeq ++ other.axisToCoord.toSeq).toMap)
  }

  def union(other: PartialCell): PartialCell = {
    val newMap = (axisToCoord.keySet ++ other.axisToCoord.keySet).flatMap { axis: Int =>
      val v = (axisToCoord.get(axis), other.axisToCoord.get(axis)) match {
        case (Some(a), Some(b)) if a == b => Some(a)
        case (Some(a), Some(b)) if a != b => None
        case (None, None) => None
        case (aOpt, bOpt) => aOpt.orElse(bOpt)
      }
      v.map(axis -> _)
    }.toMap
    PartialCell(newMap )
  }

  def x: Option[Int] = axisToCoord.get(SectorGroupHints.HypercubeGridX)
  def y: Option[Int] = axisToCoord.get(SectorGroupHints.HypercubeGridY)
  def z: Option[Int] = axisToCoord.get(SectorGroupHints.HypercubeGridZ)
  def w: Option[Int] = axisToCoord.get(SectorGroupHints.HypercubeGridW)

  private def all: Seq[Option[Int]] = Seq(x, y, z, w)

  /**
    * @return true if the cells have enough info to say they are on different axis (e.g. both have z defined,
    *         and z is different)
    */
  def conflicts(other: PartialCell): Boolean = all.zip(other.all).exists { case (a, b) => PartialCell.conflict(a, b)}
}

object HypercubeGridHint {

  def isGridHint(s: Sprite): Boolean = {
    s.getLotag >= SectorGroupHints.HypercubeGridX && s.getLotag <= SectorGroupHints.HypercubeGridW
  }

  // type PartialCell = Seq[HypercubeGridHint]

  // def groupByAxis(hints: Seq[HypercubeGridHint]): Map[Int, Seq[HypercubeGridHint]] = {
  //   hints.groupBy(hint => hint.axis)
  // }

  // def axisCrossProduct(allHints: Seq[HypercubeGridHint]): Seq[Seq[HypercubeGridHint]] = {
  //   val x = allHints.groupBy(_.axis)
  //   x.foldLeft(Seq[PartialCell]()) { case (results, (_, gridHints)) =>
  //       results.flatMap { cell => gridHints.map { cell2 => cell ++ Seq(cell2) } }
  //   }
  // }

  def crossProduct(left: Traversable[PartialCell], right: Traversable[PartialCell]): Seq[PartialCell] = {
    left.flatMap { c1 => right.map { c2 => c1.merged(c2) } }.toSeq
  }

  private def combine(cells: Iterable[Traversable[PartialCell]]): Traversable[PartialCell] = {
    //cells.reduce{ case (left: Seq[PartialCell], right: Seq[PartialCell]) => crossProduct(left, right) }
    if(cells.size == 0){
      Seq()
    }else{
      cells.reduce(crossProduct)
    }
  }

  def calculateCells(allHints: Seq[HypercubeGridHint]): Seq[PartialCell] = {
    val allHints2: Seq[HypercubeGridHint] = allHints.toSet.toSeq
    val x = allHints2.groupBy(h => h.axis).mapValues(hints => hints.map(PartialCell(_)))
    combine(x.values).toSeq
  }

  def topLeft(cells: Seq[PartialCell]): Option[(Int, Int)] = {
    val x = cells.flatMap(p => p.x)
    val y = cells.flatMap(p => p.y)
    if(x.size > 0 && y.size > 0){
      Some((x.min,y.min))
    }else{
      None
    }
  }

  // cant decide if i want to align by top floor or bottom floor, so for now, not supporting multiple
  def getLowestFloor(cells: Seq[PartialCell]): Int = {
    cells.flatMap(_.z).min
  }
}

case class HypercubeGridHint(val axis: Int, val coord: Int)


/**
  * Similar to sector group properties, but these are optional and depend on the algorithm
  *
  * TODO - ideas
  *   - sector groups that can't be next to themselves
  *   - sector groups that must be fully connected (e.g. hallways)
  *   - sector groups with individual connectors that must be fully connected
  */
case class SectorGroupHints(
  maxCopies: Option[Int],
  hypercubeGridHints: Seq[HypercubeGridHint],
  hypercubeEdge: HypercubeEdge,
  roomHeight: Int
) {

  // give the bottom location of the room, return all the others
  def otherCells(cell: GridCell): Seq[GridCell] = {
    if(roomHeight < 2){
      Seq()
    }else{
      (1 to roomHeight - 1).map(dz => cell.add((0, 0, dz, 0)))
    }
  }

  def otherCells(cell: (Int, Int, Int, Int)): Seq[GridCell] = otherCells(GridCell(cell))

}
