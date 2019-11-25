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

  final def getElevatorConn(connectorId: Int): Option[ElevatorConnector] = {
    if(connectorId < 0) throw new IllegalArgumentException
    try{
      val x = connectors.asScala.filter(c => c.connectorId == connectorId && c.getConnectorType == ConnectorType.ELEVATOR).map(_.asInstanceOf[ElevatorConnector])
      Some(x.head)
    }catch{
      case _ => None
    }
  }

}
