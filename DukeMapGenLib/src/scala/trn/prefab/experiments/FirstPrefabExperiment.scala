package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapLoader, MapUtil, PointXY, PointXYZ, Map => DMap}
import scala.collection.JavaConverters._

class PrefabBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {
  val writer = MapWriter(this)

  def pasteAndLink(
      sectorGroupId: Int,
      paletteConnectorFilter: ConnectorFilter, // this is like SimplePalette.EastConnector ...
      destConnector: Connector): PastedSectorGroup = {

    val sg: SectorGroup = palette.getSectorGroup(sectorGroupId);
    if(destConnector.isLinked(outMap)){
      throw new IllegalArgumentException("connector already connected");
    }
    val paletteConnector = CompassWriter.firstConnector(sg, paletteConnectorFilter)

    writer.pasteAndLink(destConnector.asInstanceOf[RedwallConnector], sg, paletteConnector)
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
object FirstPrefabExperiment extends PrefabExperiment {

  override val Filename: String = "cptest3.map"

  override def run(mapLoader: MapLoader): DMap = {
    val fromMap = mapLoader.load(Filename)

    val palette: PrefabPalette = PrefabPalette.fromMap(fromMap);
    val builder = new PrefabBuilder(DMap.createNew(), palette)

    val psg1: PastedSectorGroup = builder.writer.pasteSectorGroupAt(palette.getSG(10), new PointXYZ(-1024*30, -1024*50, 0))

    val psg2: PastedSectorGroup = {
      val conn2:Connector = psg1.findFirstConnector(SimpleConnector.WestConnector);
      builder.pasteAndLink(12, SimpleConnector.EastConnector, conn2);
    }

    // add a third group!
    val psg3: PastedSectorGroup  = builder.pasteAndLink(10, SimpleConnector.EastConnector, psg2.findFirstConnector(SimpleConnector.WestConnector));

    // add exit
    {
      val c: Connector = builder.findFirstUnlinkedConnector(SimpleConnector.EastConnector);
      if(c == null) throw new RuntimeException("some thing went wrong")
      builder.pasteAndLink(14, SimpleConnector.WestConnector, c);
    }

    // now try to add the player start group - 11
    {
      val leftEdge: Connector = psg3.findFirstConnector(SimpleConnector.WestConnector);
      builder.pasteAndLink(11, SimpleConnector.EastConnector, leftEdge);
    }

    //try adding a group(s) to the north of psg3
    {
      val north: Connector = psg3.findFirstConnector(SimpleConnector.NorthConnector)
      val sgNorth: PastedSectorGroup = builder.pasteAndLink(10, SimpleConnector.SouthConnector, north);

      val north2 = sgNorth.findFirstConnector(SimpleConnector.NorthConnector);
      val sgNorth2 = builder.pasteAndLink(13, SimpleConnector.SouthConnector, north2);
    }

    //some random groups to the south of something

    builder.pasteAndLink(10, SimpleConnector.NorthConnector, builder.findFirstUnlinkedConnector(SimpleConnector.SouthConnector));

    //try sector group 15
    builder.pasteAndLink(15, SimpleConnector.NorthConnector, builder.findFirstUnlinkedConnector(SimpleConnector.SouthConnector));

    //try sector group 18 - teleporter
    builder.pasteAndLink(18, SimpleConnector.NorthConnector, builder.findFirstUnlinkedConnector(SimpleConnector.SouthConnector));

    builder.writer.setAnyPlayerStart()
    //builder.clearMarkers()
    builder.outMap
  }
}
