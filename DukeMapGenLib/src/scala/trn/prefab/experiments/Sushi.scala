package trn.prefab.experiments

import trn.prefab._
import trn.{HardcodedConfig, MapLoader, PointXY, PointXYZ, RandomX, ScalaMapLoader, Wall, Map => DMap}

import scala.collection.JavaConverters._ // this is the good one

class SushiBuilder(val writer: MapWriter, palette: PrefabPalette, random: RandomX = new RandomX()) {

  writer.sgBuilder.pasteAllStaySectors(palette)

  def pasteSouthOf(existing: PastedSectorGroup, newGroup: SectorGroup): PastedSectorGroup =
    writer.pasteSouthOf(existing, newGroup)

  def pasteEastOf(existing: PastedSectorGroup, newGroup: SectorGroup): PastedSectorGroup =
    writer.pasteEastOf(existing, newGroup)

  def pasteWestOf(existing: PastedSectorGroup, newGroup: SectorGroup): PastedSectorGroup =
    writer.pasteWestOf(existing, newGroup)

  def pasteNorthOf(existing: PastedSectorGroup, newGroup: SectorGroup): PastedSectorGroup =
    writer.pasteNorthOf(existing, newGroup)

  def autoLink: Unit = writer.sgBuilder.autoLinkRedwalls()




  def pasteConnectedTo(existing: PastedSectorGroup, newGroup: SectorGroup): PastedSectorGroup = {
    // tryPasteConnectedTo(existing, newGroup)
    //   .getOrElse(throw new RuntimeException("cannot find a valid connector to attach new group"))
    tryPasteConnectedTo(Seq(existing), newGroup)
      .getOrElse(throw new RuntimeException("cannot find a valid connector to attach new group"))
  }

  def tryPasteConnectedTo(existing: PastedSectorGroup, newGroup: SectorGroup): Option[PastedSectorGroup] =
    writer.tryPasteConnectedTo(existing, newGroup, PasteOptions())

  def tryPasteConnectedTo(existing: Seq[PastedSectorGroup], newGroup: SectorGroup): Option[PastedSectorGroup] = {
    existing.foreach { psg =>
      val result = tryPasteConnectedTo(psg, newGroup)
      if(result.isDefined){
        return result
      }
    }
    return None
  }

  // TODO - invent how to unpaste a SectorGroup, and then finish this
  // def tryPasteConnectedTo(target: PastedSectorGroup, sgIds: Seq[Int]): Option[PastedSectorGroup] = {
  //   if(sgIds.size < 1){
  //     Some(target)
  //   }else{
  //     val allOptions = random.shuffle(pasteOptions(target, palette.getSG(sgIds.head)))
  //     if(allOptions.size < 1){
  //       return None
  //     }else{
  //       allOptions.foreach { p =>
  //         // TODO -- but how do we UNpaste ???
  //         val newTarget = pasteAndLink(p.existing, p.newSg, p.newConn)
  //         val result = tryPasteConnectedTo(newTarget, sgIds.tail)
  //         if(result.isDefined){
  //           return result
  //         }else{
  //           throw new RuntimeException("TODO - dont know how to unpaste")
  //         }
  //       }
  //       return None
  //     }
  //   }
  // }


}

object Sushi {
  val Filename = "sushi.map"


  val PlainHall = 1
  val HallLeftPR = 2
  val Entrance = 4
  val Corner = 3
  val BarEntrance = 5
  val BigRestaurant = 6
  val CornerCashier = 7
  val Ramp1 = 8
  val Ramp2 = 9 // the ramp and hallway area, to keep things simple for now
  // TODO - shark tank here
  val Stage = 10
  val Bar = 11 // faithful recreation
  val Bar2 = 12 // not-so-faithful recreation

  val Kitchen = 13
  val Doorway1 = 14 // creating for the bar to kitchen
  val StairsDown = 15
  val Garage = 16
  val OutSideEnd = 17


  val Doorway1_alternate = 18 // this one is just for hacking

  def main(args: Array[String]): Unit = {
    val mapLoader = new ScalaMapLoader(HardcodedConfig.DOSPATH)
    val map = run(mapLoader)
    ExpUtil.deployMap(map)
  }

  def run(mapLoader: ScalaMapLoader): DMap = {
    val sourceMap = mapLoader.load(Filename)
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);

    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    val writer = MapWriter(gameCfg)
    val builder = new SushiBuilder(writer, palette)

    // original(builder, palette)
    run2(builder, palette)
    // TODO - the test in pasteConnectedTo() should also do a bounding box check!
    builder.autoLink

