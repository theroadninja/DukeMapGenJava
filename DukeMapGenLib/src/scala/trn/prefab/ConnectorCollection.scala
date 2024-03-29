package trn.prefab

import trn.duke.{MapErrorException, TextureList}
import trn.{PointXY, PointXYZ, Sprite, Wall, Map => DMap}
import trn.MapImplicits._

import scala.collection.JavaConverters._ // this is the good one

/** first attempt at a standard connector collection interface */
trait ConnectorCollection {
  def connectors: java.util.List[Connector]
  def map: DMap

  final def allConnectorIds(): java.util.Set[Integer] = {
    connectors.asScala.map(_.connectorId).filter(_ > 0).map(_.asInstanceOf[Integer]).toSet.asJava
  }

  final def allRedwallConnectors: Seq[RedwallConnector] = {
    // connectors.asScala.filter(c => ConnectorType.isRedwallType(c.getConnectorType)).map(_.asInstanceOf[RedwallConnector]).toSeq
    connectors.asScala.filter(c => c.isRedwall).map(_.asInstanceOf[RedwallConnector]).toSeq
  }

  final def allConnectors: Seq[Connector] = connectors.asScala

  final def getConnector(connectorId: Int): Connector = {
    if(connectorId < 0) throw new IllegalArgumentException
    connectors.asScala.find(_.connectorId == connectorId) match {
      case Some(conn) => conn
      case None => throw new NoSuchElementException
    }
  }

  final def getRedwallConnectorOpt(connectorId: Int): Option[RedwallConnector] = {
    getRedwallConnectorsById(connectorId).headOption
  }

  final def getRedwallConnectorsById(connectorId: Int): Seq[RedwallConnector] = {
    if(connectorId < 0) throw new IllegalArgumentException
    val c = connectors.asScala.filter(c => c.isRedwall && c.connectorId == connectorId)
    c.map(_.asInstanceOf[RedwallConnector]).toSeq
  }

  final def getElevatorConnectorsById(connectorId: Int): Seq[ElevatorConnector] = {
    if(connectorId < 0) throw new IllegalArgumentException
    connectors.asScala.filter(_.isElevator).map(_.asInstanceOf[ElevatorConnector]).toSeq
  }

  final def getChildPointer(): ChildPointer = {
    val sprites: Seq[Sprite] = map.allSprites.filter(s => s.getTexture == Marker.MARKER_SPRITE_TEX && s.getLotag == Marker.Lotags.REDWALL_CHILD)
    if(sprites.size != 1) throw new SpriteLogicException(s"Wrong number of child marker sprites (${sprites.size})")
    val marker: Sprite = sprites(0)

    val conns = connectors.asScala.filter(c => c.getSectorId == marker.getSectorId && c.isRedwall)
    if(conns.size != 1) throw new SpriteLogicException(s"There must be exactly 1 redwall connector in sector with child marker, but there are ${conns.size}")
    val mainConn = conns(0)
    if(mainConn.connectorId < 1) throw new SpriteLogicException(s"Connector for child pointer must have ID > 0")

    val allConns = connectors.asScala.filter(c => c.connectorId == mainConn.getConnectorId)

    val groupedConns = allConns.groupBy(c => {
      if(c.isRedwall){
        20
      }else if(c.isElevator){
        ConnectorType.ELEVATOR
      }else if(c.isTeleporter && (c.asInstanceOf[TeleportConnector]).isWater){
        ConnectorType.TELEPORTER
      }else{
        throw new SpriteLogicException(s"invalid child connector type in child sector group")
      }
    })
    ChildPointer(
      marker,
      mainConn.connectorId,
      groupedConns.getOrElse(20, Seq()).map(_.asInstanceOf[RedwallConnector]).toSeq,
      //groupedConns.getOrElse(ConnectorType.TELEPORTER, Seq()).map(_.asInstanceOf[TeleportConnector]),
      //groupedConns.getOrElse(ConnectorType.ELEVATOR, Seq()).map(_.asInstanceOf[ElevatorConnector])
    )

    // if(allConns.find(c => !ConnectorType.isRedwallType(c.getConnectorType)).nonEmpty){
    //   throw new SpriteLogicException(s"child sector cannot connect with non redwall connector (id=${mainConn.getConnectorId}")
    // }else{
    //   ChildPointer(marker, mainConn.connectorId, allConns.map(_.asInstanceOf[RedwallConnector]))
    // }
  }
}

