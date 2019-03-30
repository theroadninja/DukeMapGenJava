package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapUtil, PointXY, PointXYZ, Sprite, Map => DMap}



class HyperMapBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {

  val origin: PointXYZ = new PointXYZ(DMap.MIN_X + 10*1024, DMap.MIN_Y + 10*1024, 0)

  // maximum grid spaces in the y direction; used to place z rows
  val maxGridY = 4

  val horizHallway = palette.getSectorGroup(200)
  val vertHallway = palette.getSectorGroup(250)

  val grid = scala.collection.mutable.Map[(Int, Int, Int, Int), PastedSectorGroup]()

  def placeRoom(sg: SectorGroup, x: Int, y: Int, z: Int, w: Int): PastedSectorGroup = {
    if(x < 0 || y < 0 || z < 0) throw new IllegalArgumentException

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

    val tr = anchor.getLocation.getTransformTo(origin.add(new PointXYZ(xx, yy, zz)))

    val psg = pasteSectorGroup(sg, tr)
    //val psg = new PastedSectorGroup(outMap, MapUtil.copySectorGroup(sg.map, outMap, 0, tr));

    grid((x, y, z, w)) = psg
    psg
  }


  def westConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.WestConnector).asInstanceOf[RedwallConnector]
  //def eastConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.EastConnector).asInstanceOf[RedwallConnector]
  def northConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.NorthConnector).asInstanceOf[RedwallConnector]


  def joinWalls(c1: Connector, c2: Connector): Unit = {
    PrefabUtils.joinWalls(outMap, c1.asInstanceOf[RedwallConnector], c2.asInstanceOf[RedwallConnector])
  }

  def placeHorizontalHallway(left: PastedSectorGroup, right: PastedSectorGroup): Unit = {
    val leftConn = left.findFirstConnector(SimpleConnector.EastConnector)
    val rightConn = right.findFirstConnector(SimpleConnector.WestConnector)

    val cdelta: PointXYZ = westConnector(horizHallway).getTransformTo(leftConn)
    val pastedHallway = pasteSectorGroup(horizHallway, cdelta)

    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.WestConnector), leftConn)
    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.EastConnector), rightConn)
  }

  def placeVerticalHallway(top: PastedSectorGroup, bottom: PastedSectorGroup): Unit = {
    val topConn = top.findFirstConnector(SimpleConnector.SouthConnector)
    val bottomConn = bottom.findFirstConnector(SimpleConnector.NorthConnector)

    val cdelta: PointXYZ = northConnector(vertHallway).getTransformTo(topConn)
    val pastedHallway = pasteSectorGroup(vertHallway, cdelta)

    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.NorthConnector), topConn)
    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.SouthConnector), bottomConn)
  }
  def placeHallways(): Unit = {
    grid.foreach{ case ((x, y, z, w), psg) =>
      grid.get((x + 1, y, z, w)).foreach { neighboor =>
        placeHorizontalHallway(psg, neighboor)  // INTERESTING:  no need to do x-1 ...
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

    // TODO - get rid of this
    try {
      val lowerElevator = lowerRoom.getConnector(1701).asInstanceOf[ElevatorConnector]
    } catch {
      case _: Exception => return // TODO: not all sectors have elevators ...
    }

    val lowerElevator = lowerRoom.getConnector(1701).asInstanceOf[ElevatorConnector]
    val higherElevator = higherRoom.getConnector(1701).asInstanceOf[ElevatorConnector]

    ElevatorConnector.linkElevators(
      lowerElevator,
      lowerRoom,
      higherElevator, higherRoom, nextUniqueHiTag(), elevatorStartsLower)
  }
}

object Hypercube {

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


    for(x <- 0 until 3; y <- 0 until 3; z <- 0 until 2){
      val w = 0
      if(x == 1 && y == 1){
        builder.placeRoom(mainRoomForCenter, x, y, z, w)
      }else{
        // normal room
        builder.placeRoom(mainRoom, x, y, z, w)
      }
    }

    builder.placeHallways()
    builder.placeElevators(0, 1, 1701)
    // def placeElevators(z1: Int, z2: Int, connectorId: Int): Unit ={

    // TODO - add floor numbers

    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }


}
