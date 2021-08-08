package trn.prefab

import trn.{WallView, Map => DMap}
import trn.FuncImplicits._
import scala.collection.JavaConverters._

/**
  * Provides methods for dealing with axis-aligned redwall connectors that are at the edges of sector groups, named
  * after compass directions, e.g. north, east, south, west.  May be useful for grids and hypercubes.
  *
  * TODO see also RedConnUtil.connectorTypeForWall
  */
object CompassWriter {
  val WestConn = RedConnUtil.WestConnector
  val EastConn = RedConnUtil.EastConnector
  val NorthConn = RedConnUtil.NorthConnector
  val SouthConn = RedConnUtil.SouthConnector

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

  def firstConnWithHeading(sg: SectorGroup, heading: Int) = heading match {
    case Heading.E => east(sg)
    case Heading.W => west(sg)
    case Heading.N => north(sg)
    case Heading.S => south(sg)
    case _ => throw new IllegalArgumentException(s"invalid heading: ${heading}")
  }

  // TODO - should not need different methods for SectorGroup and PastedSectorGroup
  def westConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(WestConn).asInstanceOf[RedwallConnector]
  def eastConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(EastConn).asInstanceOf[RedwallConnector]
  def northConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(NorthConn).asInstanceOf[RedwallConnector]
  def southConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(SouthConn).asInstanceOf[RedwallConnector]

  def east(sg: PastedSectorGroup): Option[RedwallConnector] = sg.connectorCollection.findFirstRedwallConn(EastConn)
  def west(sg: PastedSectorGroup): Option[RedwallConnector] = sg.connectorCollection.findFirstRedwallConn(WestConn)
  def north(sg: PastedSectorGroup): Option[RedwallConnector] = sg.connectorCollection.findFirstRedwallConn(NorthConn)
  def south(sg: PastedSectorGroup): Option[RedwallConnector] = sg.connectorCollection.findFirstRedwallConn(SouthConn)
  def firstConnWithHeading(sg: PastedSectorGroup, heading: Int): Option[RedwallConnector] = heading match {
    case Heading.E => east(sg)
    case Heading.W => west(sg)
    case Heading.N => north(sg)
    case Heading.S => south(sg)
    case _ => throw new IllegalArgumentException(s"invalid heading: ${heading}")
  }

  def farthestEast(conns: Seq[RedwallConnector]): Option[RedwallConnector] = conns.filter(_.isEast).maxByOption(_.getAnchorPoint.x)
  def farthestWest(conns: Seq[RedwallConnector]): Option[RedwallConnector] = conns.filter(_.isWest).maxByOption(_.getAnchorPoint.x * -1)
  def farthestNorth(conns: Seq[RedwallConnector]): Option[RedwallConnector] = conns.filter(_.isNorth).maxByOption(_.getAnchorPoint.y * -1)
  def farthestSouth(conns: Seq[RedwallConnector]): Option[RedwallConnector] = conns.filter(_.isSouth).maxByOption(_.getAnchorPoint.y)
  def farthestConn(conns: Seq[RedwallConnector], heading: Int): Option[RedwallConnector] = heading match {
    case Heading.E => farthestEast(conns)
    case Heading.W => farthestWest(conns)
    case Heading.N => farthestNorth(conns)
    case Heading.S => farthestSouth(conns)
    case _ => throw new IllegalArgumentException(s"invalid heading: ${heading}")
  }

  private def compassWalls(map: DMap, sectorId: Int, heading: Int): Seq[WallView] = {
    map.getWallViews(map.getAllSectorWallIds(sectorId)).asScala.filter(w => w.compassWallSide() == heading)
  }
  def eastWalls(map: DMap, sectorId: Int): Seq[WallView] = compassWalls(map, sectorId, Heading.E)
  def southWalls(map: DMap, sectorId: Int): Seq[WallView] = compassWalls(map, sectorId, Heading.S)
  def westWalls(map: DMap, sectorId: Int): Seq[WallView] = compassWalls(map, sectorId, Heading.W)
  def northWalls(map: DMap, sectorId: Int): Seq[WallView] = compassWalls(map, sectorId, Heading.N)

}
