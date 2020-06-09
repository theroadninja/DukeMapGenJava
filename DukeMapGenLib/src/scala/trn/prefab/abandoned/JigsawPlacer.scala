package trn.prefab.abandoned

import trn.prefab._
import trn.prefab.experiments.PrefabExperiment
import trn.{MapLoader, MapView, MathUtil, PointXY, PointXYZ, Sector, Map => DMap}
import trn.MapImplicits._



private[prefab] case class PartialFit(
  translatedSg: SectorGroup,
  firstMatch: ConnMatch,
  // newConn1: RedwallConnector,  // first match: connector in new group
  // existingConn1: RedwallConnector,  // first match:  connector in existing group
  translate: PointXYZ
)

case class Placement(newSg: SectorGroup, conns: Seq[ConnMatch])

/**
  * Figures out how to place SectorGroups such that they match existing connections.
  *
  * TODO: should I have a thing called ConnectorHints ?
  *   - connector-specific and algorithm-specific hints for connectors
  *
  * TODO:  support:
  *   - auto stretching for Z?
  *
  * TODO:  auto stairs??
  *   - strait
  *   - spiral?
  *
  * TODO:  stretching in X or Y
  *   - only along X or Y axis
  *   - sprite marks where the sector group is cut, points on either side move away
  *   - test conns by calculating translateTo and see if X (or Y) delta is zero (and positive/negative)
  *   - may  need to figure out how to adjust textures
  *   - should it warn or fail on teleport/water sectors?
  *
  * TODO - option to only allow rotation 90 degrees?
  *
  * TODO - stretch sectors!
  *
  */
object JigsawPlacer {

  val ZMatchExact = 0 // both floor and ceiling must batch exactly
  val ZMatchFloorExact = 1   // floor z must match exactly
  val ZMatchCeilExact = 2
  val ZMatchOverlap = 3
  val ZMatchOptional = 4 // Z doesnt need to match at all

  /**
    * @returns true if the sectors overlap in the z dimension
    */
  private def overlapZ(s1: Sector, s2: Sector): Boolean = {
    MathUtil.overlaps(s1.getFloorZ, s1.getCeilingZ, s2.getFloorZ, s2.getCeilingZ)
  }

  private def inPlaceMatch(
    sg: SectorGroup,
    newConn: RedwallConnector,
    b: RedwallConnector,
    zMatch: Int = ZMatchOptional,
    //map: ImmutableMapOld
    map: MapView
  ): Boolean = {
    if(newConn.getSectorIds.size() != 1 || b.getSectorIds.size() != 1){
      throw new RuntimeException("multi-sector not supported yet")
    }

    def ceilMatch(s1: Sector, s2: Sector): Boolean = s1.getCeilingZ == s2.getCeilingZ

    def xyMatch(c1: RedwallConnector, c2: RedwallConnector): Boolean = c1.getTransformTo(c2).asXY() == PointXY.ZERO

    lazy val s1: Sector = sg.getMap.getSector(newConn.getSectorIds.get(0))
    lazy val s2: Sector = map.getSector(b.getSectorIds.get(0))

    zMatch match {
      case ZMatchExact => newConn.isMatch(b) && newConn.getTransformTo(b) == PointXYZ.ZERO && ceilMatch(s1, s2)
      case ZMatchFloorExact => newConn.isMatch(b) && newConn.getTransformTo(b) == PointXYZ.ZERO
      case ZMatchCeilExact => newConn.isMatch(b) && xyMatch(newConn, b) && ceilMatch(s1, s2)
      case ZMatchOverlap => newConn.isMatch(b) && xyMatch(newConn, b) && overlapZ(s1, s2)
      case ZMatchOptional => newConn.isMatch(b) && xyMatch(newConn, b)
      case _ => throw new RuntimeException("not supported")
    }
  }

  private def inPlaceMatches(
    partialFit: PartialFit,
    existingConns2: Seq[RedwallConnector],
    zMatch: Int,
    map: DMap
  ): Seq[Placement] = {
    partialFit.translatedSg.allRedwallConnectors.filterNot(_ == partialFit.firstMatch.newConn).flatMap { newConn2 =>
      existingConns2.flatMap { existing2 =>
        if(inPlaceMatch(partialFit.translatedSg, newConn2, existing2, zMatch, map.asView)){
          Some(Placement(partialFit.translatedSg, Seq(partialFit.firstMatch, ConnMatch(newConn2, existing2))))
        }else{
          None
        }
      }
    }
  }

