package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapUtil, PointXY, PointXYZ, Sprite, Map => DMap}
import trn.MapImplicits._
import trn.duke.PaletteList


class HyperMapBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {

  val origin: PointXYZ = new PointXYZ(DMap.MIN_X + 10*1024, DMap.MIN_Y + 10*1024, 0)

  // maximum grid spaces in the y direction; used to place z rows
  val maxGridY = 4
  val maxGridX = 4

  val horizHallway = palette.getSectorGroup(200)
  //val horizHallway2 = palette.getSectorGroup(201)
  val vertHallway = palette.getSectorGroup(250)

  val grid = scala.collection.mutable.Map[(Int, Int, Int, Int), PastedSectorGroup]()

  def placeRoom(sg: SectorGroup, x: Int, y: Int, z: Int, w: Int): PastedSectorGroup = {
    if(x < 0 || y < 0 || z < 0 || w < 0) throw new IllegalArgumentException

    val anchor = sg.sprites.find(isAnchor).get

    // Note: for now, not placing sectors on top of each other
    // so different Z's need to go to different areas of the map
   // if(z != 0) throw new IllegalArgumentException // TODO



    // the rooms are 6 big grids width ( big grid = 1024 ) - not counting the parts that stick out
    // the parts that stick out + connectors are 2 big grids wide

    // the distance between the centers of two grid nodes
    val cellDist = 8 * 1024

    val xx = x * cellDist
    val yy = y * cellDist + z * ((maxGridY + 1) * cellDist)
    val zz = -1 * (z << 4) * cellDist + 1024 // raise it, so elevators work
    //val zz = z * + 1024 // raise it, so elevators work

    // adjust for w coordinate
    val xxx = xx + (w * ((maxGridX + 1) * cellDist))

    val tr = anchor.getLocation.getTransformTo(origin.add(new PointXYZ(xxx, yy, zz)))

    val psg = pasteSectorGroup(sg, tr)
    //val psg = new PastedSectorGroup(outMap, MapUtil.copySectorGroup(sg.map, outMap, 0, tr));

    grid((x, y, z, w)) = psg
    psg
  }


  def westConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.WestConnector).asInstanceOf[RedwallConnector]
  //def eastConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.EastConnector).asInstanceOf[RedwallConnector]
  def northConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.NorthConnector).asInstanceOf[RedwallConnector]


  def joinWalls(c1: Connector, c2: Connector): Unit = {
    //PrefabUtils.joinWalls(outMap, c1.asInstanceOf[RedwallConnector], c2.asInstanceOf[RedwallConnector])
    c1.asInstanceOf[RedwallConnector].linkConnectors(outMap, c2.asInstanceOf[RedwallConnector])
  }

  def placeHorizontalHallway(left: PastedSectorGroup, right: PastedSectorGroup, hallway: SectorGroup): Unit = {
    val leftConn = left.findFirstConnector(SimpleConnector.EastConnector).asInstanceOf[RedwallConnector]
    val rightConn = right.findFirstConnector(SimpleConnector.WestConnector)

    val cdelta: PointXYZ = westConnector(hallway).getTransformTo(leftConn)
    val pastedHallway = pasteSectorGroup(hallway, cdelta)

    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.WestConnector), leftConn)
    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.EastConnector), rightConn)
  }

  def placeVerticalHallway(top: PastedSectorGroup, bottom: PastedSectorGroup): Unit = {
    val topConn = top.findFirstConnector(SimpleConnector.SouthConnector).asInstanceOf[RedwallConnector]
    val bottomConn = bottom.findFirstConnector(SimpleConnector.NorthConnector)

    val cdelta: PointXYZ = northConnector(vertHallway).getTransformTo(topConn)
    val pastedHallway = pasteSectorGroup(vertHallway, cdelta)

    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.NorthConnector), topConn)
    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.SouthConnector), bottomConn)
  }
  def placeHallways(): Unit = {
    grid.foreach{ case ((x, y, z, w), psg) =>
      grid.get((x + 1, y, z, w)).foreach { neighboor =>
        val hallway = if(w < 1 || z < 2){
          horizHallway
        }else{
          horizHallway // TODO - make this horizHallway2
        }
        placeHorizontalHallway(psg, neighboor, hallway)  // INTERESTING:  no need to do x-1 ...
      }
      grid.get((x, y + 1, z, w)).foreach{ neighboor =>
        placeVerticalHallway(psg, neighboor)
      }
    }
  }

  def placeElevators(z1: Int, z2: Int, connectorId: Int): Unit ={
    grid.foreach{
      case ((x, y, z, w), psg) if (z == z1) =>
        grid.get((x, y, z + 1, w)).foreach { _ =>
          linkElevators(
            (x, y, z, w),
            (x, y, z + 1, w),
            connectorId,
            true
          )
        }
      case _ => {}
    }
  }

  def linkElevators(
    lower: (Int, Int, Int, Int),
    higher: (Int, Int, Int, Int),
    connectorId: Int,
    elevatorStartsLower: Boolean = true
  ): Unit = {
    val lowerRoom = grid.get(lower).get
    val higherRoom = grid.get(higher).get

    // TODO - get rid of this ( detect first... )
    try {
      val lowerElevator = lowerRoom.getConnector(connectorId).asInstanceOf[ElevatorConnector]
      val higherElevator = higherRoom.getConnector(connectorId).asInstanceOf[ElevatorConnector]
    } catch {
      case _: Exception => return // TODO: not all sectors have elevators ...
    }

    val lowerElevator = lowerRoom.getConnector(connectorId).asInstanceOf[ElevatorConnector]
    val higherElevator = higherRoom.getConnector(connectorId).asInstanceOf[ElevatorConnector]

    ElevatorConnector.linkElevators(
      lowerElevator,
      lowerRoom,
      higherElevator, higherRoom, nextUniqueHiTag(), elevatorStartsLower)
  }


  def linkAllTeleporters(): Unit = {
    // TODO - for now, just linking both to same destination
    grid.foreach{
      case ((x, y, z, w), psg) if (w == 0) =>
        grid.get((x, y, z, w + 1)).foreach { _ =>
          if(psg.hasConnector(701)){
            linkTeleporters(
              (x, y, z, w),
              (x, y, z, w + 1),
              701,
            )
          }
          if(psg.hasConnector(702)){
            linkTeleporters(
              (x, y, z, w),
              (x, y, z, w + 1),
              702,
            )
          }
        }
      case _ => {}
    }


  }

  def linkTeleporters(
    node1: (Int, Int, Int, Int),
    node2: (Int, Int, Int, Int),
    connectorId: Int

  ): Unit = {

    val room1 = grid.get(node1).get
    val room2 = grid.get(node2).get

    TeleportConnector.linkTeleporters(
      room1.getConnector(connectorId).asInstanceOf[TeleportConnector],
      room1,
      room2.getConnector(connectorId).asInstanceOf[TeleportConnector],
      room2,
      nextUniqueHiTag()
    )

  }
}