    println(s"Sector count: ${builder.writer.outMap.getSectorCount}")
    builder.writer.setAnyPlayerStart()
    builder.writer.clearMarkers()
    builder.writer.outMap
  }

  def original(builder: SushiBuilder, palette: PrefabPalette): Unit = {
    val entrance = builder.writer.pastedSectorGroups.head

    val corner = builder.pasteEastOf(entrance, palette.getSectorGroup(Corner))
    val psg = builder.pasteSouthOf(corner, palette.getSectorGroup(HallLeftPR))
    val psg2 = builder.pasteSouthOf(psg, palette.getSectorGroup(HallLeftPR))
    val psg3 = builder.pasteSouthOf(psg2, palette.getSectorGroup(HallLeftPR))
    val corner2 = builder.pasteSouthOf(psg3, palette.getSectorGroup(3).rotateCW)
    val barEntrance = builder.pasteWestOf(corner2, palette.getSG(BarEntrance).rotateCW)
    val corner3 = builder.pasteWestOf(barEntrance, palette.getSG(Corner).rotate180)
    val bigRestaurant = builder.pasteNorthOf(corner3, palette.getSG(BigRestaurant))
    val cashier = builder.pasteNorthOf(bigRestaurant, palette.getSG(CornerCashier).rotateCCW)
    val barRamp = builder.pasteConnectedTo(barEntrance, palette.getSG(Ramp2))
    val stage = builder.pasteEastOf(barRamp, palette.getSG(Stage))
    val bar = builder.pasteSouthOf(barRamp, palette.getSG(Bar2))
    val kitchen = builder.pasteWestOf(
      builder.pasteWestOf(bar, palette.getSG(Doorway1)),
      palette.getSG(Kitchen))
    val stairs = builder.pasteConnectedTo(kitchen, palette.getSG(StairsDown))
    val garage = builder.pasteNorthOf(stairs, palette.getSG(Garage))
    val end = builder.pasteConnectedTo(garage, palette.getSG(OutSideEnd))
  }



  def run2(builder: SushiBuilder, palette: PrefabPalette): Unit = {
    val entrance = builder.writer.pastedSectorGroups.head

    val corner = builder.pasteEastOf(entrance, palette.getSectorGroup(Corner))

    val psg = builder.pasteSouthOf(corner, palette.getSectorGroup(HallLeftPR))
    val psg2 = builder.pasteSouthOf(psg, palette.getSectorGroup(HallLeftPR))
    val psg3 = builder.pasteSouthOf(psg2, palette.getSectorGroup(HallLeftPR))

    val corner2 = builder.pasteSouthOf(psg3, palette.getSectorGroup(3).rotateCW)

    val barEntrance = builder.pasteWestOf(corner2, palette.getSG(BarEntrance).rotateCW)

    val corner3 = builder.pasteWestOf(barEntrance, palette.getSG(Corner).rotate180)
    val bigRestaurant = builder.pasteNorthOf(corner3, palette.getSG(BigRestaurant))

    val cashier = builder.pasteNorthOf(bigRestaurant, palette.getSG(CornerCashier).rotateCCW)
    // cannot find a valid connect -- its not a sprite logic error...some kind of "backtrack error"

    val barRamp = builder.pasteConnectedTo(barEntrance, palette.getSG(Ramp2))

    val stage = builder.pasteConnectedTo(barRamp, palette.getSG(Stage))

    val bar = builder.pasteConnectedTo(barRamp, palette.getSG(Bar2))
    //val doorway = builder.pasteConnectedTo(bar, palette.getSG(Doorway1))
    val doorway = builder.pasteConnectedTo(bar, palette.getSG(Doorway1_alternate))

    val kitchenOpt = builder.tryPasteConnectedTo(Seq(bar, doorway), palette.getSG(Kitchen)).orElse{
      val doorway2: PastedSectorGroup = builder.pasteConnectedTo(bar, palette.getSG(Doorway1_alternate))
      builder.tryPasteConnectedTo(Seq(doorway, doorway2, bar), palette.getSG(Kitchen))
    }
    if(!kitchenOpt.isDefined){
      println("ERROR PLACING KICTHEN")
      return
    }
    val kitchen = kitchenOpt.get


    val stairsOpt = builder.tryPasteConnectedTo(kitchen, palette.getSG(StairsDown))
    if(!stairsOpt.isDefined){
      println("ERROR PLACING STAIRS")
      return
    }

    val garage = builder.pasteConnectedTo(stairsOpt.get, palette.getSG(Garage))
    val end = builder.pasteConnectedTo(garage, palette.getSG(OutSideEnd))
  }





}
