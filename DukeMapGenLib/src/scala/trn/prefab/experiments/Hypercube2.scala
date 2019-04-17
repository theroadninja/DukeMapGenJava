package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, Map => DMap}
import trn.MapImplicits._
import trn.duke.PaletteList
import trn.prefab.SimpleConnector.Direction
import trn.prefab.experiments.Hyper2MapBuilder.Cell

import scala.collection.JavaConverters._


object Hyper2MapBuilder {
  type Cell = (Int, Int, Int, Int)
}

class Hyper2MapBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {

  val grid = scala.collection.mutable.Map[Cell, PastedSectorGroup]()
  val grid2 = scala.collection.mutable.Map[Cell, Room]()
  val margin: Int = 10 * 1024

  // Main hypercube stuff goes here
  val origin: PointXYZ = new PointXYZ(
    DMap.MIN_X + margin + (14 * 1024),  // extra for the train
    DMap.MIN_Y + margin, 0)

  // tracks locations for placing "floating" sectors that can go anywhere, like underwater areas.
  val sgPacker: SectorGroupPacker = new SimpleSectorGroupPacker(
    new PointXY(DMap.MIN_X, 0),
    new PointXY(DMap.MAX_X, DMap.MAX_Y),
    512)

  // rooms are six big grid cells wide and anchor is in the middle
  // hallways are one grid cell wide
  val cellDist = (6 + 1) * 1024
  val hallwayWidth = 1024
  val MAXGRID = 2

  def placeAnywhere(sg: SectorGroup): PastedSectorGroup = {
    val topLeft = sgPacker.reserveArea(sg)
    val tr = sg.boundingBox.getTranslateTo(topLeft).withZ(0)
    pasteSectorGroup(sg, tr)
  }

  def joinWalls(c1: Connector, c2: Connector): Unit = {
    if(c1 == null || c2 == null) throw new IllegalArgumentException
    c1.asInstanceOf[RedwallConnector].linkConnectors(outMap, c2.asInstanceOf[RedwallConnector])
  }

  def toCoordinates(gridCell: (Int, Int, Int, Int)): PointXYZ = {
    // offsets must shift the max width of the grid for each value
    val offsetForZ = gridCell._3 * cellDist * (MAXGRID + 1)
    val offsetForW = gridCell._4 * cellDist * (MAXGRID + 1) // TODO - actually we'll want W in the same place ...
    val x = gridCell._1 * cellDist + offsetForW
    val y = gridCell._2 * cellDist + offsetForZ
    //val z = gridCell._3 * cellDist

    // NOTE:  negative z is higher.  Also need to shift z
    val z = -1 * (gridCell._3 << 4) * cellDist + 1024 // raise it, so elevators work
    new PointXYZ(x, y, z)
  }

  /** sets the player start of the map to the location of the first player start marker sprite it finds */
  def setPlayerStart(cell: Cell): Unit = setPlayerStart(grid(cell))

  @deprecated // old version
  def addRoom(room: SectorGroup, gridCell: (Int, Int, Int, Int)): PastedSectorGroup = {
    val anchor: PointXYZ = room.getAnchor.withZ(0)
    val p = toCoordinates(gridCell)
    val tr = anchor.getTransformTo(origin.add(p))
    val psg = pasteSectorGroup(room, tr)
    grid(gridCell) = psg
    psg
  }

  // new version
  def addRoom(room: Room, gridCell: (Int, Int, Int, Int)): PastedSectorGroup = {
    if(grid.get(gridCell).nonEmpty) throw new IllegalArgumentException("that cell already taken")
    if(grid2.get(gridCell).nonEmpty) throw new IllegalArgumentException("that cell already taken")

    val anchor = room.sectorGroup.getAnchor.withZ(0)
    val p = toCoordinates(gridCell)

    val tr = anchor.getTransformTo(origin.add(p))
    val psg = pasteSectorGroup(room.sectorGroup, tr)

    grid(gridCell) = psg
    grid2(gridCell) = room

    psg
  }

