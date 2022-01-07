package trn.prefab.experiments

import trn.prefab._
import trn.{HardcodedConfig, Main, MapLoader, MapUtil, PointXY, PointXYZ, Map => DMap}

import scala.collection.JavaConverters._

class PrefabBuilder(val writer: MapWriter, palette: PrefabPalette) {

  def pasteAndLink(
      sectorGroupId: Int,
      paletteConnectorFilter: ConnectorFilter, // this is like SimplePalette.EastConnector ...
      destConnector: Connector): PastedSectorGroup = {

    val sg: SectorGroup = palette.getSectorGroup(sectorGroupId);
    if(destConnector.isLinked(writer.outMap)){
      throw new IllegalArgumentException("connector already connected");
    }
    val paletteConnector = CompassWriter.firstConnector(sg, paletteConnectorFilter)

    writer.pasteAndLink(destConnector.asInstanceOf[RedwallConnector], sg, paletteConnector, Seq.empty)
  }

  def findFirstUnlinkedConnector(cf: ConnectorFilter): Connector = {
    writer.sgBuilder.pastedSectorGroups.foreach { psg =>
    //pastedGroups.asScala.foreach { psg =>
      psg.connectors.asScala.foreach { c: Connector =>
        if(cf.matches(c) && !psg.isConnectorLinked(c)) {
          return c;
        }
      }
    }
    return null;
  }
}
/**
  * This is a scala version of the "copy test 3" java code, which was the first real experiment with prefab sesctor
  * groups.
  *
  * The code was in trn.duke.experiments.prefab.PrefabExperiment.
  */
object FirstPrefabExperiment {

  val Filename: String = "cptest3.map"

  def main(args: Array[String]): Unit = {
    val mapLoader = new MapLoader(HardcodedConfig.DOSPATH)
    val map = run(mapLoader)
    ExpUtil.deployMap(map)
  }
  def run(mapLoader: MapLoader): DMap = {
    val fromMap = mapLoader.load(Filename)

    val palette: PrefabPalette = PrefabPalette.fromMap(fromMap);
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile, HardcodedConfig.getAtomicHeightsFile)
    val writer = MapWriter(gameCfg)
    val builder = new PrefabBuilder(writer, palette)

    val psg1: PastedSectorGroup = builder.writer.pasteSectorGroupAt(palette.getSG(10), new PointXYZ(-1024*30, -1024*50, 0))

    val psg2: PastedSectorGroup = {
      val conn2:Connector = psg1.findFirstConnector(RedConnUtil.WestConnector);
      builder.pasteAndLink(12, RedConnUtil.EastConnector, conn2);
    }

    // add a third group!
    val psg3: PastedSectorGroup  = builder.pasteAndLink(10, RedConnUtil.EastConnector, psg2.findFirstConnector(RedConnUtil.WestConnector));

    // add exit
    {
      val c: Connector = builder.findFirstUnlinkedConnector(RedConnUtil.EastConnector);
      if(c == null) throw new RuntimeException("some thing went wrong")
      builder.pasteAndLink(14, RedConnUtil.WestConnector, c);
    }

    // now try to add the player start group - 11
    {
      val leftEdge: Connector = psg3.findFirstConnector(RedConnUtil.WestConnector);
      builder.pasteAndLink(11, RedConnUtil.EastConnector, leftEdge);
    }

    //try adding a group(s) to the north of psg3
    {
      val north: Connector = psg3.findFirstConnector(RedConnUtil.NorthConnector)
      val sgNorth: PastedSectorGroup = builder.pasteAndLink(10, RedConnUtil.SouthConnector, north);

      val north2 = sgNorth.findFirstConnector(RedConnUtil.NorthConnector);
      val sgNorth2 = builder.pasteAndLink(13, RedConnUtil.SouthConnector, north2);
    }

    //some random groups to the south of something

    builder.pasteAndLink(10, RedConnUtil.NorthConnector, builder.findFirstUnlinkedConnector(RedConnUtil.SouthConnector));

    //try sector group 15
    builder.pasteAndLink(15, RedConnUtil.NorthConnector, builder.findFirstUnlinkedConnector(RedConnUtil.SouthConnector));

    //try sector group 18 - teleporter
    builder.pasteAndLink(18, RedConnUtil.NorthConnector, builder.findFirstUnlinkedConnector(RedConnUtil.SouthConnector));

    builder.writer.setAnyPlayerStart()
    //builder.clearMarkers()
    builder.writer.outMap
  }
}
