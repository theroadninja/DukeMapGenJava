package trn.prefab

import trn.duke.{MapErrorException, TextureList}
import trn.{DukeConstants, ISpriteFilter, MapUtil, PointXY, PointXYZ, Sprite, Wall, Map => DMap}
import trn.MapImplicits._

import scala.collection.JavaConverters._ // this is the good one


object PastedSectorGroup {

  def apply(map: DMap, copystate: MapUtil.CopyState, groupId: Option[Int]): PastedSectorGroup = {
    new PastedSectorGroup(map, copystate, ConnectorFactory.findConnectorsInPsg(map, copystate), groupId)
  }

}

class PastedSectorGroup private (
  val map: DMap,
  copystate: MapUtil.CopyState,
  val connectors: java.util.List[Connector],
  val groupId: Option[Int] // id of the sector group this was copied from
)
  extends SectorGroupBase
  with ISectorGroup
{
  val destSectorIds = copystate.destSectorIds

  // val connectors: java.util.List[Connector] = new java.util.ArrayList[Connector]();
  val connectorCollection = new PastedConnectorCollection(connectors)

  val sectorIds: Set[Int] = destSectorIds.asScala.map(_.toInt).toSet

  final def getCopyState: MapUtil.CopyState = copystate
  override def getMap: DMap = map
  override def findSprites(picnum: Int, lotag: Int, sectorId: Int): java.util.List[Sprite] = {
    getMap().findSprites(picnum, lotag, sectorId)
  }
  override def findSprites(filters: ISpriteFilter*): java.util.List[Sprite] = getMap().findSprites4Scala(filters.asJava)

  def allSprites: Seq[Sprite] = getMap().allSprites

  final def getConnector(connectorId: Int): Connector = connectorCollection.getConnector(connectorId)
  //   if(connectorId < 0) throw new IllegalArgumentException
  //   connectors.asScala.find(_.connectorId == connectorId) match {
  //     case Some(conn) => conn
  //     case None => throw new NoSuchElementException
  //   }
  // }

  final def hasConnector(connectorId: Int): Boolean = connectorCollection.hasConnector(connectorId)

  def getFirstElevatorConnector: ElevatorConnector = {
    connectorCollection.getFirstElevatorConnector.getOrElse(throw new NoSuchElementException)
  }

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

  protected override def allSectorIds: Set[Int] = sectorIds

}
