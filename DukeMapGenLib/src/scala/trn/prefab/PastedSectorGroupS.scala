package trn.prefab

import trn.duke.{MapErrorException, TextureList}
import trn.{DukeConstants, ISpriteFilter, PointXY, PointXYZ, Sprite, Wall, Map => DMap}
import trn.MapImplicits._

import scala.collection.JavaConverters._ // this is the good one

class PastedSectorGroupS(val map: DMap, destSectorIds: java.util.Set[java.lang.Short]) extends SectorGroupBase {
  val connectors: java.util.List[Connector] = new java.util.ArrayList[Connector]();
  val sectorIds: Set[Int] = destSectorIds.asScala.map(_.toInt).toSet

  final def getConnector(connectorId: Int): Connector = {
    if(connectorId < 0) throw new IllegalArgumentException
    connectors.asScala.find(_.connectorId == connectorId) match {
      case Some(conn) => conn
      case None => throw new NoSuchElementException
    }
  }

  final def isConnectorLinked(c: Connector): Boolean = c.isLinked(map)

  final def unlinkedConnectors: Seq[Connector] = {
    connectors.asScala.filter(c => !isConnectorLinked(c))
  }

  final def redwallConnectors: Seq[RedwallConnector] = {
    connectors.asScala.flatMap { c =>
      c match {
        case rw: RedwallConnector => Some(rw)
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

  protected def wallSeq(): Seq[Wall] = {
    val wallIds = sectorIds.map(map.getSector(_)).flatMap(s => map.getAllSectorWallIds(s).asScala)
    wallIds.map(map.getWall(_)).toSeq
  }

}