  def autoLinkRooms(): Unit = {
    grid2.foreach { case (gridCell, room) =>
        val (x, y, z, w) = gridCell

        // if we only check +1, no risk of repeating
      Seq(  // TODO - the other dimensions
        (x + 1, y, z, w),
        (x, y + 1, z, w),
        (x, y, z + 1, w),
        (x, y, z, w + 1)
      ).foreach{ neighboorCell =>
        grid2.get(neighboorCell).foreach { n =>

          if(x + 1 == neighboorCell._1){
            tryAutoLinkWestToEast(gridCell, neighboorCell)
          } else if(y + 1 == neighboorCell._2){
            tryAutoLinkNorthToSouth(gridCell, neighboorCell)
          } else if(z + 1 == neighboorCell._3){
            tryAutoLinkElevator(gridCell, neighboorCell)
          } else if(w + 1 == neighboorCell._4){
            tryAutoLinkTeleporer(gridCell, neighboorCell)
          }
        }
      }
    }
  }

  def tryAutoLinkTeleporer(cell1: Cell, cell2: Cell): Boolean = {
    (grid2.get(cell1), grid2.get(cell2)) match {
      case (Some(_), Some(_)) =>
        val teleporters1 = grid(cell1).findConnectorsByType(ConnectorType.TELEPORTER)
        val teleporters2 = grid(cell2).findConnectorsByType(ConnectorType.TELEPORTER)
        if(teleporters1.size() == 1 && teleporters2.size() == 1){
          TeleportConnector.linkTeleporters(teleporters1.get(0), grid(cell1), teleporters2.get(0), grid(cell2), nextUniqueHiTag())
          true
        } else {
        false
        }
      case _ => false
    }
  }

  def tryAutoLinkElevator(bottomCell: Cell, topCell: Cell, elevatorStartsLower: Boolean = true): Boolean = {
    (grid2.get(bottomCell), grid2.get(topCell)) match {
      case (Some(bottomRoom), Some(topRoom)) =>
        if(bottomRoom.elevator && topRoom.elevator){
          ElevatorConnector.linkElevators(
            grid(bottomCell).getFirstElevatorConnector,
            this,
            grid(topCell).getFirstElevatorConnector,
            this,
            nextUniqueHiTag(),
            elevatorStartsLower)
          true
        } else {
          false
        }
      case _ => false
    }
  }

  def tryAutoLinkWestToEast(westCell: Cell, eastCell: Cell): Boolean ={
    val westRoomOpt = grid2.get(westCell)
    val eastRoomOpt = grid2.get(eastCell)
    if(westRoomOpt.isEmpty || eastRoomOpt.isEmpty) return false;
    val westRoom = westRoomOpt.get
    val eastRoom = eastRoomOpt.get

    val E = Heading.EAST
    val W = Heading.WEST
    if((westRoom.hasLowDoor(E) && eastRoom.hasLowDoor(W)
      || (westRoom.hasHighDoor(E) && eastRoom.hasHighDoor(W)) )){

      //val eastWestHallway = palette.getSectorGroup(200)
      val hallway = palette.getSectorGroup(200)
      placeHallwayEW(grid(westCell), hallway, grid(eastCell))
      return true
    }else if(westRoom.hasLowDoor(E) && eastRoom.hasHighDoor(W)){
      placeHallwayEW(grid(westCell), palette.getSectorGroup(202), grid(eastCell))
      return true
    }

    return false
  }

  def tryAutoLinkNorthToSouth(northCell: Cell, southCell: Cell): Boolean = {
    (grid2.get(northCell), grid2.get(southCell)) match {
      case (Some(northRoom), Some(southRoom)) => {
        if((northRoom.hasLowDoor(Heading.S) && southRoom.hasLowDoor(Heading.N))
        || (northRoom.hasHighDoor(Heading.S) && southRoom.hasHighDoor(Heading.N))) {

          //val northSouthHallway = palette.getSectorGroup(201)
          val hallway: SectorGroup = palette.getSectorGroup(201)
          placeHallwayNS(grid(northCell), hallway, grid(southCell))
          true
        } else if(northRoom.hasHighDoor(Heading.S) && southRoom.hasLowDoor(Heading.N)) {

          val elevator: SectorGroup = palette.getSectorGroup(203)
          placeHallwayNS(grid(northCell), elevator, grid(southCell))
          true
        } else {
          false
        }
      }
      case _ => false
    }
  }

