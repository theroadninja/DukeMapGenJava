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

  val grid = scala.collection.mutable.Map[(Int, Int, Int, Int), PastedSectorGroup]()

  val grid2 = scala.collection.mutable.Map[(Int, Int, Int, Int), Room]()

  val margin: Int = 10 * 1024

  // Main hypercube stuff goes here
  val origin: PointXYZ = new PointXYZ(
    DMap.MIN_X + margin + (14 * 1024),  // extra for the train
    DMap.MIN_Y + margin, 0)


  //val waterOrigin: PointXYZ = new PointXYZ(DMap.MIN_X + margin, DMap.MIN_Y + margin + 20 * 1024, 0)

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
    //PrefabUtils.joinWalls(outMap, c1.asInstanceOf[RedwallConnector], c2.asInstanceOf[RedwallConnector])
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

  // old version
  def addRoom(room: SectorGroup, gridCell: (Int, Int, Int, Int)): PastedSectorGroup = {

    val anchor: PointXYZ = room.getAnchor.withZ(0)
    val p = toCoordinates(gridCell)

    //val tr = anchor.getTransformTo(origin.add(new PointXYZ(x, y, z)))
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
          }
        }
      }
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
    elevator: Boolean = false
  ): Room = {
    new Room(sectorGroup, highDoors, lowDoors, elevator)
  }

  def apply(
  sectorGroup: SectorGroup,
  highDoors: Seq[Int],
  lowDoors: Seq[Int],
  elevator: Boolean): Room = {
    new Room(sectorGroup, highDoors.map(d => (d, true)).toMap, lowDoors.map(d => (d, true)).toMap, elevator)
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
}

