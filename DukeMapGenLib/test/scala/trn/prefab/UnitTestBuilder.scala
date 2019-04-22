package trn.prefab

import trn.prefab.{MapBuilder, PrefabPalette}
import trn.{DukeConstants, Main, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, Map => DMap}
import scala.collection.JavaConverters._

class UnitTestBuilder(val outMap: DMap) extends MapBuilder with AnywhereBuilder {

  val sgPacker: SectorGroupPacker = new SimpleSectorGroupPacker(
    new PointXY(DMap.MIN_X, 0),
    new PointXY(DMap.MAX_X, DMap.MAX_Y),
    512)

  // def joinWalls(c1: Connector, c2: Connector): Unit = {
  //   if(c1 == null || c2 == null) throw new IllegalArgumentException
  //   //c1.asInstanceOf[RedwallConnector].linkConnectors(outMap, c2.asInstanceOf[RedwallConnector])
  //   SimpleConnector.linkConnectors(c1.asInstanceOf[RedwallConnector].linkConnectors(outMap, c2.asInstanceOf[RedwallConnector])
  // }
}