  def placeHallwayEW(hallway: SectorGroup, left: (Int, Int, Int, Int), right: (Int, Int, Int, Int)): PastedSectorGroup = {
    if(left._1 + 1 != right._1) throw new IllegalArgumentException
    if(!(left._2 == right._2 && left._3 == right._3 && left._4 == right._4)) throw new IllegalArgumentException
    val leftRoom = grid(left)
    val rightRoom = grid(right)
    placeHallwayEW(leftRoom, hallway, rightRoom)
  }

  def placeHallwayEW(leftRoom: PastedSectorGroup, hallway: SectorGroup, rightRoom: PastedSectorGroup): PastedSectorGroup = {
    def westConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.WestConnector).asInstanceOf[RedwallConnector]
    def eastConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.EastConnector).asInstanceOf[RedwallConnector]
    val leftConn = leftRoom.findFirstConnector(SimpleConnector.EastConnector).asInstanceOf[RedwallConnector]
    val rightConn = rightRoom.findFirstConnector(SimpleConnector.WestConnector).asInstanceOf[RedwallConnector]

    //val cdelta: PointXYZ = westConnector(hallway).getTransformTo(leftConn)
    val cdelta: PointXYZ = if(leftConn.getAnchorPoint.z < rightConn.getAnchorPoint.z){
      westConnector(hallway).getTransformTo(leftConn)
    }else{
      eastConnector(hallway).getTransformTo(rightConn)
    }

    val pastedHallway = pasteSectorGroup(hallway, cdelta)
    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.WestConnector), leftConn)
    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.EastConnector), rightConn)
    pastedHallway
  }

  def placeHallwayNS(hallway: SectorGroup, top: Cell, bottom: Cell): PastedSectorGroup = {
    val topRoom = grid(top)
    val bottomRoom = grid(bottom)
    placeHallwayNS(topRoom, hallway, bottomRoom)
  }

  def placeHallwayNS(topRoom: PastedSectorGroup, hallway: SectorGroup, bottomRoom: PastedSectorGroup): PastedSectorGroup = {
    def northConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.NorthConnector).asInstanceOf[RedwallConnector]
    def southConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.SouthConnector).asInstanceOf[RedwallConnector]

    val topConn = topRoom.findFirstConnector(SimpleConnector.SouthConnector).asInstanceOf[RedwallConnector]
    val bottomConn = bottomRoom.findFirstConnector(SimpleConnector.NorthConnector).asInstanceOf[RedwallConnector]

    val cdelta: PointXYZ = if(topConn.getAnchorPoint.z < bottomConn.getAnchorPoint.z){
      northConnector(hallway).getTransformTo(topConn)
    } else {
      southConnector(hallway).getTransformTo(bottomConn)
    }

    val pastedHallway = pasteSectorGroup(hallway, cdelta)
    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.NorthConnector), topConn)
    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.SouthConnector), bottomConn)
    pastedHallway
  }


}


object Room {

  def apply(
    sectorGroup: SectorGroup,
    highDoors: Map[Int, Boolean],
    lowDoors: Map[Int, Boolean],
    elevator: Boolean = false,
    teleporter: Boolean = false
  ): Room = {
    new Room(sectorGroup, highDoors, lowDoors, elevator)
  }

  def apply(
    sectorGroup: SectorGroup,
    highDoors: Seq[Int],
    lowDoors: Seq[Int],
    elevator: Boolean,
    teleporter: Boolean): Room = {
    new Room(sectorGroup, highDoors.map(d => (d, true)).toMap, lowDoors.map(d => (d, true)).toMap, elevator)
  }

