package trn.prefab

import trn.prefab.experiments.Placement
import trn.{IdMap, PointXYZ, RandomX, Map => DMap}

import scala.collection.JavaConverters._

object CompoundGroup {
  def apply(sg: SectorGroup): CompoundGroup = CompoundGroup(sg, Seq.empty)

  def apply(sg: SectorGroup, teleportChildren: java.util.List[SectorGroup]): CompoundGroup = {
    new CompoundGroup(sg, teleportChildren.asScala)
  }
}

/**
  * If necessary, this can become a trait with multiple options.
  *
  * @param sg   the main sector group to paste; only redwall conns in this group will be used.
  * @param teleportChildGroups other groups that are conceptually "part of" the main sg and need to be pasted with it
  */
case class CompoundGroup(sg: SectorGroup, teleportChildGroups: Seq[SectorGroup]){

  def allRotations: Seq[CompoundGroup] = Seq(this, rotateCW, rotate180, rotateCCW)

  def rotateCW: CompoundGroup = CompoundGroup(
    sg.rotateCW,
    teleportChildGroups.map(_.rotateCW)
  )

  def rotate180: CompoundGroup = rotateCW.rotateCW

  def rotateCCW: CompoundGroup = rotateCW.rotateCW.rotateCW
}

case class PasteOptions(
  allowOverlap: Boolean = false,
  allowRotate: Boolean = true,
  // exactHeightMatch = false // TODO implement this?
  // TODO add option to reject connections between below water and above water sectors
)

case class Placement2(
  extantPsg: PastedSectorGroup,
  extantConn: RedwallConnector,
  newSg: SectorGroup,
  newConn: RedwallConnector,
  floatingGroups: Seq[SectorGroup]
)

/**
  * Yet another class for copy and pasting prefabs.  This one should finally have a decent interface.
  *
  * Compare to (and maybe replace a lot of):
  * - SgMapBuilder
  * - JigsawPlacer (provides stuff for adding a sector between TWO existing sectors)
  * - MapWriter
  * - SquareTileMain builder
  */
trait MapWriter2 {

  def outMap: DMap

  def random: RandomX

  def sgPacker: Option[SectorGroupPacker]

  // TODO - DRY with AnywhereBuilder.placeAnywhere() in MapBuilder.scala
  final def pasteAnywhere(sg: SectorGroup): PastedSectorGroup = {
    val packer = sgPacker.getOrElse(
      throw new RuntimeException("Cannot paste sector to 'anywhere' because the packing area was not specified.  Pass an instance of SectorGroupPacker to MapWriter")
    )
    val topLeft = packer.reserveArea(sg)
    val tr = sg.boundingBox.getTranslateTo(topLeft).withZ(0)
    val (psg, _) = pasteSectorGroup2(sg, tr, Seq.empty) // TODO this IS the method for pasting floating groups; circular logic here
    psg
  }

  def pasteSectorGroup2(sg: SectorGroup, translate: PointXYZ, floatingGroups: Seq[SectorGroup]): (PastedSectorGroup, IdMap)

  def canPlaceAndConnect(
    existingConn: RedwallConnector,
    newConn: RedwallConnector,
    newSg: SectorGroup,
    checkSpace: Boolean = true
  ): Boolean

  def pasteAndLink(
    existingConn: RedwallConnector,
    newSg: SectorGroup,
    newConn: RedwallConnector,
    floatingGroups: Seq[SectorGroup]
  ): PastedSectorGroup

  private def allConns(psg: PastedSectorGroup) = psg.redwallConnectors.map(c => (psg, c))

  // NOTE: this can't handle rotation, because connectors have to be rescanned after a sector group rotate
  private[prefab] def findPlacementsRaw(
    existing: Seq[(PastedSectorGroup, RedwallConnector)], // need psg to measure Z
    sg: SectorGroup,
    sgConns: Option[Seq[RedwallConnector]] = None, // None means use any connector
    floatingGroups: Seq[SectorGroup] = Seq.empty,
    allowOverlap: Boolean = false
  ): Seq[Placement2] = {

    val newConns = sgConns.getOrElse(sg.allRedwallConnectors)
    val all = existing.flatMap {
      case (psg, existingConn) => newConns.map{ newConn =>
        Placement2(psg, existingConn, sg, newConn, floatingGroups)
      }
    }
    all
      .filter(p => !p.extantConn.isLinked(p.extantPsg.map))
      .filter(p => canPlaceAndConnect(p.extantConn, p.newConn, p.newSg, checkSpace = !allowOverlap))
  }

