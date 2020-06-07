package trn.prefab

import trn.prefab.experiments.Placement
import trn.{Map => DMap}

case class PasteOptions(
  allowOverlap: Boolean = false,
  allowRotate: Boolean = true,
  // exactHeightMatch = false // TODO implement this?
)

case class Placement2(
  extantPsg: PastedSectorGroup,
  extantConn: RedwallConnector,
  newSg: SectorGroup,
  newConn: RedwallConnector
)
// existing
// extant
// pasted

/**
  * Yet another class for copy and pasting prefabs.  This one should finally have a decent interface.
  *
  * Compare to (and maybe replace a lot of):
  * - SgMapBuilder
  * - JigsawPlacer
  * - MapWriter
  * - ExperimentalWriter
  * - FirstPrefabExperiment builder
  * - PipeDream builder
  * - PoolExperiment builder
  * - SquareTileMain builder
  */
trait MapWriter2 {

  def outMap: DMap

  def random: RandomX

  def canPlaceAndConnect(
    existingConn: RedwallConnector,
    newConn: RedwallConnector,
    newSg: SectorGroup,
    checkSpace: Boolean = true
  ): Boolean

  def pasteAndLink(
    existingConn: RedwallConnector,
    newSg: SectorGroup,
    newConn: RedwallConnector
  ): PastedSectorGroup

  private def allConns(psg: PastedSectorGroup) = psg.redwallConnectors.map(c => (psg, c))

  // NOTE: this can't handle rotation, because connectors have to be rescanned after a sector group rotate
  private[prefab]def findPlacementsRaw(
    existing: Seq[(PastedSectorGroup, RedwallConnector)], // need psg to measure Z
    sg: SectorGroup,
    sgConns: Option[Seq[RedwallConnector]] = None, // None means use any connector
    allowOverlap: Boolean = false
  ): Seq[Placement2] = {

    val newConns = sgConns.getOrElse(sg.allRedwallConnectors)
    val all = existing.flatMap {
      case (psg, existingConn) => newConns.map{ newConn =>
        Placement2(psg, existingConn, sg, newConn)
      }
    }
    all
      .filter(p => !p.extantConn.isLinked(p.extantPsg.map))
      .filter(p => canPlaceAndConnect(p.extantConn, p.newConn, p.newSg, checkSpace = !allowOverlap))
  }

  def pasteAndConnect(p: Placement2): PastedSectorGroup = pasteAndLink(p.extantConn, p.newSg, p.newConn)

  def findPlacementsForSg(
    psg: PastedSectorGroup,
    sg: SectorGroup,
    options: PasteOptions = PasteOptions()
  ): Seq[Placement2] = {
    val sgs = if(options.allowRotate){
      sg.allRotations
    }else{
      Seq(sg)
    }
    sgs.flatMap{ newSg =>
      findPlacementsRaw(allConns(psg), newSg, None, options.allowOverlap)
    }
  }

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





  // THIS sg with this conn to THIS psg (any conn)
  def tryPasteConnectedTo(
    map: DMap,
    psg: PastedSectorGroup,
    sg: SectorGroup,
    sgConn: Option[RedwallConnector],
    options: PasteOptions
  ): Option[PastedSectorGroup] = {

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
      findPlacementsRaw(allConns(psg), newSg, newSgConn.map(c => Seq(c)), options.allowOverlap)
    }
    // val placements = findPlacementsRaw(allConns(psg), sg, sgConn.map(c => Seq(c)), options.allowOverlap)

    if(placements.isEmpty){
      None
    }else{
      Some(pasteAndConnect(random.randomElement(placements)))
    }
  }

  // THIS sg with this conn to THIS psg with this conn
  def tryPasteConnectedTo(
    map: DMap,
    existingConn: RedwallConnector,
    sg: SectorGroup,
    sgConn: Option[RedwallConnector]
  ): PastedSectorGroup = ???

}
