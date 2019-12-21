package trn.prefab

import java.util

import trn.duke.PaletteList
import trn.{ISpriteFilter, IdMap, PointXY, PointXYZ, Sprite, Map => DMap}
import trn.MapImplicits._

import scala.collection.JavaConverters._


case class ConnMatch(newConn: RedwallConnector, existingConn: RedwallConnector)

// creating this as an adapter for old (new?) code
class MapBuilderAdapter(val outMap: DMap) extends MapBuilder {

}

object MapWriter {
  val MarkerTex = PrefabUtils.MARKER_SPRITE_TEX

  val WestConn = SimpleConnector.WestConnector
  val EastConn = SimpleConnector.EastConnector
  val NorthConn = SimpleConnector.NorthConnector
  val SouthConn = SimpleConnector.SouthConnector

  /** @deprecated */
  def firstConnector(sg: SectorGroup, cf: ConnectorFilter): RedwallConnector = sg.findFirstConnector(cf).asInstanceOf[RedwallConnector]

  def westConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(WestConn).asInstanceOf[RedwallConnector]
  def eastConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(EastConn).asInstanceOf[RedwallConnector]
  def northConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(NorthConn).asInstanceOf[RedwallConnector]
  def southConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SouthConn).asInstanceOf[RedwallConnector]

  def east(sg: SectorGroup): Option[RedwallConnector] = sg.findFirstConnectorOpt(EastConn).map(_.asInstanceOf[RedwallConnector])
  def west(sg: SectorGroup): Option[RedwallConnector] = sg.findFirstConnectorOpt(WestConn).map(_.asInstanceOf[RedwallConnector])
  def north(sg: SectorGroup): Option[RedwallConnector] = sg.findFirstConnectorOpt(NorthConn).map(_.asInstanceOf[RedwallConnector])
  def south(sg: SectorGroup): Option[RedwallConnector] = sg.findFirstConnectorOpt(SouthConn).map(_.asInstanceOf[RedwallConnector])

  // TODO - should not need different methods for SectorGroup and PastedSectorGroup
  def westConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(WestConn).asInstanceOf[RedwallConnector]
  def eastConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(EastConn).asInstanceOf[RedwallConnector]
  def northConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(NorthConn).asInstanceOf[RedwallConnector]
  def southConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(SouthConn).asInstanceOf[RedwallConnector]

  def east(sg: PastedSectorGroup): Option[RedwallConnector] = sg.connectorCollection.findFirstRedwallConn(EastConn)
  def west(sg: PastedSectorGroup): Option[RedwallConnector] = sg.connectorCollection.findFirstRedwallConn(WestConn)
  def north(sg: PastedSectorGroup): Option[RedwallConnector] = sg.connectorCollection.findFirstRedwallConn(NorthConn)
  def south(sg: PastedSectorGroup): Option[RedwallConnector] = sg.connectorCollection.findFirstRedwallConn(SouthConn)

  def painted(sg: SectorGroup, colorPalette: Int, excludeTextures: Seq[Int] = Seq.empty): SectorGroup = {
    val sg2 = sg.copy
    sg2.getMap.allWalls.foreach { w=>
      if (! excludeTextures.contains(w.getTexture)){
        w.setPal(colorPalette)
      }
    }
    sg2
  }

  // this includes the floors
  // TODO - see if we can merge with painted()
  def painted2(sg: SectorGroup, colorPalette: Int, excludeTextures: Seq[Int] = Seq.empty): SectorGroup = {
    val sg2 = sg.copy
    sg2.getMap.allWalls.foreach { w=>
      if (! excludeTextures.contains(w.getTexture)){
        w.setPal(colorPalette)
      }
    }
    sg2.allSectorIds.map(sg2.getMap.getSector(_)).foreach { sector =>
      sector.setFloorPalette(colorPalette)
      sector.setCeilingPalette(colorPalette)
    }
    sg2
  }

  def withElevatorsLinked(
    sg: SectorGroup,
    lowerConn: ElevatorConnector,
    higherConn: ElevatorConnector,
    hitag: Int,
    startLower: Boolean
  ): SectorGroup = {
    val sg2 = sg.copy
    ElevatorConnector.linkElevators(lowerConn, sg2, higherConn, sg2, hitag, startLower)
    sg2
  }

  // this is a merge operation
  def connected(sg1: SectorGroup, sg2: SectorGroup, connectorId: Int): SectorGroup = {
    val c1: RedwallConnector = sg1.getRedwallConnector(connectorId)
    val c2: RedwallConnector = sg2.getRedwallConnector(connectorId)
    sg1.connectedTo(c1, sg2, c2)
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
class MapWriter(val builder: MapBuilder, val sgBuilder: SgMapBuilder, val random: RandomX = new RandomX())
  extends ISectorGroup {

  /** throws if the map has too many sectors */
  def checkSectorCount(): Unit = {
    println(s"checkSectorCount(): Total Sector Count: ${builder.outMap.getSectorCount}")
    if(builder.outMap.getSectorCount > 1024){
      throw new Exception("too many sectors") // I think maps are limited to 1024 sectors
    }
  }

  def outMap: DMap = getMap

  //
  //  Pasting and Linking
  //
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


    pasteAndLink2(newSg, cdelta, Seq(ConnMatch(newConn, existingConn)))
    // val (psg, idmap) = builder.pasteSectorGroup2(newSg, cdelta)
    // val pastedConn2 = newConn.translateIds(idmap, cdelta)
    // sgBuilder.linkConnectors(existingConn, pastedConn2)
    // psg
  }

  def pasteAndLink2(
    newSg: SectorGroup,
    translate: PointXYZ,
    conns: Seq[ConnMatch]
  ): PastedSectorGroup = {
    require(conns.size > 0)
    val (psg, idmap) = builder.pasteSectorGroup2(newSg, translate)
    conns.foreach { cmatch =>
      val newConn = cmatch.newConn.translateIds(idmap, translate)
      sgBuilder.linkConnectors(cmatch.existingConn, newConn)
    }
    psg
  }

  /** convenience method that tries to autolink any connectors in two PSGs */
  def autoLink(psg1: PastedSectorGroup, psg2: PastedSectorGroup): Int = {
    var count = 0
    psg1.redwallConnectors.foreach { c1 =>
      psg2.redwallConnectors.foreach { c2 =>
        if(sgBuilder.autoLink(c1, c2)){
          count += 1
        }
      }
    }
    count
  }

  // TODO - get rid of this
  def pasteStays(palette: PrefabPalette): Seq[PastedSectorGroup] = {
    val stays = palette.getStaySectorGroups.asScala
    stays.map { sg =>
      builder.pasteSectorGroup(sg, PointXYZ.ZERO) // no translate == leave where it is
    }
  }

  def pasteStays2(palette: PrefabPalette): Seq[(Option[Int], PastedSectorGroup)] = {
    val stays = palette.getStaySectorGroups.asScala
    stays.map(sg =>(sg.props.groupId, builder.pasteSectorGroup(sg, PointXYZ.ZERO)))
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

  //
  //  CONNECTORS
  //

  // See the (static) MapWriter object for the version that works on unpasted SectorGroups
  def linkElevators(
    lowerConn: ElevatorConnector,
    higherConn: ElevatorConnector,
    startLower: Boolean
  ): Unit = ElevatorConnector.linkElevators(
    lowerConn,
    this,
    higherConn,
    this,
    sgBuilder.nextUniqueHiTag(),
    startLower
  )

  //
  //  ISectorGroup Methods
  //  TODO - sgBuilder should implement these, not MapWriter
  //
  override def getMap: DMap = builder.outMap

  override def findSprites(picnum: Int, lotag: Int, sectorId: Int): util.List[Sprite] = builder.findSprites(picnum, lotag, sectorId)

  override def findSprites(filters: ISpriteFilter*): util.List[Sprite] = builder.findSprites(filters:_*)


  //
  // Random
  //
  def randomElement[E](collection: Iterable[E]): E = random.randomElement(collection)

  def randomElementOpt[E](collection: Iterable[E]): Option[E] = {
    if(collection.isEmpty){
      None
    }else{
      Some(random.randomElement(collection))
    }
  }
}
