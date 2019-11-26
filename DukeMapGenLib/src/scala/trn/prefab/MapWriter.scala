package trn.prefab

import trn.duke.PaletteList
import trn.{PointXY, PointXYZ, Map => DMap}
import trn.MapImplicits._



// creating this as an adapter for old (new?) code
class MapBuilderAdapter(val outMap: DMap) extends MapBuilder {

}

object MapWriter {
  val WestConn = SimpleConnector.WestConnector
  val EastConn = SimpleConnector.EastConnector
  val NorthConn = SimpleConnector.NorthConnector
  val SouthConn = SimpleConnector.SouthConnector

  def westConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(WestConn).asInstanceOf[RedwallConnector]
  def eastConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(EastConn).asInstanceOf[RedwallConnector]
  def northConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(NorthConn).asInstanceOf[RedwallConnector]
  def southConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SouthConn).asInstanceOf[RedwallConnector]

  // TODO - should not need different methods for SectorGroup and PastedSectorGroup
  def westConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(WestConn).asInstanceOf[RedwallConnector]
  def eastConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(EastConn).asInstanceOf[RedwallConnector]
  def northConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(NorthConn).asInstanceOf[RedwallConnector]
  def southConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(SouthConn).asInstanceOf[RedwallConnector]

  def painted(sg: SectorGroup, colorPalette: Int, excludeTextures: Seq[Int] = Seq.empty): SectorGroup = {
    val sg2 = sg.copy
    sg2.getMap.allWalls.foreach { w=>
      if (! excludeTextures.contains(w.getTexture)){
        w.setPal(colorPalette)
      }
    }
    sg2
  }

  def apply(map: DMap): MapWriter = {
    val builder = new MapBuilderAdapter(map)
    new MapWriter(builder, builder.sgBuilder)
  }

  def apply(builder: MapBuilder): MapWriter = new MapWriter(builder, builder.sgBuilder)
}
/**
  * TODO - write unit tests for this class
  *
  * @param builder
  * @param sgBuilder
  */
class MapWriter(val builder: MapBuilder, val sgBuilder: SgMapBuilder) {

  /** throws if the map has too many sectors */
  def checkSectorCount(): Unit = {
    println(s"checkSectorCount(): Total Sector Count: ${builder.outMap.getSectorCount}")
    if(builder.outMap.getSectorCount > 1024){
      throw new Exception("too many sectors") // I think maps are limited to 1024 sectors
    }
  }

  def pastedSectorGroups: Seq[PastedSectorGroup] = sgBuilder.pastedSectorGroups

  def pasteAndLink(
    existingConn: RedwallConnector,
    newSg: SectorGroup,
    newConn: RedwallConnector
  ): PastedSectorGroup = {
    require(Option(existingConn).isDefined)
    require(Option(newSg).isDefined)
    require(Option(newConn).isDefined)
    val cdelta = newConn.getTransformTo(existingConn)
    val (psg, idmap) = builder.pasteSectorGroup2(newSg, cdelta)
    val pastedConn2 = newConn.translateIds(idmap, cdelta)

    //existingConn.linkConnectors(outMap, pastedConn2)
    sgBuilder.linkConnectors(existingConn, pastedConn2)
    psg
  }

  /**
    * TODO - copied from PipeDream
    * TODO - making this one more advanced
    * Tests if there is space for the given sector group AFTER being moved by tx
    */
  def spaceAvailable(sg: SectorGroup, tx: PointXY): Boolean = {
    def conflict(psg: PastedSectorGroup, bb: BoundingBox): Boolean ={
      psg.boundingBox.intersect(bb).map(_.area).getOrElse(0) > 0
    }

    val bb = sg.boundingBox.translate(tx)
    lazy val sgBoxes = sg.fineBoundingBoxes.map(_.translate(tx))

    if(!bb.isInsideInclusive(MapBuilder.mapBounds)){
      false
    }else{
      val conflicts = pastedSectorGroups.filter { psg =>
        conflict(psg, bb) && BoundingBox.nonZeroOverlap(psg.fineBoundingBoxes, sgBoxes)
      }
      conflicts.isEmpty
    }
  }

  /**
    * Tests if the given box is available (empty).  This tests the entire (axis-aligned) box, which means it is
    * an inefficient measure for sector groups that are not box-like.
    * For a better approximation, see spaceAvailable(SectorGroup, PointXY)
    * @param bb the bounding box
    * @return true if the bounding box is in bounds and there are no existing groups in that area.
    */
  def spaceAvailable(bb: BoundingBox): Boolean = {
    bb.isInsideInclusive(MapBuilder.mapBounds) &&
      pastedSectorGroups.filter(psg => psg.boundingBox.intersect(bb).map(_.area).getOrElse(0) > 0).isEmpty
  }


  //
  //  ORDINAL DIRECTION METHODS BELOW
  //
  private def pasteAndLinkNextTo(
    existingGroup: PastedSectorGroup,
    existingConn: ConnectorFilter,
    newGroup: SectorGroup,
    newConn: ConnectorFilter
  ): PastedSectorGroup = {
    val conn1 = existingGroup.findFirstConnector(existingConn).asInstanceOf[RedwallConnector]
    pasteAndLink(conn1, newGroup, newGroup.findFirstConnector(newConn).asInstanceOf[RedwallConnector])
  }

  def pasteSouthOf(
    existing: PastedSectorGroup,
    newGroup: SectorGroup
  ): PastedSectorGroup = pasteAndLinkNextTo(existing, MapWriter.SouthConn, newGroup, MapWriter.NorthConn)

  def pasteEastOf(
    existing: PastedSectorGroup,
    newGroup: SectorGroup
  ): PastedSectorGroup = pasteAndLinkNextTo(existing, MapWriter.EastConn, newGroup, MapWriter.WestConn)

  def pasteWestOf(
    existing: PastedSectorGroup,
    newGroup: SectorGroup
  ): PastedSectorGroup = pasteAndLinkNextTo(existing, MapWriter.WestConn, newGroup, MapWriter.EastConn)

  def pasteNorthOf(
    existing: PastedSectorGroup,
    newGroup: SectorGroup
  ): PastedSectorGroup = pasteAndLinkNextTo(existing, MapWriter.NorthConn, newGroup, MapWriter.SouthConn)
}