  // TODO - how do we detect low doors vs high?
  def autoDoors(sectorGroup: SectorGroup): Seq[Int] = {
    val standardDoorLength = 2048
    def hasHeading(h: Int): Boolean  = sectorGroup.getRedwallConnectors(SimpleConnector.connectorTypeForHeading(h)) match {
      case x: Seq[RedwallConnector] => {
        x.find(_.totalManhattanLength(sectorGroup.getMap) == standardDoorLength).nonEmpty
      }
      case _ => false
    }
    val headings: Seq[Int] = Heading.all.asScala.flatMap(h => if(hasHeading(h)){ Some(h.toInt)}else{ None })
    headings
  }

  def auto(sectorGroup: SectorGroup, highDoors: Seq[Int], lowDoors: Seq[Int]): Room = {
    // TODO - maybe the best thing is for the rooms to not even understand high vs low and do it at a lower level...
    val hasTeleport: Boolean = sectorGroup.getTeleportConnectors().filter(t => !t.isWater).size > 0
    val hasElevator: Boolean = sectorGroup.getElevatorConnectors().size > 0
    Room(sectorGroup, highDoors, lowDoors, hasElevator, hasTeleport)
  }

  def flipY(doors: Map[Int, Boolean]): Map[Int, Boolean] = {
    doors.map{ d =>
      if(d._1 == Heading.S){
        (Heading.N, d._2)
      }else if(d._1 == Heading.N){
        (Heading.S, d._2)
      }else{
        d
      }
    }
  }

  def flipX(doors: Map[Int, Boolean]): Map[Int, Boolean] = {
    doors.map{ d =>
      if(d._1 == Heading.W){
        (Heading.E, d._2)
      }else if(d._1 == Heading.E){
        (Heading.W, d._2)
      }else{
        d
      }
    }
  }

}

case class Room(
  sectorGroup: SectorGroup,
  highDoors: Map[Int, Boolean],
  lowDoors: Map[Int, Boolean],
  elevator: Boolean = false,
  teleporter: Boolean = false
) {
  def hasLowDoor(direction: Int): Boolean = lowDoors.get(direction).getOrElse(false)
  def hasHighDoor(direction: Int): Boolean = highDoors.get(direction).getOrElse(false)
  def flipY: Room = {
    Room(sectorGroup.flippedY(), Room.flipY(highDoors), Room.flipY(lowDoors), elevator, teleporter)
  }
  def flipX: Room = {
    Room(sectorGroup.flippedX(), Room.flipX(highDoors), Room.flipX(lowDoors), elevator, teleporter)
  }
}

object Hypercube2 {
  //
  // Room Prefabs (room id is marker w/ lotag 1)
  //
  // 100 - basic room
  // 101 - pool room top
  // 102 - pool room bottom
  // 103 - reactor room top

  // 104 - circular nuke sign room (can connect on both top or bottom)
  // 105 - room with a view (must be top level)
  // 106 - command center
  // 107 - habitat
  // 108 - train station

  // 109 - testing double red wall
  // 1091 - other room for testing double red wall

  // 110 - basic room that can be assembled
  // 1101 - basic room assembly - elevator
  // 1102 - basic room assembly - empty
  // 1103 - basic room assembly - teleporter
  // 1104 - basic room assembly - teleporter empty

  // Modular Connectors
  // 123 - ELEVATOR
  // 124 - (test connector for multi redwall)
  // 125 - TELEPORTER

  // 111 - roof antenna
  // 11101 - connector for roof antenna
  // 112 - roof antenna bottom (no longer used -- now its a child sector)

  // 113 - medical bay
  // 11301 - medical bay upper floor (no longer used -- now its a child sector)
  // 11302 - redwall connection to upper floor

  // Intra-sector connectors
  // 150 - elevator sector

  //
  // Connection Groups
  //
  // 200 - east-west hallway
  // 201 - north-south hallway
  //
  // 202 - east-west simple elevator, with east=high part
  // 203 - low south to night north elevator
  //
  // 204 - hallway for troubleshooting

  //
  // More underwater stuff
  //
  // 301 - underwater connector
  //
  // 303 - reactor underwater


  // note:  6 large squares ( 1042 ) seem like a good size.


  def run(sourceMap: DMap): DMap = {

    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true);
    val builder = new Hyper2MapBuilder(DMap.createNew(), palette)