  def pasteAndConnect(p: Placement2): PastedSectorGroup = pasteAndLink(p.extantConn, p.newSg, p.newConn, p.floatingGroups)

  def findPlacementsForSg(
    psg: PastedSectorGroup,
    //sg: SectorGroup,
    sg: CompoundGroup,
    options: PasteOptions = PasteOptions()
  ): Seq[Placement2] = {
    val sgs = if(options.allowRotate){
      sg.allRotations
    }else{
      Seq(sg)
    }
    sgs.flatMap{ newSg =>
      findPlacementsRaw(allConns(psg), newSg.sg, None, newSg.teleportChildGroups, allowOverlap = options.allowOverlap)
    }
  }

  def tryPasteConnectedTo(
    psg: PastedSectorGroup,
    sg: SectorGroup,
    options: PasteOptions
  ): Option[PastedSectorGroup] = {
    tryPasteConnectedTo(psg, CompoundGroup(sg, Seq.empty), options).flatMap(_.headOption)
    //val allOptions = findPlacementsForSg(psg, sg, options)
    //if(allOptions.size < 1){
    //  None
    //}else{
    //  val p = random.randomElement(allOptions)
    //  Some(pasteAndConnect(p))
    //}
  }

  def tryPasteConnectedTo(
    psg: PastedSectorGroup,
    sg: CompoundGroup,
    options: PasteOptions
  ): Option[Seq[PastedSectorGroup]] = {
    val allOptions = findPlacementsForSg(psg, sg, options)
    if(allOptions.size < 1){
      None
    }else{
      val p = random.randomElement(allOptions)
      Some(Seq(pasteAndConnect(p)))
    }
  }

  def tryPasteConnectedTo(
    psg: PastedSectorGroup,
    extantConn: RedwallConnector,
    sg: SectorGroup,
    options: PasteOptions
  ): Option[PastedSectorGroup] = {
    val sgs = if(options.allowRotate){
      sg.allRotations
    }else{
      Seq(sg)
    }
    val allOptions = sgs.flatMap{ newSg =>
      findPlacementsRaw(Seq((psg, extantConn)), newSg, None, Seq.empty, options.allowOverlap)
    }
    random.randomElementOpt(allOptions).map(placement => pasteAndConnect(placement))
    //if(allOptions.size < 1){
    //  None
    //}else{
    //  Some(pasteAndConnect(random.randomElement(allOptions)))
    //}
  }

  // Was trying to write this for FirstPrefabExperiment
  //def tryPastedConnectedTo(
  //  psg: PastedSectorGroup,
  //  psgConn: RedwallConnector,
  //  sg: SectorGroup,
  //  sgConn: RedwallConnector
  //  // TODO - param for allowOverlap?
  //): Option[PastedSectorGroup] = {
  //  if(canPlaceAndConnect(psgConn, sgConn, sg)){
  //    Some(pasteAndLink(psgConn, sg, sgConn))
  //  }else{
  //    None
  //  }
  //}

  // ------------------
  // Compass Directions
  // ------------------

  def pasteAndLinkNextTo(
    existingGroup: PastedSectorGroup,
    existingConn: ConnectorFilter,
    newGroup: SectorGroup,
    newConn: ConnectorFilter
  ): PastedSectorGroup = {
    val conn1 = existingGroup.findFirstConnector(existingConn).asInstanceOf[RedwallConnector]
    pasteAndLink(conn1, newGroup, newGroup.findFirstConnector(newConn).asInstanceOf[RedwallConnector], Seq.empty)
  }

