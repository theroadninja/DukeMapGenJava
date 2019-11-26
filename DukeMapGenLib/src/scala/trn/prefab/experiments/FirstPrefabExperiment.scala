package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapLoader, MapUtil, PointXY, PointXYZ, Map => DMap}
import scala.collection.JavaConverters._

class PrefabBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {

  val pastedGroups: java.util.List[PastedSectorGroup]  = new java.util.ArrayList[PastedSectorGroup]()
  val writer = MapWriter(this)

  // this does not need to be more generic
  def pasteSectorGroup(sgId: Int, translate: PointXYZ): PastedSectorGroup = {
    val psg = pasteSectorGroup(palette.getSectorGroup(sgId), translate)
    add(psg)
  }

  private def add(psg: PastedSectorGroup ): PastedSectorGroup = {
    pastedGroups.add(psg)
    psg
  }

  def pasteAndLink(
      sectorGroupId: Int,
      paletteConnectorFilter: ConnectorFilter, // this is like SimplePalette.EastConnector ...
      destConnector: Connector): PastedSectorGroup = {

    val sg: SectorGroup = palette.getSectorGroup(sectorGroupId);
    if(destConnector.isLinked(outMap)){
      throw new IllegalArgumentException("connector already connected");
    }
    // val paletteConnector = sg.findFirstConnector(paletteConnectorFilter).asInstanceOf[RedwallConnector]
    val paletteConnector = MapWriter.firstConnector(sg, paletteConnectorFilter)
    return add(writer.pasteAndLink(destConnector.asInstanceOf[RedwallConnector], sg, paletteConnector))
  }

  def findFirstUnlinkedConnector(cf: ConnectorFilter): Connector = {
    pastedGroups.asScala.foreach { psg =>
      psg.connectors.asScala.foreach { c: Connector =>
        if(cf.matches(c) && !psg.isConnectorLinked(c)) {
          return c;
        }
      }
    }
    return null;
  }

  def unlinkedConnectors: Seq[Connector] = {
    pastedGroups.asScala.flatMap { psg =>
      psg.connectors.asScala.filter { c =>
        !psg.isConnectorLinked(c)
      }
    }
  }

}
/**
  * This is a scala version of the "copy test 3" java code, which was the first real experiment with prefab sesctor
  * groups.
  *
  * The code was in trn.duke.experiments.prefab.PrefabExperiment.
  */
object FirstPrefabExperiment {

  def run(fromMap: DMap): DMap = {
    val palette: PrefabPalette = PrefabPalette.fromMap(fromMap);
    val builder = new PrefabBuilder(DMap.createNew(), palette)




    val psg1:PastedSectorGroup  = builder.pasteSectorGroup(10, new PointXYZ(-1024*30, -1024*50, 0));
    val psg2: PastedSectorGroup = {
      //SimpleConnector conn2 = psg1.getConnector(123);
      //psg2 = mb.pasteAndLink(12, conn2);
      val conn2:Connector = psg1.findFirstConnector(SimpleConnector.WestConnector);
      builder.pasteAndLink(12, SimpleConnector.EastConnector, conn2);
    }

    // add a third group!
    val psg3: PastedSectorGroup  = builder.pasteAndLink(10, SimpleConnector.EastConnector, psg2.findFirstConnector(SimpleConnector.WestConnector));

    // add exit
    {
      //SimpleConnector c = psg3.findFirstConnector(SimpleConnector.EastConnector);
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

    builder.setPlayerStart()
    //builder.clearMarkers()
    builder.outMap
  }
}