/**
  * A 3x3x3x2 hypercube that uses hyper1.map
  */
object Hypercube1 {

  def run1(sourceMap: DMap): DMap = {
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new HyperMapBuilder(DMap.createNew(), palette)
    val mainRoom: SectorGroup = palette.getSectorGroup(100)
    //val mainRoomForCenter: SectorGroup = palette.getSectorGroup(101)



    builder.placeRoom(mainRoom, 0, 0, 0, 0)
    builder.placeRoom(mainRoom, 0, 0, 1, 0)
    builder.linkElevators((0, 0, 0, 0), (0, 0, 1, 0), 1701, true)

    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }

  def run(sourceMap: DMap): DMap = {
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new HyperMapBuilder(DMap.createNew(), palette)
    val mainRoom: SectorGroup = palette.getSectorGroup(100)
    val mainRoomForCenter: SectorGroup = palette.getSectorGroup(101)
    val bottomFloorRoom: SectorGroup = palette.getSectorGroup(102)
    val topFloorRoom: SectorGroup = palette.getSectorGroup(103)

    for(x <- 0 until 3; y <- 0 until 3; z <- 0 until 3; w <- 0 until 2){

      val roomToPlace: Option[SectorGroup] = if(x == 1 && y == 1) {
        if (z == 1 && w < 1) {
          Some(mainRoomForCenter)
        } else {
          None // no center room on top floor, or bottom floor, or in red W
        }
      }else if(x == 2 && y == 2){
        // nothing on corners - TODO - get rid of this
        None
      }else if(z == 0) {
        Some(bottomFloorRoom)
      }else if(z == 2){
        Some(topFloorRoom)
      }else{
        Some(mainRoom)
      }

      val roomToPlace2 = roomToPlace.map { room =>
        if (w == 1) {
          val r = room.copy()
          r.getMap.allWalls.foreach(_.setPal(PaletteList.BLUE_TO_RED))
          r
        } else {
          room
        }
      }

      roomToPlace2.foreach(builder.placeRoom(_, x, y, z, w))


      // doesnt work - it affects the entire map
      // if(w == 1){
      //   psg.getMap.allWalls.foreach { w =>
      //     w.setPal(PaletteList.BLUE_TO_RED)
      //   }
      // }else if(w == 0){
      //   psg.getMap.allWalls.foreach { w =>
      //     w.setPal(PaletteList.NORMAL)
      //   }

      // }
    }

    builder.placeHallways()
    builder.placeElevators(0, 1, 1702)
    builder.placeElevators(1, 2, 1701)
    // def placeElevators(z1: Int, z2: Int, connectorId: Int): Unit ={
    builder.linkAllTeleporters()

    // TODO - add floor numbers

    builder.setAnyPlayerStart()
    builder.clearMarkers()
    println(s"TOTAL SECTOR COUNT: ${builder.outMap.getSectorCount}")
    if(builder.outMap.getSectorCount > 1024){
      throw new Exception("too many sectors") // I think maps are limited to 1024 sectors
    }
    builder.outMap
  }


}
