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

  // def run(sourceMap: DMap): DMap = {
  def run(mapLoader: MapLoader): DMap = {
    val sourceMap = mapLoader.load(FILENAME)

    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new RefBuilder(DMap.createNew(), palette)




    val pasted1 = builder.pasteSectorGroupAt(palette.getSectorGroup(1), new PointXYZ(0, 0, 0))
    val conn1: RedwallConnector = pasted1.connectors.get(0).asInstanceOf[RedwallConnector]
    //val conn1 = palette.getSectorGroup(1).allRedwallConnectors.head

    val sg2: SectorGroup = palette.getSectorGroup(2)//.rotateCW

    val conn2 = sg2.allRedwallConnectors.head
    val delta: PointXYZ = conn2.getTransformTo(conn1)
    val pasted2 = builder.pasteSectorGroup(palette.getSectorGroup(2), delta)
    conn1.linkConnectors(builder.outMap, pasted2.connectors.get(0).asInstanceOf[RedwallConnector])

    println(s"Sector count: ${builder.outMap.getSectorCount}")
    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }

}
