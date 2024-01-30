package duchy.sg

import trn.prefab.{ConnectorFactory2, Connector, Marker}
import trn.{MapView, CopyState, Map => DMap}
import trn.MapImplicits._

import scala.collection.JavaConverters._

/**
  * TODO there is another ConnectorScanner in trn.prefab.ConnectorScanner.scala
  */
object SimpleConnectorScanner {

  /** lotags that mark a wall as a part of a redwall connector */
  val RedwallLotags = Seq(1) // TODO add 2 and 3

  private def allSectors(sectorId: Int): Boolean = true

  def scanAndFilter(map: MapView, sectorIdFilter: Int => Boolean = allSectors): Seq[Connector] = {
    val redwallConns = RedwallConnectorScanner.findAllRedwallConns(map, sectorIdFilter)

    val markerSprites = map.allSprites.filter(s => s.getTex == Marker.MARKER_SPRITE_TEX)
    val connectors: Seq[Connector] = markerSprites.flatMap { marker =>
      Option(ConnectorFactory2.createOther(map, marker))
    }.filter(s => sectorIdFilter(s.getSectorId))
    // val multiSectorResults = ConnectorScanner.findMultiSectorConnectors(map).asScala

    redwallConns ++ connectors
  }

  def scan(map: MapView): Seq[Connector] = scanAndFilter(map)

  /**
    * for backwards compatibility;  currently the existing callers expect a mutable java list :/
    */
  def scanAsJava(map: MapView): java.util.List[Connector] = {
    val javaList: java.util.List[Connector] = new java.util.LinkedList[Connector]()
    scan(map).foreach(conn => javaList.add(conn))
    javaList
  }

  def scanPsgAsJava(map: DMap, copystate: CopyState): java.util.List[Connector] = {
    val conns = scanAndFilter(map.asView, (sectorId) => copystate.destSectorIds().contains(sectorId.toShort))

    val javaList: java.util.List[Connector] = new java.util.LinkedList[Connector]()
    conns.foreach(conn => javaList.add(conn))
    javaList
  }

  def getLinkWallIds(map: MapView, sectorId: Int): Seq[Int] = {
    map.getAllSectorWallIdsBySectorId(sectorId).filter { wallId =>
      RedwallLotags.contains(map.getWall(wallId).getLotag)
    }
  }

  def getLinkWallIdsJava(map: MapView, sectorId: Int): java.util.List[Integer] = getLinkWallIds(map, sectorId).map { id =>
    val idJava: Integer = id
    idJava
  }.asJava


}
