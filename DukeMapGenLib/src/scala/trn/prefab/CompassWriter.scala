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

  def westConnector(sg: ReadOnlySectorGroup): RedwallConnector = sg.getCompassConnectors(Heading.W).head
  def eastConnector(sg: ReadOnlySectorGroup): RedwallConnector = sg.getCompassConnectors(Heading.E).head
  def northConnector(sg: ReadOnlySectorGroup): RedwallConnector = sg.getCompassConnectors(Heading.N).head
  def southConnector(sg: ReadOnlySectorGroup): RedwallConnector = sg.getCompassConnectors(Heading.S).head

  def east(sg: ReadOnlySectorGroup): Option[RedwallConnector] = sg.getCompassConnectors(Heading.E).headOption
  def south(sg: ReadOnlySectorGroup): Option[RedwallConnector] = sg.getCompassConnectors(Heading.S).headOption
  def west(sg: ReadOnlySectorGroup): Option[RedwallConnector] = sg.getCompassConnectors(Heading.W).headOption
  def north(sg: ReadOnlySectorGroup): Option[RedwallConnector] = sg.getCompassConnectors(Heading.N).headOption

  def firstConnWithHeading(sg: SectorGroup, heading: Int) = heading match {
    case Heading.E => east(sg)
    case Heading.W => west(sg)
    case Heading.N => north(sg)
    case Heading.S => south(sg)
    case _ => throw new IllegalArgumentException(s"invalid heading: ${heading}")
  }

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
