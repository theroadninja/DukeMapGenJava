package trn.prefab.experiments

import trn.prefab._
import trn.{MapLoader, PointXY, PointXYZ, Wall, Map => DMap}
import scala.collection.JavaConverters._ // this is the good one

class SushiBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {

  // TODO - this is copied from Hypercube2
  val sgPacker: SectorGroupPacker = new SimpleSectorGroupPacker(
    new PointXY(DMap.MIN_X, 0),
    new PointXY(DMap.MAX_X, DMap.MAX_Y),
    512)

  // TODO - this is copied from Hypercube2
  def placeAnywhere(sg: SectorGroup): PastedSectorGroup = {
    val topLeft = sgPacker.reserveArea(sg)
    val tr = sg.boundingBox.getTranslateTo(topLeft).withZ(0)
    pasteSectorGroup(sg, tr)
  }

  // TODO - copied from PipeDream
  def pasteAndLink(
    existingConn: RedwallConnector,
    newSg: SectorGroup,
    newConn: RedwallConnector
  ): PastedSectorGroup = {
    require(Option(existingConn).isDefined)
    require(Option(newSg).isDefined)
    require(Option(newConn).isDefined)
    val cdelta = newConn.getTransformTo(existingConn)
    val (psg, idmap) = pasteSectorGroup2(newSg, cdelta)
    val pastedConn2 = newConn.translateIds(idmap, cdelta)
    existingConn.linkConnectors(outMap, pastedConn2)
    psg
  }

  def pasteAndLinkSouthOf(
    existingGroup: PastedSectorGroup,
    newGroup: SectorGroup
  ): PastedSectorGroup = {
    val conn1 = existingGroup.findFirstConnector(SimpleConnector.SouthConnector).asInstanceOf[RedwallConnector]
    pasteAndLink(conn1, newGroup, newGroup.findFirstConnector(SimpleConnector.NorthConnector).asInstanceOf[RedwallConnector])
  }

  def pasteAndLinkWestOf(
    existingGroup: PastedSectorGroup,
    newGroup: SectorGroup
  ): PastedSectorGroup = {
    val conn1 = existingGroup.findFirstConnector(SimpleConnector.WestConnector).asInstanceOf[RedwallConnector]
    pasteAndLink(conn1, newGroup, newGroup.findFirstConnector(SimpleConnector.EastConnector).asInstanceOf[RedwallConnector])
  }

  private def pasteAndLinkNextTo(
    existingGroup: PastedSectorGroup,
    existingConn: ConnectorFilter,
    newGroup: SectorGroup,
    newConn: ConnectorFilter
  ): PastedSectorGroup = {
    val conn1 = existingGroup.findFirstConnector(existingConn).asInstanceOf[RedwallConnector]
    pasteAndLink(conn1, newGroup, newGroup.findFirstConnector(newConn).asInstanceOf[RedwallConnector])
  }

  def pasteEastOf(
    existing: PastedSectorGroup,
    newGroup: SectorGroup
  ): PastedSectorGroup = pasteAndLinkNextTo(existing, SimpleConnector.EastConnector, newGroup, SimpleConnector.WestConnector)

  def pasteWestOf(
    existing: PastedSectorGroup,
    newGroup: SectorGroup
  ): PastedSectorGroup = pasteAndLinkNextTo(existing, SimpleConnector.WestConnector, newGroup, SimpleConnector.EastConnector)

  def pasteNorthOf(
    existing: PastedSectorGroup,
    newGroup: SectorGroup
  ): PastedSectorGroup = pasteAndLinkNextTo(existing, SimpleConnector.NorthConnector, newGroup, SimpleConnector.SouthConnector)

  // TODO - finish this
  // compare to Hypercube2 autoLinkRooms
  def autoLink(): Unit = {
    val unlinked = pastedSectorGroups.flatMap(psg => psg.unlinkedRedwallConnectors)
    unlinked.foreach(c => require(!c.isLinked(outMap)))
    unlinked.foreach { x =>
      unlinked.foreach { y =>
        if (x.isFullMatch(y, outMap)) {
          x.linkConnectors(outMap, y)
        }
      }
    }
  }
}

object Sushi {
  val FILENAME = "sushi.map"
  def run(mapLoader: MapLoader): DMap = {
    val sourceMap = mapLoader.load(FILENAME)
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new SushiBuilder(DMap.createNew(), palette)

    run2(builder, palette)
    println(s"Sector count: ${builder.outMap.getSectorCount}")
    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }

  def run2(builder: SushiBuilder, palette: PrefabPalette): Unit = {

    val PlainHall = 1
    val Entrance = 4
    val Corner = 3
    val BarEntrance = 5
    val BigRestaurant = 6
    val CornerCashier = 7

    //val sg = palette.getRandomSectorGroup()
    val sg = palette.getSectorGroup(1)
    require(null != sg.findFirstConnector(SimpleConnector.NorthConnector))

    require(Option(palette.getSG(BarEntrance).findFirstConnector(SimpleConnector.NorthConnector)).isDefined)
    require(Option(palette.getSG(BarEntrance).findFirstConnector(SimpleConnector.SouthConnector)).isDefined)
    require(Option(palette.getSG(BigRestaurant).findFirstConnector(SimpleConnector.SouthConnector)).isDefined)

    val entrance = builder.pasteSectorGroupAt(palette.getSectorGroup(Entrance).rotateCW.rotate180, PointXYZ.ZERO)

    //val corner = builder.pasteSectorGroupAt(palette.getSectorGroup(3), PointXYZ.ZERO)
    //val psg = builder.placeAnywhere(sg)
    val corner = builder.pasteEastOf(entrance, palette.getSectorGroup(Corner))

    val psg = builder.pasteAndLinkSouthOf(corner, palette.getSectorGroup(1))
    val psg2 = builder.pasteAndLinkSouthOf(psg, palette.getSectorGroup(2))
    val psg3 = builder.pasteAndLinkSouthOf(psg2, palette.getSectorGroup(2))

    val corner2 = builder.pasteAndLinkSouthOf(psg3, palette.getSectorGroup(3).rotateCW)
    val horizontalHall = palette.getSectorGroup(2).rotateCW
    val hall4 = builder.pasteAndLinkWestOf(corner2, horizontalHall)

    val barEntrance = builder.pasteAndLinkWestOf(hall4, palette.getSG(BarEntrance).rotateCW)
    val corner3 = builder.pasteWestOf(barEntrance, palette.getSG(Corner).rotate180)
    val bigRestaurant = builder.pasteNorthOf(corner3, palette.getSG(BigRestaurant))

    val cashier = builder.pasteNorthOf(bigRestaurant, palette.getSG(CornerCashier).rotateCCW)

    val cashierHall = builder.pasteEastOf(cashier, palette.getSG(PlainHall).rotateCW)

    // TODO - need to be able to auto connect connectors that are in the same place...
    builder.autoLink

  }

}
