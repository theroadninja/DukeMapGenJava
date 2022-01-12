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
    connectors.asScala.filter(c => ConnectorType.isRedwallType(c.getConnectorType)).map(_.asInstanceOf[RedwallConnector]).toSeq
  }

  final def getConnector(connectorId: Int): Connector = {
    if(connectorId < 0) throw new IllegalArgumentException
    connectors.asScala.find(_.connectorId == connectorId) match {
      case Some(conn) => conn
      case None => throw new NoSuchElementException
    }
  }

  // TODO - copied from PastedConnectorCollection.findFirstConnector()
  @Deprecated
  def findFirstConnectorOpt(cf: ConnectorFilter): Option[Connector] = {
    val it: java.util.Iterator[Connector] = ConnectorFactory.findConnectors(connectors, cf).iterator // TODO - does the caller need us to rescan every time???
    //Iterator<Connector> it = Connector.findConnectors(this.connectors_(), cf).iterator();
    //return it.hasNext() ? it.next() : null;
    if(it.hasNext){
      Some(it.next)
    }else{
      None
    }
  }

  final def getRedwallConnectorsById(connectorId: Int): Seq[RedwallConnector] = {
    if(connectorId < 0) throw new IllegalArgumentException
    val c = connectors.asScala.filter(c => ConnectorType.isRedwallType(c.getConnectorType) && c.connectorId == connectorId)
    c.map(_.asInstanceOf[RedwallConnector]).toSeq
  }

  final def getElevatorConnectorsById(connectorId: Int): Seq[ElevatorConnector] = {
    if(connectorId < 0) throw new IllegalArgumentException
    connectors.asScala.filter(_.getConnectorType == ConnectorType.ELEVATOR).map(_.asInstanceOf[ElevatorConnector]).toSeq
  }

  final def getChildPointer(): ChildPointer = {
    val sprites: Seq[Sprite] = map.allSprites.filter(s => s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == PrefabUtils.MarkerSpriteLoTags.REDWALL_CHILD)
    if(sprites.size != 1) throw new SpriteLogicException(s"Wrong number of child marker sprites (${sprites.size})")
    val marker: Sprite = sprites(0)

    val conns = connectors.asScala.filter(c => c.getSectorId == marker.getSectorId && ConnectorType.isRedwallType(c.getConnectorType))
    if(conns.size != 1) throw new SpriteLogicException(s"There must be exactly 1 redwall connector in sector with child marker, but there are ${conns.size}")
    val mainConn = conns(0)
    if(mainConn.connectorId < 1) throw new SpriteLogicException(s"Connector for child pointer must have ID > 0")

    val allConns = connectors.asScala.filter(c => c.connectorId == mainConn.getConnectorId)

    val groupedConns = allConns.groupBy(c => {
      if(ConnectorType.isRedwallType(c.getConnectorType)){
        20
      }else if(c.getConnectorType == ConnectorType.ELEVATOR){
        ConnectorType.ELEVATOR
      }else if(c.getConnectorType == ConnectorType.TELEPORTER && (c.asInstanceOf[TeleportConnector]).isWater){
        ConnectorType.TELEPORTER
      }else{
        throw new SpriteLogicException(s"invalid child connector (type ${c.getConnectorType}) in child sector group")
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