    def modularRoom(x: Int, y: Int, elevator: Boolean, teleporter: Boolean, w: Int = 0): Room = {
      val lowDoors = Seq(Heading.W, Heading.N)
      val teleportId = if(teleporter){ 1103 }else{ 1104 } // 1104 is teleporter empty
      val elevatorId = if(elevator){ 1101 }else{ 1102 }
      val sg = palette.getSectorGroup(110).connectedTo(125, palette.getSectorGroup(teleportId))
      val sg2 = sg.connectedTo(123, palette.getSectorGroup(elevatorId))
      val sg3 = w match {
        case 0 => sg2
        case 1 => {
          val g = sg2.copy()
          g.getMap.allWalls.foreach(_.setPal(PaletteList.BLUE_TO_RED))
          g
        }
      }
      val modularRoomBR = Room(sg3, Seq(), lowDoors, elevator, teleporter)
      (x, y) match {
        case (0, 0) => modularRoomBR.flipX.flipY
        case (1, 0) => modularRoomBR.flipY
        case (0, 1) => modularRoomBR.flipX
        case (1, 1) => modularRoomBR
        case _ => throw new IllegalArgumentException
      }
    }

    val allDoors: Seq[Int] = Heading.all.asScala.map(_.toInt)
    val circleRoom = Room.auto(palette.getSectorGroup(104), allDoors, allDoors)

    val roomWithView = Room(palette.getSectorGroup(105), Seq(), Seq(Heading.W, Heading.N), false, false)

    val poolRoom = Room.auto(palette.getSectorGroup(101), allDoors, Seq())

    val commandCenter = Room.auto(palette.getSectorGroup(106), Seq(), Room.autoDoors(palette.getSectorGroup(106)))

    val habitat = Room.auto(palette.getSectorGroup(107), Seq(), Seq(Heading.W))

    val trainStop = Room(palette.getSectorGroup(108), Seq(), Seq( Heading.EAST, Heading.SOUTH ), false, false)

    val ELEVATOR_GROUP = 1101
    val dishSg = palette.getSectorGroup(111).connectedTo(123, palette.getSectorGroup(ELEVATOR_GROUP))
    val dishRoof = Room.auto(dishSg, Seq(Heading.S, Heading.W), Seq())

    // 113 - medical bay
    val medicalBay = Room(palette.getSectorGroup(113), Seq(), Seq(Heading.S), false, false)

    // TODO - anchor sprite removal with connectedTo() is not working


    // BOTTOM FLOOR
    builder.addRoom(trainStop, (0, 0, 0, 0))
    builder.addRoom(habitat, (1, 0, 0, 0))
    builder.addRoom(modularRoom(0, 1, true, true), (0, 1, 0, 0))
    builder.addRoom(poolRoom, (1, 1, 0, 0))

    // TOP FLOOR
    builder.addRoom(commandCenter.flipY, (0, 0, 1, 0))
    builder.addRoom(dishRoof, (1, 0, 1, 0)) // TOP RIGHT
    builder.addRoom(modularRoom(0, 1, true, true), (0, 1, 1, 0)) // BOTTOM LEFT
    builder.addRoom(roomWithView, (1, 1, 1, 0))   // BOTTOM RIGHT

    // W+ ------------------------------------

    // BOTTOM FLOOR
    // builder.addRoom(modularRoom(0, 0, true, false, w=1), (0, 0, 0, 1))  // TOP LEFT
    builder.addRoom(medicalBay, (0, 0, 0, 1))  // TOP LEFT

    //builder.addRoom(modularRoom(1, 0, false, false, w=1), (1, 0, 0, 1))

    // TODO - need to shift the hitags/lotags!
    builder.addRoom(trainStop.flipX, (1, 0, 0, 1))
    builder.addRoom(modularRoom(0, 1, true, true, w=1), (0, 1, 0, 1))
    builder.addRoom(modularRoom(1, 1, true, false, w=1), (1, 1, 0, 1))

    // TOP FLOOR
    builder.addRoom(modularRoom(0, 0, false, false, w=1), (0, 0, 1, 1))  // TOP LEFT
    builder.addRoom(modularRoom(1, 0, false, false, w=1), (1, 0, 1, 1))
    builder.addRoom(modularRoom(0, 1, true, true, w=1), (0, 1, 1, 1))
    builder.addRoom(modularRoom(1, 1, true, false, w=1), (1, 1, 1, 1))

