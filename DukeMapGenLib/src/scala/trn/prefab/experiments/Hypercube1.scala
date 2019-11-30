package trn.prefab.experiments

import trn.duke.PaletteList
import trn.prefab._
import trn.{PointXYZ, Map => DMap}


class GridManager(
  val origin: PointXYZ,
  val cellDist: Int,  // the distance between the centers of two grid nodes
  val maxGridY: Int = 4, // maximum grid spaces in the y direction; used to place z rows
  val maxGridX: Int = 4
){
  /**
    * transforms grid cell index (XYZW) to raw xyz coordinates
    */
  def cellPosition(x: Int, y: Int, z: Int, w: Int): PointXYZ = {
    // the rooms are 6 big grids width ( big grid = 1024 ) - not counting the parts that stick out
    // the parts that stick out + connectors are 2 big grids wide
    val xx = x * cellDist
    val yy = y * cellDist + z * ((maxGridY + 1) * cellDist)
    val zz = -1 * (z << 4) * cellDist + 1024 // raise it, so elevators work
    //val zz = z * + 1024 // raise it, so elevators work

    val xxx = xx + (w * ((maxGridX + 1) * cellDist)) // adjust for w coordinate
    origin.add(new PointXYZ(xxx, yy, zz))
  }
}

class HyperMapBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {
  val writer = new MapWriter(this, sgBuilder) // TODO
  val Hallway = 250
  val gridManager = new GridManager(
    new PointXYZ(DMap.MIN_X + 10*1024, DMap.MIN_Y + 10*1024, 0),
    cellDist = 8 * 1024
  )

  val horizHallway = palette.getSectorGroup(Hallway)
  val vertHallway = horizHallway.rotateCW

  val grid = scala.collection.mutable.Map[(Int, Int, Int, Int), PastedSectorGroup]()

  def placeRoom(sg: SectorGroup, x: Int, y: Int, z: Int, w: Int): PastedSectorGroup = {
    if(x < 0 || y < 0 || z < 0 || w < 0) throw new IllegalArgumentException
    val tr = sg.getAnchorSprite.get.getLocation.getTransformTo(gridManager.cellPosition(x, y, z, w))
    val psg = pasteSectorGroup(sg, tr)
    grid((x, y, z, w)) = psg
    psg
  }

  private def placeHorizontalHallway(left: PastedSectorGroup, right: PastedSectorGroup, hallway: SectorGroup): Unit = {
    val leftConn = MapWriter.eastConnector(left)
    val rightConn = MapWriter.westConnector(right)
    val cdelta: PointXYZ = MapWriter.westConnector(hallway).getTransformTo(leftConn)
    val pastedHallway = pasteSectorGroup(hallway, cdelta)
    sgBuilder.linkConnectors(MapWriter.westConnector(pastedHallway), leftConn)
    sgBuilder.linkConnectors(MapWriter.eastConnector(pastedHallway), rightConn)
  }

  private def placeVerticalHallway(top: PastedSectorGroup, bottom: PastedSectorGroup): Unit = {
    val topConn = MapWriter.southConnector(top)
    val bottomConn = MapWriter.northConnector(bottom)
    val cdelta: PointXYZ = MapWriter.northConnector(vertHallway).getTransformTo(topConn)
    val pastedHallway = pasteSectorGroup(vertHallway, cdelta)
    sgBuilder.linkConnectors(MapWriter.northConnector(pastedHallway), topConn)
    sgBuilder.linkConnectors(MapWriter.southConnector(pastedHallway), bottomConn)
  }

  def placeHallways(): Unit = {
    grid.foreach{ case ((x, y, z, w), psg) =>
      grid.get((x + 1, y, z, w)).foreach { neighboor => placeHorizontalHallway(psg, neighboor, horizHallway) }
      grid.get((x, y + 1, z, w)).foreach{ neighboor => placeVerticalHallway(psg, neighboor) }
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

  private def linkElevators(
    lower: (Int, Int, Int, Int),
    higher: (Int, Int, Int, Int),
    connectorId: Int,
    elevatorStartsLower: Boolean = true
  ): Unit = {
    val lowerRoom = grid.get(lower).get
    val higherRoom = grid.get(higher).get

    val lowerOpt = lowerRoom.getElevatorConn(connectorId)
    val higherOpt = higherRoom.getElevatorConn(connectorId)
    (lowerOpt, higherOpt) match {
      case (Some(lowerElevator), Some(higherElevator)) => {
        writer.linkElevators(lowerElevator, higherElevator, elevatorStartsLower)
        // ElevatorConnector.linkElevators(
        //   lowerElevator,
        //   lowerRoom,
        //   higherElevator, higherRoom, nextUniqueHiTag(), elevatorStartsLower)
      }
      case _ => {}
    }
  }

  def linkAllTeleporters(): Unit = {
    // TODO - for now, just linking both teleporers to the same destination room
    val connectorIds = Seq(701, 702)
    grid.foreach{
      case ((x, y, z, w), psg) if (w == 0) =>
        grid.get((x, y, z, w + 1)).foreach { _ =>
          connectorIds.foreach { connectorId =>
            if(psg.hasConnector(connectorId)){
              linkTeleporters(
                grid(x, y, z, w),
                grid(x, y, z, w + 1),
                connectorId,
              )
            }
          }
        }
      case _ => {}
    }
  }

  private def linkTeleporters(g1: PastedSectorGroup, g2: PastedSectorGroup, connectorId: Int): Unit = {
    sgBuilder.linkTeleporters(g1.getTeleportConnector(connectorId), g1, g2.getTeleportConnector(connectorId), g2)
  }
}

/**
  * A 3x3x3x2 hypercube that uses hyper1.map
  */
object Hypercube1 {
  val MainRoom = 100
  val MainRoomCenter = 101
  val BottomFloorRoom = 102
  val TopFloorRoom = 103
  val rooms = Map((0 -> BottomFloorRoom), 1 -> MainRoom, 2 -> TopFloorRoom)

  def getRoom(palette: PrefabPalette, x: Int, y: Int, z: Int, w: Int): Option[SectorGroup] = {
    val room: SectorGroup = if(x == 1 && y == 1) {
      palette.getSG(MainRoomCenter)
    }else{
      palette.getSG(rooms(z))
    }
    if (w == 1) { // color differently for w dimension
      Some(MapWriter.painted(room, PaletteList.BLUE_TO_RED))
    } else {
      Some(room)
    }
  }

  def run(sourceMap: DMap): DMap = {
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new HyperMapBuilder(DMap.createNew(), palette)

    for(x <- 0 until 3; y <- 0 until 3; z <- 0 until 3; w <- 0 until 2){
      val roomToPlace = getRoom(palette, x, y, z, w)
      roomToPlace.foreach(builder.placeRoom(_, x, y, z, w))
    }
    builder.placeHallways()
    builder.placeElevators(0, 1, 1702)
    builder.placeElevators(1, 2, 1701)
    builder.linkAllTeleporters()

    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.writer.checkSectorCount()
    builder.outMap
  }
}