  // it is simpler to get this "hallway" use case working first.  can make it more complicated later.
  // TODO - this is used by hypercube
  def findPlacements(
    sg: SectorGroup,
    existingGroup1: Seq[RedwallConnector],
    existingGroup2: Seq[RedwallConnector],
    map: DMap, // TODO - should be a read only map!
    allowRotation: Boolean,
    zMatch: Int
  ): Seq[Placement] = {
    if(allowRotation){
      sg.allRotations.flatMap(rotated => findPlacements(rotated, existingGroup1, existingGroup2, map, false, zMatch))
    }else{
      val available1 = existingGroup1.filterNot(_.isLinked(map))
      val available2 = existingGroup2.filterNot(_.isLinked(map))
      val potentials = sg.allRedwallConnectors.flatMap { newConn =>
        available1.flatMap { existing =>
          if(newConn.isMatch(existing)){
            val tx = newConn.getTransformTo(existing)
            Some(PartialFit(sg.translated(tx), ConnMatch(newConn, existing), tx))
          }else{
            None
          }
        }
      }
      potentials.flatMap(inPlaceMatches(_, available2, zMatch, map))
    }
  }

  private[abandoned] def findPlacements(
    sg: SectorGroup,
    psg1: PastedSectorGroup,
    psg2: PastedSectorGroup,
    map: DMap,
    allowRotation: Boolean = true,
    zMatch: Int = ZMatchOptional
  ): Seq[Placement] = findPlacements(sg, psg1.redwallConnectors, psg2.redwallConnectors, map, allowRotation, zMatch)
}

class TestBuilder(val outMap: DMap) extends MapBuilder {
}

object JigsawPlacerMain extends PrefabExperiment {
  override def Filename: String = "JIGSAW.MAP"

