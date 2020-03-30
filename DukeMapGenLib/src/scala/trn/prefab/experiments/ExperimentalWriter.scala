package trn.prefab.experiments

import trn.prefab.{MapWriter, PastedSectorGroup, RandomX, RedwallConnector, SectorGroup, SimpleConnector}

/**
  * This is for code that I'm not sure should go into MapWriter yet.
  */
object ExperimentalWriter {

  def isEastConn(c: RedwallConnector): Boolean = {
    val EastConn = SimpleConnector.EastConnector
    EastConn.matches(c)
  }

  /**
    * paste using ANY connection that fits
    * @param allowOverlap - a temp hack for putting sectors over sectors.  TODO check z floor/ceil instead
    */
  def tryPasteConnectedTo(
    writer: MapWriter,
    random: RandomX, // TODO - just pull from the writer
    existing: PastedSectorGroup,
    newGroup: SectorGroup,
    allowOverlap: Boolean = false
  ): Option[PastedSectorGroup] = {

    Placement.allPasteOptions(writer, existing, newGroup, allowRotation = true, allowOverlap = allowOverlap)
    val allOptions = Placement.pasteOptions(writer, existing, newGroup)
    if(allOptions.size < 1){
      None
    }else{
      //val (c1, c2, g) = random.randomElement(allOptions)
      val p = random.randomElement(allOptions)
      //Some(pasteAndLink(c1, g, c2))
      Some(writer.pasteAndLink(p.existing, p.newSg, p.newConn))
    }
  }



}
