package trn.prefab

import scala.collection.JavaConverters._ // this is the good one

/**
  * Version of ConnectorCollection for the PastedSectorGroup
  * creating this class temporarily to make the duplication more obvious
  *
  * TODO - PastedConnectorCollection and ConnectorCollection should be the same object
  */
class PastedConnectorCollection(
  val connectors: java.util.List[Connector] = new java.util.ArrayList[Connector]()
) {

  final def getConnector(connectorId: Int): Connector = {
    if(connectorId < 0) throw new IllegalArgumentException
    connectors.asScala.find(_.connectorId == connectorId) match {
      case Some(conn) => conn
      case None => throw new NoSuchElementException
    }
  }

  def findFirstConnector(cf: ConnectorFilter): Option[Connector] = {
    val it: java.util.Iterator[Connector] = Connector.findConnectors(connectors, cf).iterator
    //Iterator<Connector> it = Connector.findConnectors(this.connectors_(), cf).iterator();
    //return it.hasNext() ? it.next() : null;
    if(it.hasNext){
      Some(it.next)
    }else{
      None
    }
  }
  def findFirstRedwallConn(cf: ConnectorFilter): Option[RedwallConnector] = findFirstConnector(cf).map(_.asInstanceOf[RedwallConnector])

  def findConnectorsByType(connectorType: Int): java.util.List[Connector] = {
    connectors.asScala.filter(_.getConnectorType == connectorType).asJava
  }

  final def getElevatorConn(connectorId: Int): Option[ElevatorConnector] = {
    if(connectorId < 0) throw new IllegalArgumentException
    try{
      val x = connectors.asScala.filter(c => c.connectorId == connectorId && c.getConnectorType == ConnectorType.ELEVATOR).map(_.asInstanceOf[ElevatorConnector])
      Some(x.head)
    }catch{
      case _ => None
    }
  }

  def getRedwallConnectorsById(connectorId: Int): java.util.List[RedwallConnector] = {
    connectors.asScala.filter{ c =>
      c.getConnectorId == connectorId && ConnectorType.isRedwallType(c.getConnectorType)
    }.map(_.asInstanceOf[RedwallConnector]).asJava
  }

  def hasConnector(connectorId: Int): Boolean = {
    require(connectorId >= 0)
    val x = connectors.asScala.find(_.connectorId == connectorId)
    x.map(_ => true).getOrElse(false)
    // for(Connector c: connectors_()){
    //   if(c.getConnectorId() == connectorId){
    //     return true;
    //   }
    // }
    // return false;
  }

  def getFirstElevatorConnector: Option[ElevatorConnector] = {
    connectors.asScala.find(_.getConnectorType == ConnectorType.ELEVATOR).map(_.asInstanceOf[ElevatorConnector])
  }

}