case class Room(
  sectorGroup: SectorGroup,
  highDoors: Map[Int, Boolean],
  lowDoors: Map[Int, Boolean],
  elevator: Boolean = false
) {

  def hasLowDoor(direction: Int): Boolean = lowDoors.get(direction).getOrElse(false)
  def hasHighDoor(direction: Int): Boolean = highDoors.get(direction).getOrElse(false)

  def flipY: Room = {
    Room(
      sectorGroup.flippedY(),
      Room.flipY(highDoors),
      Room.flipY(lowDoors),
      elevator
    )
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

  // 111 - roof antenna
  // 11101 - connector for roof antenna
  // 112 - roof antenna bottom

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

  // Connectors
  // 123 - ELEVATOR


  // note:  6 large squares ( 1042 ) seem like a good size.

  def run2(sourceMap: DMap): DMap = {
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true);
    val builder = new Hyper2MapBuilder(DMap.createNew(), palette)

    val sg = palette.getSectorGroup(109)
    val sg2 = palette.getSectorGroup(1091)

    val basicElevator = palette.getSectorGroup(1101)
    // val basicEmpty = palette.getSectorGroup(1102).flippedY(0)
    // val basicRoom = palette.getSectorGroup(110).flippedY()
    // val basicEmpty = palette.getSectorGroup(1102).flippedX(0)
    // val basicRoom = palette.getSectorGroup(110).flippedX()
    val basicEmpty = palette.getSectorGroup(1102).flippedY(0).flippedX(0)
    val basicRoom = palette.getSectorGroup(110).flippedY().flippedX()

    // val room = basicRoom.connectedTo(
    //   basicRoom.getRedwallConnector(123),
    //   basicEmpty, //basicElevator,
    //   //basicElevator.getRedwallConnector(123));
    //   basicEmpty.getRedwallConnector(123));

    val room = basicRoom.connectedTo(123, basicEmpty)
    builder.placeAnywhere(room);

    //val sg3 = sg.connectedTo(
    //  sg.getConnector(123).asInstanceOf[RedwallConnector],
    //  sg2,
    //  sg2.getConnector(124).asInstanceOf[RedwallConnector])
    //builder.placeAnywhere(sg3)


    builder.setAnyPlayerStart()

    //builder.setPlayerStart((0, 0, 0, 0))
    builder.clearMarkers()
    builder.outMap
  }


  def run(sourceMap: DMap): DMap = {
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true);
    val builder = new Hyper2MapBuilder(DMap.createNew(), palette)

    val roomBottomRight = Room(
      palette.getSectorGroup(100),
      Map[Int, Boolean](),
      Map((SimpleConnector.Direction.WEST -> true), (SimpleConnector.Direction.NORTH -> true)),
      true
    )
    val roomBottomLeft = Room(
      palette.getSectorGroup(100).flippedX(),
      Map[Int, Boolean](),
      Map((SimpleConnector.Direction.EAST -> true), (SimpleConnector.Direction.NORTH -> true)),
      true
    )
    val roomTopLeft = Room(
      palette.getSectorGroup(100).flippedX().flippedY(),
      Seq(),
      Seq(Heading.E, Heading.S),
      true
    )

    val emptySg = palette.getSectorGroup(1102).flippedX(0).flippedY(0)
    val modularRoom = palette.getSectorGroup(110).flippedX().flippedY()
    // val emptySg = palette.getSectorGroup(1102).flippedY(0)
    // val modularRoom = palette.getSectorGroup(110).flippedY()

    //val roomTopLeftNoElevator = modularRoom
    val roomTopLeftNoElevator = Room(
      modularRoom.connectedTo(123, emptySg),
      Seq(),
      Seq(Heading.E, Heading.S),
      false
    )


    val roomTopRight = Room(
      palette.getSectorGroup(100).flippedY(),
      Seq(),
      Seq(Heading.W, Heading.S),
      true
    )

    val circleRoom = Room(
      palette.getSectorGroup(104),
      SimpleConnector.Direction.all.asScala.map(d => (d.toInt, true)).toMap,
      SimpleConnector.Direction.all.asScala.map(d => (d.toInt, true)).toMap
    )

    val roomWithView = Room(
      palette.getSectorGroup(105),
      Map[Int, Boolean](),
      Map((SimpleConnector.Direction.WEST, true), (SimpleConnector.Direction.NORTH, true))
    )

    val poolRoom = Room(
      palette.getSectorGroup(101),
      Heading.all.asScala.map(d => (d.toInt, true)).toMap,
      Map[Int, Boolean]()
    )

    val commandCenter = Room(
      palette.getSectorGroup(106),
      Seq(),
      Seq(Heading.E, Heading.W, Heading.N),
      false
    )

    val habitat = Room(
      palette.getSectorGroup(107),
      Seq(),
      Seq(Heading.W),
      true
    )

    val trainStop = Room(palette.getSectorGroup(108), Seq(), Seq(
      Heading.EAST, Heading.SOUTH
    ), false)


    val ELEVATOR_GROUP = 1101
    val dishRoof = Room(
      palette.getSectorGroup(111).connectedTo(11101, palette.getSectorGroup(112)).connectedTo(123, palette.getSectorGroup(ELEVATOR_GROUP)),
      Seq(Heading.S, Heading.W),
      Seq(),
      true) // TODO - add an elevator to a lower level


    // TODO - anchor sprite removal with connectedTo() is not working

    // val eastWestHallway = palette.getSectorGroup(200)
    // def westConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.WestConnector).asInstanceOf[RedwallConnector]
    // println(s"hallway anchor point: ${westConnector(eastWestHallway).getAnchorPoint}")
    // println(s"room anchor point: ${westConnector(palette.getSectorGroup(100)).getAnchorPoint}")

    // BOTTOM FLOOR
    //builder.addRoom(roomTopLeft, (0, 0, 0, 0))
    builder.addRoom(trainStop, (0, 0, 0, 0))
    builder.addRoom(habitat, (1, 0, 0, 0))
    //builder.addRoom(circleRoom, (0, 1, 0, 0))
    builder.addRoom(roomBottomLeft, (0, 1, 0, 0))
    builder.addRoom(poolRoom, (1, 1, 0, 0))

    // TOP FLOOR
    //builder.addRoom(roomTopLeftNoElevator, (0, 0, 1, 0))
    //builder.addRoom(roomTopLeft, (0, 0, 1, 0))
    builder.addRoom(commandCenter.flipY, (0, 0, 1, 0))
    //builder.addRoom(roomTopRight, (1, 0, 1, 0))  // TOP RIGHT
    builder.addRoom(dishRoof, (1, 0, 1, 0)) // TOP RIGHT

    //builder.addRoom(commandCenter, (0, 1, 1, 0))  // BOTTOM LEFT
    builder.addRoom(roomBottomLeft, (0, 1, 1, 0))
    builder.addRoom(roomWithView, (1, 1, 1, 0))   // BOTTOM RIGHT


    builder.autoLinkRooms()

    builder.setPlayerStart((0, 0, 0, 0))
    builder.clearMarkers()
    builder.outMap
  }

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



    //val topPsg = builder.addRoom(poolRoomTop, (1, 0, 0, 0))
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
