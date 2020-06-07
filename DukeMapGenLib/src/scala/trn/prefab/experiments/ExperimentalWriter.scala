package trn.prefab.experiments

import trn.prefab.{MapWriter, PasteOptions, PastedSectorGroup, RandomX, RedwallConnector, SectorGroup, SimpleConnector}

/**
  * TODO - get rid of this, have everything call methods on MapWriter2 instead.
  *
  * This WAS for code that I'm not sure should go into MapWriter yet.
  */
object ExperimentalWriter {

  def isEastConn(c: RedwallConnector): Boolean = {
    val EastConn = SimpleConnector.EastConnector
    EastConn.matches(c)
  }

  /**
    * TODO - this has been replaced by the one on MapWriter
    *
    * paste using ANY connection that fits
    * @param allowOverlap - a temp hack for putting sectors over sectors.  TODO check z floor/ceil instead
    * @deprecated
    */
  def tryPasteConnectedTo(
    writer: MapWriter,
    random: RandomX, // TODO - just pull from the writer
    existing: PastedSectorGroup,
    newGroup: SectorGroup,
    allowOverlap: Boolean = false
  ): Option[PastedSectorGroup] = {


    // Placement.allPasteOptions(writer, existing, newGroup, allowRotation = true, allowOverlap = allowOverlap)
    // val allOptions = Placement.pasteOptions(writer, existing, newGroup)
    val allOptions = writer.findPlacementsForSg(existing, newGroup, PasteOptions(allowOverlap = allowOverlap))
    if(allOptions.size < 1){
      None
    }else{
      val p = random.randomElement(allOptions)
      //Some(writer.pasteAndLink(p.existing, p.newSg, p.newConn))
      Some(writer.pasteAndLink(p.extantConn, p.newSg, p.newConn))
    }
  }



}
