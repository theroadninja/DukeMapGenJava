package trn.prefab.experiments

import trn.prefab._
import trn.{MapLoader, PointXY, PointXYZ, Wall, Map => DMap}

class RefBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {

}
/**
  * This assembled a map used for small experiments and regression testing.
  */
object ReferenceTestExperiment {
  val FILENAME = "ref.map"


  //def linkById(psg1: PastedSectorGroup, psg2: PastedSectorGroup, )
  // def run(sourceMap: DMap): DMap = {
  def run(mapLoader: MapLoader): DMap = {
    val sourceMap = mapLoader.load(FILENAME)

    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new RefBuilder(DMap.createNew(), palette)




    val pasted1 = builder.pasteSectorGroupAt(palette.getSectorGroup(1), new PointXYZ(0, 0, 0))
    val conn1: RedwallConnector = pasted1.connectors.get(0).asInstanceOf[RedwallConnector]
    //val conn1 = palette.getSectorGroup(1).allRedwallConnectors.head

    val sg2: SectorGroup = palette.getSectorGroup(2)//.rotateCW

    val conn2: RedwallConnector = sg2.getRedwallConnectorsById(123).head
    val delta: PointXYZ = conn2.getTransformTo(conn1)
    val pasted2 = builder.pasteSectorGroup(palette.getSectorGroup(2), delta)

    conn1.linkConnectors(builder.outMap, pasted2.getRedwallConnectorsById(123).get(0))


    val sg3 = palette.getSectorGroup(3)
    val conn3 = sg3.getRedwallConnectorsById(124).head.asInstanceOf[SimpleConnector]
    val delta3 = conn3.getTransformTo(pasted2.getConnector(124).asInstanceOf[SimpleConnector])
    val pasted3 = builder.pasteSectorGroup(sg3, delta3)

    //val conn2pasted = pasted2.getRedwallConnectorsById(124).get(0)
    val conn2pasted = pasted2.getConnector(124).asInstanceOf[RedwallConnector]
    if(conn2pasted.isLinked(builder.outMap)) throw new RuntimeException
    conn2pasted.linkConnectors(builder.outMap, pasted3.getConnector(124).asInstanceOf[RedwallConnector])


    println(s"Sector count: ${builder.outMap.getSectorCount}")
    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }

}
