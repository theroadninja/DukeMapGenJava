package trn.prefab

import trn.{CopyState, ISpriteFilter, Sector, Sprite, Wall, WallView, Map => DMap}
import trn.MapImplicits._

import scala.collection.JavaConverters._ // this is the good one


object PastedSectorGroup {

  def apply(map: DMap, copystate: CopyState, groupId: Option[Int]): PastedSectorGroup = {
    new PastedSectorGroup(map, copystate, ConnectorFactory.findConnectorsInPsg(map, copystate), groupId)
  }

}

class PastedSectorGroup private (
  val map: DMap,
  copystate: CopyState,
  val connectors: java.util.List[Connector],
  val groupId: Option[Int] // id of the sector group this was copied from
)
  extends SectorGroupBase
  with ISectorGroup
  with ReadOnlySectorGroup
{
  val destSectorIds = copystate.destSectorIds

  // val connectors: java.util.List[Connector] = new java.util.ArrayList[Connector]();
  val connectorCollection = new PastedConnectorCollection(connectors)

  val sectorIds: Set[Int] = destSectorIds.asScala.map(_.toInt).toSet

  final def getCopyState: CopyState = copystate
  override def getMap: DMap = map
  override def findSprites(picnum: Int, lotag: Int, sectorId: Int): java.util.List[Sprite] = {
    getMap().findSprites(picnum, lotag, sectorId)
  }
  // override def findSprites(filters: ISpriteFilter*): java.util.List[Sprite] = getMap().findSprites4Scala(filters.asJava)

  def allSprites: Seq[Sprite] = getMap().allSprites

  override def getWallView(wallId: Int): WallView = map.getWallView(wallId)

  override def getSector(sectorId: Int): Sector = map.getSector(sectorId)

  final def getConnector(connectorId: Int): Connector = connectorCollection.getConnector(connectorId)

  override def getRedwallConnector(connectorId: Int): RedwallConnector = {
    getConnector(connectorId).asInstanceOf[RedwallConnector]
  }

  override def getCompassConnectors(heading: Int): Seq[RedwallConnector] = {
    redwallConnectors.filter(_.heading == heading)
    // connectorCollection.findConnectorsByType(ConnectorType.fromHeading(heading)).asScala.map(_.asInstanceOf[RedwallConnector])
  }

  final def hasConnector(connectorId: Int): Boolean = connectorCollection.hasConnector(connectorId)

  def getFirstElevatorConnector: ElevatorConnector = {
    connectorCollection.getFirstElevatorConnector.getOrElse(throw new NoSuchElementException)
  }

  @Deprecated
  def findFirstConnector(cf: ConnectorFilter): Connector = connectorCollection.findFirstConnector(cf).orNull

  def findConnectorsByType(ctype: Int): java.util.List[Connector] = connectorCollection.findConnectorsByType(ctype)

  def getRedwallConnectorsById(connectorId: Int): java.util.List[RedwallConnector] = connectorCollection.getRedwallConnectorsById(connectorId)

  final def getElevatorConn(connId: Int): Option[ElevatorConnector] = connectorCollection.getElevatorConn(connId)

  final def getTeleportConnector(connectorId: Int): TeleportConnector = {
    getConnector(connectorId).asInstanceOf[TeleportConnector]
  }

  final def isConnectorLinked(c: Connector): Boolean = c.isLinked(map)

  final def unlinkedConnectors: Seq[Connector] = {
    connectors.asScala.filter(c => !isConnectorLinked(c)).toSeq
  }

  final def redwallConnectors: Seq[RedwallConnector] = {
    connectors.asScala.flatMap { c =>
      c match {
        case rw: RedwallConnector => Some(rw)
        case _ => None
      }
    }.toSeq
  }

  final def allTeleportConnectors: Seq[TeleportConnector] = {
    connectors.asScala.flatMap { c =>
      c match {
        case t: TeleportConnector => Some(t)
        case _ => None
      }
    }
  }

  final def allElevatorConnectors: Seq[ElevatorConnector] = {
    connectors.asScala.flatMap { c =>
      c match {
        case t: ElevatorConnector => Some(t)
        case _ => None
      }
    }
  }


  final def unlinkedRedwallConnectors: Seq[RedwallConnector] = {
    unlinkedConnectors.flatMap { c =>
      c match {
        case rw: RedwallConnector => {
          require(!rw.isLinked(map))
          Some(rw)
        }
        case _ => None
      }
    }
  }

  protected override def wallSeq(): Seq[Wall] = {
    val wallIds = sectorIds.map(map.getSector(_)).flatMap(s => map.getAllSectorWallIds(s).asScala)
    wallIds.map(map.getWall(_)).toSeq
  }

  /**
    * this should return only the sectors "managed" by this object
    * @return
    */
  override def allSectorIds: Set[Int] = sectorIds

}
