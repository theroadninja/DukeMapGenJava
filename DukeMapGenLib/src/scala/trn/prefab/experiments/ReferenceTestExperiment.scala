package trn.prefab.experiments

import trn.prefab._
import trn.{MapLoader, PointXY, PointXYZ, Wall, Map => DMap}
import scala.collection.JavaConverters._

import scala.collection.mutable.ListBuffer

class RefBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {

  //val pastedSectorGroups: ListBuffer[PastedSectorGroup] = new ListBuffer()

  def addViaConnector(sg: SectorGroup, connId: Int): PastedSectorGroup = {
    addViaConnectors(sg, connId, connId)
  }

  def addViaConnectors(sg: SectorGroup, existingConnId: Int, newConnId: Int): PastedSectorGroup = {
    val conns = pastedSectorGroups
      .flatMap(psg => psg.getRedwallConnectorsById(existingConnId).asScala)
      .filter(c => !c.isLinked(outMap))
    if(conns.size != 1){
      throw new RuntimeException(
        s"connector id ${existingConnId} does not match to exactly one connector - size=${conns.size}")
    }

    val conn1 = conns.head
    val conn2 = sg.getRedwallConnectorsById(newConnId).head
    val delta: PointXYZ = conn2.getTransformTo(conn1)
    val psg = pasteSectorGroup(sg, delta)
    conn1.linkConnectors(outMap, psg.getRedwallConnectorsById(newConnId).get(0))
    pastedSectorGroups.append(psg)
    psg
  }

  def addAt(sg: SectorGroup, loc: PointXYZ): PastedSectorGroup ={
    val psg = pasteSectorGroupAt(sg, loc)
    pastedSectorGroups.append(psg)
    psg
  }

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




    //val pasted1 = builder.pasteSectorGroupAt(palette.getSectorGroup(1), new PointXYZ(0, 0, 0))
    val pasted1 = builder.addAt(palette.getSectorGroup(1), new PointXYZ(0, 0, 0))
    val conn1: RedwallConnector = pasted1.connectors.get(0).asInstanceOf[RedwallConnector]
    //val conn1 = palette.getSectorGroup(1).allRedwallConnectors.head

    // val sg2: SectorGroup = palette.getSectorGroup(2)//.rotateCW
    // val conn2: RedwallConnector = sg2.getRedwallConnectorsById(123).head
    // val delta: PointXYZ = conn2.getTransformTo(conn1)
    // val pasted2 = builder.pasteSectorGroup(palette.getSectorGroup(2), delta)
    // conn1.linkConnectors(builder.outMap, pasted2.getRedwallConnectorsById(123).get(0))
    val pasted2 = builder.addViaConnector(palette.getSectorGroup(2), 123)


    // val sg3 = palette.getSectorGroup(3)
    // val conn3 = sg3.getRedwallConnectorsById(124).head.asInstanceOf[SimpleConnector]
    // val delta3 = conn3.getTransformTo(pasted2.getConnector(124).asInstanceOf[SimpleConnector])
    // val pasted3 = builder.pasteSectorGroup(sg3, delta3)
    val pasted3 = builder.addViaConnector(palette.getSectorGroup(3), 124)

    // val conn2pasted = pasted2.getConnector(124).asInstanceOf[RedwallConnector]
    // if(conn2pasted.isLinked(builder.outMap)) throw new RuntimeException
    // conn2pasted.linkConnectors(builder.outMap, pasted3.getConnector(124).asInstanceOf[RedwallConnector])

    //builder.addViaConnector(palette.getSectorGroup(3))

    builder.addViaConnectors(palette.getSectorGroup(3), 125, 124)

    println(s"Sector count: ${builder.outMap.getSectorCount}")
    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }

}
