package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapLoader, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, Map => DMap}
import trn.MapImplicits._
import trn.duke.{PaletteList, TextureList}
import trn.prefab.hypercube.GridManager
import trn.prefab.hypercube.GridManager.Cell

import scala.collection.JavaConverters._
import trn.MapImplicits._
import trn.prefab.PrefabPalette
import trn.prefab.experiments.Hypercube4.{Filename, run}

class BasicBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {
  val writer = new MapWriter(this, sgBuilder) // TODO

  def pasteAllStays(): Seq[PastedSectorGroup] = {
    sgBuilder.pasteAllStaySectors(palette)
  }

  def sectorCount: Int = sgBuilder.sectorCount

  def spaceAvailable(b: BoundingBox): Boolean = writer.spaceAvailable(b)

  // def pasteAndLink(existing: RedwallConnector, newSg: SectorGroup, newConn: RedwallConnector): PastedSectorGroup =
  //   writer.pasteAndLink(existing, newSg, newConn)

  // def openConnectors: Seq[RedwallConnector] = pastedSectorGroups.flatMap{ psg =>
  //   psg.unlinkedConnectors.collect{case cc: RedwallConnector => cc}
  // }

  // def pasteConnectedTo(existingId: Int, newSg: SectorGroup, allowOverlap: Boolean = false): PastedSectorGroup = {
  //   tryPastedConnectedTo(existingId, newSg, allowOverlap).getOrElse(throw new Exception("failed to paste sector group"))
  // }

  // def tryPastedConnectedTo(existingId: Int, newSg: SectorGroup, allowOverlap: Boolean = false): Option[PastedSectorGroup] = {
  //   val existing = pastedSectorGroups.find(psg => psg.groupId == Some(existingId)).get
  //   ExperimentalWriter.tryPasteConnectedTo(writer, writer.random, existing, newSg, allowOverlap = allowOverlap)
  // }
  def tryPastedConnectedTo(existing: PastedSectorGroup, newSg: SectorGroup): Option[PastedSectorGroup] = {
    writer.tryPasteConnectedTo(existing, newSg, PasteOptions())
    // ExperimentalWriter.tryPasteConnectedTo(writer, writer.random, existing, newSg)
  }

  def pasteConnectedTo(existingPsg: PastedSectorGroup, newSg: SectorGroup, allowOverlap: Boolean = false): PastedSectorGroup =
    writer.tryPasteConnectedTo(existingPsg, newSg, PasteOptions(allowOverlap = allowOverlap)).getOrElse(throw new Exception("couldnt paste sg"))
    //ExperimentalWriter.tryPasteConnectedTo(writer, writer.random, existingPsg, newSg, allowOverlap).getOrElse(throw new Exception("couldnt paste sg"))


  def pasteOptions(existingPsg: PastedSectorGroup, newSg: SectorGroup, rotate: Boolean): Seq[Placement] = {
    // TODO - make this work with rotate = false
    Placement.pasteOptions(writer, existingPsg, newSg)

  }

  def findFirstPsg(psgId: Int): PastedSectorGroup = pastedSectorGroups.find(_.groupId == Some(psgId)).get
}

object PersonalStorage extends PrefabExperiment {
  override val Filename = "storage.map"

  override def run(mapLoader: MapLoader): DMap = {
    val sourceMap = mapLoader.load(Filename)
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true)
    val builder = new BasicBuilder(DMap.createNew(), palette)
    try{
      run(builder, palette)
      builder.clearMarkers()
    }catch{
      case ex: Exception => {
        ex.printStackTrace()
      }
    }
    println(s"Sector count: ${builder.outMap.getSectorCount}")
    builder.outMap
  }

  def run(builder: BasicBuilder, palette: PrefabPalette): Unit = {
    builder.pasteAllStays()

    val Garage = 2
    val OutsideLots = 3
    val BlueDoor1 = 4
    val BlueDoor2 = 5

    val blueLocks = Seq(BlueDoor1, BlueDoor2)


    val Office = 6
    val LowerFloor = 7

    val LargeStorage = 8
    val SmallStorage = 9

    val Ramp = 10

    val UpperFloor = 11
    val UpperFloorWindows = 12
    val ExplosiveWall = 13
    val NukeButton = 14

    // val garage = builder.pastedSectorGroups.find(psg => psg.groupId == Some(Garage)).get
    // ExperimentalWriter.tryPasteConnectedTo(builder.writer, builder.writer.random, garage, sg).getOrElse(throw new Exception("couldnt paste to garage"))

    builder.pasteConnectedTo(builder.findFirstPsg(Garage), palette.getSectorGroup(OutsideLots))
    val hallId = builder.writer.randomElement(blueLocks)
    val psg = builder.pasteConnectedTo(builder.findFirstPsg(Garage), palette.getSectorGroup(hallId))
    builder.pasteConnectedTo(psg, palette.getSectorGroup(Office))

    builder.pasteConnectedTo(builder.findFirstPsg(Garage), palette.getSectorGroup(LowerFloor))



    val lowerPsg = builder.pastedSectorGroups.find(_.groupId == Some(LowerFloor)).get
    val smallStorage = palette.getSectorGroup(SmallStorage)

    // ExperimentalWriter.tryPasteConnectedTo(builder.writer, builder.writer.random, lowerPsg, smallStorage)

    // This works:
    // val conns = lowerPsg.redwallConnectors.filter(!_.isLinked(builder.outMap))
    //   .filter(_.totalManhattanLength(builder.outMap) == 1024)
    //   .filter(ExperimentalWriter.isEastConn(_))
    // builder.pasteAndLink(conns.head, smallStorage, smallStorage.connectors.get(0).asInstanceOf[RedwallConnector])

    def addStorageUnits(psg: PastedSectorGroup): Unit = {
      while(builder.tryPastedConnectedTo(psg, palette.getSectorGroup(SmallStorage)).isDefined){}
      while(builder.tryPastedConnectedTo(psg, palette.getSectorGroup(LargeStorage)).isDefined){}

    }


    //builder.tryPastedConnectedTo(LowerFloor, smallStorage)
    while(builder.tryPastedConnectedTo(builder.findFirstPsg(LowerFloor), smallStorage).isDefined){}

    builder.pasteConnectedTo(builder.findFirstPsg(LowerFloor), palette.getSectorGroup(Ramp))
    val upper1 = builder.pasteConnectedTo(builder.findFirstPsg(Ramp), palette.getSectorGroup(UpperFloor), allowOverlap = true)
    addStorageUnits(upper1)
    val upper2 = builder.pasteConnectedTo(builder.findFirstPsg(UpperFloor), palette.getSectorGroup(UpperFloorWindows), allowOverlap = true)
    addStorageUnits(upper2)

    builder.pasteConnectedTo(upper2, palette.getSectorGroup(ExplosiveWall), allowOverlap = true)
    builder.pasteConnectedTo(builder.findFirstPsg(ExplosiveWall), palette.getSG(NukeButton), allowOverlap = true)


    builder.setPlayerStart(builder.findFirstPsg(Garage))
    // builder.setAnyPlayerStart()

    // TODO - add an optional "duke logic check" that will warn for obvious issues with SE sprites
    // TODO - add ability to pull things from multiple files, including the nuke button
  }
}