    builder.autoLinkRooms()

    builder.setPlayerStart((0, 0, 0, 0))
    builder.clearMarkers()
    builder.outMap
  }









  // def run2(sourceMap: DMap): DMap = {
  //   val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true);
  //   val builder = new Hyper2MapBuilder(DMap.createNew(), palette)

  //   val sg = palette.getSectorGroup(109)
  //   val sg2 = palette.getSectorGroup(1091)

  //   val basicElevator = palette.getSectorGroup(1101)
  //   val basicEmpty = palette.getSectorGroup(1102).flippedY(0).flippedX(0)
  //   val basicRoom = palette.getSectorGroup(110).flippedY().flippedX()

  //   val room = basicRoom.connectedTo(123, basicEmpty)
  //   builder.placeAnywhere(room);

  //   builder.setAnyPlayerStart()
  //   builder.clearMarkers()
  //   builder.outMap
  // }

  def runUnderwaterTest(sourceMap: DMap): DMap = {
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new Hyper2MapBuilder(DMap.createNew(), palette)

    val basicRoom = palette.getSectorGroup(100)

    val poolRoomTop = palette.getSectorGroup(101)
    val poolRoomBottom = palette.getSectorGroup(102)
    val reactorRoomTop = palette.getSectorGroup(103)

    val eastWestHallway = palette.getSectorGroup(200)
    val northSouthHallway = palette.getSectorGroup(201)
    val eastWestElevator = palette.getSectorGroup(202)

    val underWaterPassage1 = palette.getSectorGroup(301)
    val reactorUnderwaterRoom = palette.getSectorGroup(303)

    val a = basicRoom.getAnchor.asPointXY

    val topPsg = builder.addRoom(reactorRoomTop, (1, 0, 0, 0))

    //val tmpPoolBottom = poolRoomBottom.connectedTo(RedwallJoinType.NorthToSouth, underWaterPassage1)
    val tmpPoolBottom = poolRoomBottom.connectedTo(RedwallJoinType.NorthToSouth, underWaterPassage1)
      //.connectedTo(RedwallJoinType.NorthToSouth, poolRoomBottom)
      .connectedTo(RedwallJoinType.NorthToSouth, reactorUnderwaterRoom)

    val bottomPsg = builder.placeAnywhere(tmpPoolBottom)

    val top2Psg = builder.addRoom(poolRoomTop, (1, 1, 0, 0))

    builder.linkAllWater2(Seq(topPsg, top2Psg), Seq(bottomPsg))

    builder.placeHallwayNS(northSouthHallway, (1, 0, 0, 0), (1, 1, 0, 0))

    // TODO - the hallway connectors should all be automatic ...

    // TODO - redwall connectors should check distance and throw if wall length not equal

    // TODO copy code should warn if there are sectors in the map that it didnt copy

    // TODO - add a smaller type of door

    builder.addRoom(basicRoom.flippedX(a.x), (0, 1, 0, 0))
    builder.placeHallwayEW(eastWestElevator, (0, 1, 0, 0), (1, 1, 0, 0))

    // builder.addRoom(basicRoom, (1, 1, 0, 0))
    // builder.addRoom(basicRoom.flippedX(a.x), (0, 1, 0, 0))
    // builder.addRoom(basicRoom.flippedY(a.y).flippedX(a.x), (0, 0, 0, 0))

    // builder.placeHallwayEW(eastWestHallway, (0, 1, 0, 0), (1, 1, 0, 0))
    // builder.placeHallwayNS(northSouthHallway, (0, 0, 0, 0), (0, 1, 0, 0))

    // builder.addRoom(basicRoom.flippedY(a.x), (1, 0, 0, 0))
    // builder.placeHallwayEW(eastWestHallway, (0, 0, 0, 0), (1, 0, 0, 0))
    // builder.placeHallwayNS(northSouthHallway, (1, 0, 0, 0), (1, 1, 0, 0))

    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }


}
