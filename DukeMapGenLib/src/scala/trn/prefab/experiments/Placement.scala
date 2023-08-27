package trn.prefab.experiments

import trn.prefab.{MapWriter, PastedSectorGroup, RedwallConnector, SectorGroup}

// case class PlacementOps(allowRotation: Boolean, allowOverlap: Boolean)

object Placement {

  // TODO improved version currently at trn.prefab.experiments.subway.Subway1 .rotateAndPasteToTrack
  // TODO see also SnapAngle.rotateUntil2
  private def placements(writer: MapWriter, existingConns: Seq[RedwallConnector], sg: SectorGroup, allowOverlap: Boolean = false): Seq[Placement] = {
    val conns2 = sg.allRedwallConnectors
    existingConns.flatMap(c1 => conns2.map(c2 => Placement(c1, c2, sg))).filter { p =>
      writer.canPlaceAndConnect(p.existing, p.newConn, p.newSg, checkSpace = !allowOverlap)
    }
  }

  /**
    * Finds the first possible option for pasting the new sector group.
    * This tries to avoid hitting all possible combinations, so it is not random.
    */
  def firstPasteOption(
    writer: MapWriter,
    existing: PastedSectorGroup,
    newGroup: SectorGroup,
    allowRotation: Boolean = true,
    allowOverlap: Boolean = false
  ): Option[Placement] = {
    val conns1 = existing.unlinkedRedwallConnectors

    val rotations: Seq[SectorGroup] = if(!allowRotation){ Seq(newGroup) }else{ newGroup.allRotations }
    rotations.view.map(sg => placements(writer, conns1, sg).headOption).collectFirst { case Some(p) => p}
  }

  def allPasteOptions(
    writer: MapWriter,
    existing: PastedSectorGroup,
    newGroup: SectorGroup,
    allowRotation: Boolean = true, // TODO - create a PlacementArgs object(?)
    allowOverlap: Boolean = false // TODO - check Z instead of just a yes/no for overlap
  ): Seq[Placement] = {
    val conns1 = existing.unlinkedRedwallConnectors

    val rotations: Seq[SectorGroup] = if(!allowRotation){ Seq(newGroup) }else{ newGroup.allRotations }
    rotations.flatMap(sg => placements(writer, conns1, sg))
  }





  // ----------- deprecated below here....or is it? --------------

  private def isMatch(writer: MapWriter, existing: RedwallConnector, newConn: RedwallConnector, newGroup: SectorGroup): Boolean = {
    if(existing.isMatch(newConn)){
      val t = newConn.getTransformTo(existing)
      //spaceAvailable(newGroup.boundingBox.translate(t.asXY()))
      writer.spaceAvailable(newGroup, t.asXY)
    }else{
      false
    }
  }

  /**
    * @deprecated
    */
  def pasteOptions(writer: MapWriter, existing: PastedSectorGroup, newGroup: SectorGroup): Seq[Placement] = {

    def possibleConnections(g1: PastedSectorGroup, g2: SectorGroup) = {
      val conns1 = g1.unlinkedRedwallConnectors
      val conns2 = g2.allRedwallConnectors
      conns1.flatMap(c1 => conns2.map(c2 => Placement(c1, c2, g2))).filter { p =>
        //case (c1, c2, _) => isMatch(c1, c2, g2)
        isMatch(writer, p.existing, p.newConn, p.newSg)
      }
    }

    // TODO TODO TODO - add a feature c1.isMatch(c2, allowRotation=True) and it tells you what rotation to use!
    // TODO - for now, hacking this together ...
    //val allOptions = possibleConnections(existing, newGroup) ++ possibleConnections(existing, newGroup.rotateCW)
    Seq(newGroup, newGroup.rotateCW, newGroup.rotate180, newGroup.rotateCCW).flatMap { g =>
      possibleConnections(existing, g)
    }
  }
}
/**
  *
  * @param existing connector in an existing, PastedSectorGroup
  * @param newConn connector in a new SectorGroup that has not been pasted yet
  * @param newSg the new SectorGroup that has not been pasted yet, which contains newConn
  */
case class Placement(existing: RedwallConnector, newConn: RedwallConnector, newSg: SectorGroup) {

}