  override def run(mapLoader: MapLoader): DMap = {
    val sourceMap = mapLoader.load(Filename)
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true);
    run(palette)
  }

  def run(palette: PrefabPalette): DMap = {
    val builder = new TestBuilder(DMap.createNew())
    val writer = new MapWriter(builder, builder.sgBuilder)

    val stays = writer.pasteStays2(palette)
    println(stays)

    def getPsg(groupId: Int): PastedSectorGroup = {
      stays.find(_._1 == Some(groupId)).map(_._2).head
    }
    def getSg(groupId: Int): SectorGroup = palette.getSG(groupId)

    def place(placement: Placement): Unit = {
      writer.pasteAndLink2(placement.newSg, PointXYZ.ZERO, placement.conns)
    }

    val hallway100 = palette.getSectorGroup(100)

    def findPlacements(sg: SectorGroup, psg1: PastedSectorGroup, psg2: PastedSectorGroup, allowRotation: Boolean, zMatch: Int = JigsawPlacer.ZMatchOptional): Seq[Placement] = {
      JigsawPlacer.findPlacements(sg, psg1, psg2, builder.outMap, allowRotation, zMatch)
    }

    val p0 = findPlacements(hallway100, getPsg(1), getPsg(2), false, JigsawPlacer.ZMatchOverlap)
    require(p0.size > 0)
    val x = findPlacements(hallway100, getPsg(1), getPsg(2), false)
    require(x.size > 0)
    require(getPsg(1).unlinkedConnectors.size == 1)
    place(x.head)
    require(getPsg(1).unlinkedConnectors.size == 0)

    require(0 == findPlacements(hallway100, getPsg(3), getPsg(4), false).size)
    val p3 = JigsawPlacer.findPlacements(hallway100, getPsg(3), getPsg(4), builder.outMap, true)
    require(p3.size > 0)
    place(p3.head)

    val p4 = JigsawPlacer.findPlacements(hallway100, getPsg(5), getPsg(6), builder.outMap)
    place(p4.head)

    val p5 = JigsawPlacer.findPlacements(getSg(101), getPsg(7), getPsg(8), builder.outMap)
    place(p5.head)

    // z options
    require(0 == findPlacements(getSg(100), getPsg(9), getPsg(10), false, zMatch =  JigsawPlacer.ZMatchExact).size)
    require(0 == findPlacements(getSg(100), getPsg(9), getPsg(10), false, zMatch =  JigsawPlacer.ZMatchFloorExact).size)
    require(1 == findPlacements(getSg(100), getPsg(9), getPsg(10), false, zMatch = JigsawPlacer.ZMatchCeilExact).size)
    require(1 == findPlacements(getSg(100), getPsg(9), getPsg(10), false, zMatch =  JigsawPlacer.ZMatchOptional).size)
    val p9 = findPlacements(getSg(100), getPsg(9), getPsg(10), false, zMatch = JigsawPlacer.ZMatchOverlap)
    require(p9.size > 0)
    place(p9.head)

    //val p6 = JigsawPlacer.findPlacements(getSg(100), getPsg(9), getPsg(10), builder.outMap, false, zMatch =  JigsawPlacer.ZMatchExact)
    //writer.pasteAndLink2(p6.head.newSg, PointXYZ.ZERO, p6.head.conns)

    require(0 == findPlacements(getSg(100), getPsg(11), getPsg(12), false, JigsawPlacer.ZMatchExact).size)
    require(1 == findPlacements(getSg(100), getPsg(11), getPsg(12), false, JigsawPlacer.ZMatchFloorExact).size)
    require(0 == findPlacements(getSg(100), getPsg(11), getPsg(12), false, JigsawPlacer.ZMatchCeilExact).size)
    require(1 == findPlacements(getSg(100), getPsg(11), getPsg(12), false, JigsawPlacer.ZMatchOptional).size)
    require(1 == findPlacements(getSg(100), getPsg(11), getPsg(12), false, JigsawPlacer.ZMatchOverlap).size)
    place(findPlacements(getSg(100), getPsg(11), getPsg(12), false, JigsawPlacer.ZMatchOverlap).head)

    require(0 == findPlacements(getSg(100), getPsg(13), getPsg(14), false, JigsawPlacer.ZMatchExact).size)
    require(1 == findPlacements(getSg(100), getPsg(13), getPsg(14), false, JigsawPlacer.ZMatchFloorExact).size)
    // b/c of hallway size:
    require(0 == findPlacements(getSg(100), getPsg(13), getPsg(14), false, JigsawPlacer.ZMatchCeilExact).size)
    require(1 == findPlacements(getSg(102), getPsg(13), getPsg(14), false, JigsawPlacer.ZMatchCeilExact).size)
    require(1 == findPlacements(getSg(100), getPsg(13), getPsg(14), false, JigsawPlacer.ZMatchOptional).size)
    require(1 == findPlacements(getSg(100), getPsg(13), getPsg(14), false, JigsawPlacer.ZMatchOverlap).size)
    place(findPlacements(getSg(100), getPsg(13), getPsg(14), false, JigsawPlacer.ZMatchOverlap).head)

    require(0 == findPlacements(getSg(100), getPsg(15), getPsg(16), false, JigsawPlacer.ZMatchExact).size)
    require(0 == findPlacements(getSg(100), getPsg(15), getPsg(16), false, JigsawPlacer.ZMatchFloorExact).size)
    require(0 == findPlacements(getSg(100), getPsg(15), getPsg(16), false, JigsawPlacer.ZMatchCeilExact).size)
    require(0 == findPlacements(getSg(102), getPsg(15), getPsg(16), false, JigsawPlacer.ZMatchCeilExact).size)
    require(1 == findPlacements(getSg(100), getPsg(15), getPsg(16), false, JigsawPlacer.ZMatchOptional).size)
    require(1 == findPlacements(getSg(100), getPsg(15), getPsg(16), false, JigsawPlacer.ZMatchOverlap).size)
    place(findPlacements(getSg(100), getPsg(15), getPsg(16), false, JigsawPlacer.ZMatchOverlap).head)

    require(0 == findPlacements(getSg(100), getPsg(17), getPsg(18), false, JigsawPlacer.ZMatchExact).size)
    require(0 == findPlacements(getSg(100), getPsg(17), getPsg(18), false, JigsawPlacer.ZMatchFloorExact).size)
    require(0 == findPlacements(getSg(100), getPsg(17), getPsg(18), false, JigsawPlacer.ZMatchCeilExact).size)
    require(0 == findPlacements(getSg(102), getPsg(17), getPsg(18), false, JigsawPlacer.ZMatchCeilExact).size)
    require(1 == findPlacements(getSg(100), getPsg(17), getPsg(18), false, JigsawPlacer.ZMatchOptional).size)
    require(0 == findPlacements(getSg(100), getPsg(17), getPsg(18), false, JigsawPlacer.ZMatchOverlap).size)
    place(findPlacements(getSg(100), getPsg(17), getPsg(18), false, JigsawPlacer.ZMatchOptional).head)
    // TODO - learn how to write this room from scratch in code!

    // -- standard stuff below --
    writer.sgBuilder.autoLinkRedwalls()
    writer.setAnyPlayerStart()
    writer.sgBuilder.clearMarkers()
    writer.outMap
  }
}