  def pasteSouthOf(
    existing: PastedSectorGroup,
    newGroup: SectorGroup
  ): PastedSectorGroup = pasteAndLinkNextTo(existing, CompassWriter.SouthConn, newGroup, CompassWriter.NorthConn)

  def pasteEastOf(
    existing: PastedSectorGroup,
    newGroup: SectorGroup
  ): PastedSectorGroup = pasteAndLinkNextTo(existing, CompassWriter.EastConn, newGroup, CompassWriter.WestConn)

  def pasteWestOf(
    existing: PastedSectorGroup,
    newGroup: SectorGroup
  ): PastedSectorGroup = pasteAndLinkNextTo(existing, CompassWriter.WestConn, newGroup, CompassWriter.EastConn)

  def pasteNorthOf(
    existing: PastedSectorGroup,
    newGroup: SectorGroup
  ): PastedSectorGroup = pasteAndLinkNextTo(existing, CompassWriter.NorthConn, newGroup, CompassWriter.SouthConn)

  // -------------------------------------
  // CODE BELOW HERE -- trying to find a better API
  // (this is complicated by the fact that we have to rescan connectors after rotating the sg)
  // -------------------------------------

  // any SG to any PSG
  // any SG to this PSG (any conn)
  // this SG (any conn) to any PSG
  def findPlacements(
    psgs: Seq[PastedSectorGroup],
    sgs: Seq[SectorGroup]
  ): Seq[Placement] = ???

  // this SG (with this conn) to any PSG
  // this SG (with this conn) to this PSG (any conn)
  def findPlacements(
    psgs: Seq[PastedSectorGroup],
    sg: SectorGroup,
    sgConn: Option[RedwallConnector]
  ): Seq[Placement] = ???

  // any SG to this PSG (with this conn)
  def findPlacements(
    existingConn: RedwallConnector,
    sgs: Seq[SectorGroup]
  ): Seq[Placement] = ???

  // this SG (with this conn) to this PSG (with this conn)
  def findPlacements(
    existingConn: RedwallConnector,
    sg: SectorGroup,
    sgConn: Option[RedwallConnector]
  ): Seq[Placement] = ???




  // cant rotate
  def tryPasteConnectedTo(
    extantPsg: PastedSectorGroup,
    extantConn: RedwallConnector,
    newSg: SectorGroup,
    newConn: RedwallConnector
  ): Option[PastedSectorGroup] = {

    val placements = findPlacementsRaw(Seq((extantPsg, extantConn)), newSg, Some(Seq(newConn)), Seq.empty)
    if(placements.isEmpty){
      None
    }else{
      Some(pasteAndConnect(random.randomElement(placements)))
    }

  }

  def tryPasteConnectedTo(
    map: DMap,
    psg: PastedSectorGroup,
    sg: SectorGroup,
    sgConn: Option[RedwallConnector],
    options: PasteOptions
  ): Option[PastedSectorGroup] = {
    val floatingGroups: Seq[SectorGroup] = Seq.empty

    // TODO - this is complete untested.  Not sure if I want to keep it

    val newSgAndConn = if(options.allowRotate){
      if(sgConn.isDefined){
        throw new RuntimeException("allowing rotation and using a specific conn for the new sg is not supported")
      }else{
        sg.allRotations.map(s => (s, None))
      }
    }else{
      Seq((sg, sgConn))
    }

    val placements = newSgAndConn.flatMap{ case (newSg, newSgConn) =>
      findPlacementsRaw(allConns(psg), newSg, newSgConn.map(c => Seq(c)), floatingGroups, options.allowOverlap)
    }
    // val placements = findPlacementsRaw(allConns(psg), sg, sgConn.map(c => Seq(c)), options.allowOverlap)

    if(placements.isEmpty){
      None
    }else{
      Some(pasteAndConnect(random.randomElement(placements)))
    }
  }

  //// THIS sg with this conn to THIS psg with this conn
  //def tryPasteConnectedTo(
  //  map: DMap,
  //  existingConn: RedwallConnector,
  //  sg: SectorGroup,
  //  sgConn: Option[RedwallConnector]
  //): PastedSectorGroup = ???

}
