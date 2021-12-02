package trn.prefab.experiments

import trn.prefab._
import trn.{HardcodedConfig, Main, MapLoader, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, Map => DMap}
import trn.MapImplicits._
import trn.duke.{PaletteList, TextureList}
import trn.prefab.experiments.Hyper2MapBuilder.Cell

import scala.collection.JavaConverters._

class GridManagerV2(
  val origin: PointXYZ,
  val cellDist: Int,
  val maxGrid: Int
) {
  def toCoordinates(gridCell: (Int, Int, Int, Int)): PointXYZ = {
    // offsets must shift the max width of the grid for each value
    val offsetForZ = gridCell._3 * cellDist * (maxGrid + 1)
    val offsetForW = gridCell._4 * cellDist * (maxGrid + 1) // TODO - actually we'll want W in the same place ...
    val x = gridCell._1 * cellDist + offsetForW
    val y = gridCell._2 * cellDist + offsetForZ

    // NOTE:  negative z is higher.  Also need to shift z
    val z = -1 * (gridCell._3 << 4) * cellDist + 1024 // raise it, so elevators work
    origin.add(new PointXYZ(x, y, z))
  }
}

object Hyper2MapBuilder {
  type Cell = (Int, Int, Int, Int)
}

class Hyper2MapBuilder(val outMap: DMap, palette: PrefabPalette, val gameCfg: GameConfig) extends MapBuilder {
  val writer = new MapWriter(this, sgBuilder) // TODO

  val grid = scala.collection.mutable.Map[Cell, PastedSectorGroup]()
  val grid2 = scala.collection.mutable.Map[Cell, Room]()
  val margin: Int = 10 * 1024

  // rooms are six big grid cells wide and anchor is in the middle
  // hallways are one grid cell wide
  val gridManager = new GridManagerV2(
    new PointXYZ(
      DMap.MIN_X + margin + (14 * 1024),  // extra for the train
      DMap.MIN_Y + margin, 0
    ),
    cellDist = (6 + 1) * 1024,
    maxGrid = 2 // max number of rooms, used to offset z and w
  )

  // tracks locations for placing "floating" sectors that can go anywhere, like underwater areas.
  val sgPacker: SectorGroupPacker = new SimpleSectorGroupPacker(
    new PointXY(DMap.MIN_X, 0),
    new PointXY(DMap.MAX_X, DMap.MAX_Y),
    512)

  // def placeAnywhere(sg: SectorGroup): PastedSectorGroup = {
  //   val topLeft = sgPacker.reserveArea(sg)
  //   val tr = sg.boundingBox.getTranslateTo(topLeft).withZ(0)
  //   pasteSectorGroup(sg, tr)
  // }

  def setPlayerStart(cell: Cell): Unit = writer.setPlayerStart(grid(cell))

  // new version
  def addRoom(room: Room, gridCell: (Int, Int, Int, Int)): PastedSectorGroup = {
    if(grid.get(gridCell).nonEmpty) throw new IllegalArgumentException("that cell already taken")
    if(grid2.get(gridCell).nonEmpty) throw new IllegalArgumentException("that cell already taken")
    val anchor = room.sectorGroup.getAnchor.withZ(0)
    val tr = anchor.getTransformTo(gridManager.toCoordinates(gridCell))
    val psg = writer.pasteSectorGroup(room.sectorGroup, tr)
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
          writer.linkElevators(
            grid(bottomCell).getFirstElevatorConnector,
            grid(topCell).getFirstElevatorConnector,
            elevatorStartsLower
          )
          // ElevatorConnector.linkElevators(
          //   grid(bottomCell).getFirstElevatorConnector,
          //   this,
          //   grid(topCell).getFirstElevatorConnector,
          //   this,
          //   nextUniqueHiTag(),
          //   elevatorStartsLower)
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

      val hallway = if(westCell._4 < 1){ palette.getSectorGroup(206) }else{ palette.getSectorGroup(200) }
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
        } else if(northRoom.hasLowDoor(Heading.S) && southRoom.hasHighDoor(Heading.N)) {
          val elevator: SectorGroup = palette.getSectorGroup(203).flippedY(0)
          placeHallwayNS(grid(northCell), elevator, grid(southCell))
          true
        } else {
          println(s"WARNING: Missing hallway to connect ${northCell} and ${southCell}")
          false
        }
      }
      case _ => false
    }
  }

  def placeHallwayEW(leftRoom: PastedSectorGroup, hallway: SectorGroup, rightRoom: PastedSectorGroup): PastedSectorGroup = {
    val leftConn = leftRoom.findFirstConnector(RedConnUtil.EastConnector).asInstanceOf[RedwallConnector]
    val rightConn = rightRoom.findFirstConnector(RedConnUtil.WestConnector).asInstanceOf[RedwallConnector]
    val cdelta: PointXYZ = if(leftConn.getAnchorPoint.z < rightConn.getAnchorPoint.z){
      CompassWriter.westConnector(hallway).getTransformTo(leftConn)
    }else{
      CompassWriter.eastConnector(hallway).getTransformTo(rightConn)
    }
    val pastedHallway = writer.pasteSectorGroup(hallway, cdelta)
    sgBuilder.linkConnectors(CompassWriter.westConnector(pastedHallway), leftConn)
    sgBuilder.linkConnectors(CompassWriter.eastConnector(pastedHallway), rightConn)
    pastedHallway
  }

  def placeHallwayNS(topRoom: PastedSectorGroup, hallway: SectorGroup, bottomRoom: PastedSectorGroup): PastedSectorGroup = {
    val topConn = CompassWriter.southConnector(topRoom)
    val bottomConn = CompassWriter.northConnector(bottomRoom)
    val cdelta: PointXYZ = if(topConn.getAnchorPoint.z < bottomConn.getAnchorPoint.z){
      CompassWriter.northConnector(hallway).getTransformTo(topConn)
    } else {
      CompassWriter.southConnector(hallway).getTransformTo(bottomConn)
    }
    val pastedHallway = writer.pasteSectorGroup(hallway, cdelta)
    sgBuilder.linkConnectors(CompassWriter.northConnector(pastedHallway), topConn)
    sgBuilder.linkConnectors(CompassWriter.southConnector(pastedHallway), bottomConn)
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
    def hasHeading(h: Int): Boolean  = sectorGroup.getRedwallConnectors(RedConnUtil.connectorTypeForHeading(h)) match {
      case x: Seq[RedwallConnector] => {
        //x.find(_.totalManhattanLength(sectorGroup.getMap) == standardDoorLength).nonEmpty
        x.find(_.totalManhattanLength(sectorGroup) == standardDoorLength).nonEmpty
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

  def rotateCW(doors: Map[Int, Boolean]): Map[Int, Boolean] = {
    doors.map{ d =>
      val i = Heading.rotateCW(d._1)
      if(i == d._1) throw new RuntimeException
      (Heading.rotateCW(d._1), d._2)
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

  def withoutLowDoor(direction: Int): Room = {
    Room(sectorGroup, highDoors, lowDoors - direction, elevator, teleporter)
  }
  def withLowDoors(lowDoors2: Map[Int, Boolean]): Room = this.copy(lowDoors = lowDoors2)
  def withHighDoors(highDoors2: Map[Int, Boolean]): Room = this.copy(highDoors = highDoors2)

  def flipY: Room = {
    Room(sectorGroup.flippedY(), Room.flipY(highDoors), Room.flipY(lowDoors), elevator, teleporter)
  }
  def flipX: Room = {
    Room(sectorGroup.flippedX(), Room.flipX(highDoors), Room.flipX(lowDoors), elevator, teleporter)
  }
  def rotateCW: Room = {
    val sg = sectorGroup.rotateAroundCW(sectorGroup.getAnchor.asXY())
    Room(sg, Room.rotateCW(highDoors), Room.rotateCW(lowDoors), elevator, teleporter)
  }

  def rotateCCW: Room = this.rotateCW.rotateCW.rotateCW

  def paintedRed: Room = this.copy (
    sectorGroup = MapWriter.painted(sectorGroup, PaletteList.BLUE_TO_RED, Seq(225, 229))
  )

  def withCarpet: Room = this.copy (
    sectorGroup = sectorGroup.withModifiedSectors{ s =>
      if(s.getFloorTexture == 181){
        s.setFloorTexture(898)
      }
    }
  )
}

object Hypercube2 {
  val BasicRoom = 100
  val PoolRoomTop = 101
  // 102 - pool room bottom
  // 103 - reactor room top
  val CircularRoom = 104 // 104 - circular nuke sign room (can connect on both top or bottom)
  val RoomWithView = 105 // must be top level
  val CommandCenter = 106
  val Habitat = 107
  val TrainStation = 108

  // 109 - testing double red wall
  // 1091 - other room for testing double red wall

  // 110 - basic room that can be assembled (high version is 118)
  // 1101 - basic room assembly - elevator
  // 1102 - basic room assembly - empty
  // 1103 - basic room assembly - teleporter
  // 1104 - basic room assembly - teleporter empty

  // Modular Connectors
  // 123 - ELEVATOR
  // 124 - (test connector for multi redwall)
  // 125 - TELEPORTER

  val RoofAntenna = 111
  val MedicalBay = 113
  val EndTrain = 114
  val SuperComputerRoom = 115
  val PlantRoom = 116
  val UpperRoomWithView = 117
  val ModularRoomHigh = 118

  // Intra-sector connectors
  // 150 - elevator sector

  // Connection Groups
  //
  // 200 - east-west hallway
  // 201 - north-south hallway
  // 202 - east-west simple elevator, with east=high part
  // 203 - low south to night north elevator
  // 204 - hallway for troubleshooting
  // 205 - some other hallway for troubleshooting (probably unused)
  // 206 - locked hallway

  // More underwater stuff
  // 301 - underwater connector
  // 303 - reactor underwater



  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    // val writer = MapWriter(gameCfg)
    val palette = MapLoader.loadPalette(HardcodedConfig.getEduke32Path(Filename))
    try {
      val outMap = run(gameCfg, palette)
      val writer = MapWriter(outMap, gameCfg)
      ExpUtil.finishAndWrite(writer, forcePlayerStart = false)
    } catch {
      case e => {
        // writer.setAnyPlayerStart(true)
        // Main.deployTest(writer.outMap, "error.map", HardcodedConfig.getEduke32Path("error.map"))
        throw e
      }
    }
  }

  //def run(sourceMap: DMap): DMap = {
  def run(gameCfg: GameConfig, palette: PrefabPalette): DMap = {
    val builder = new Hyper2MapBuilder(DMap.createNew(), palette, gameCfg)

    def modularRoom2(sg: SectorGroup, x: Int, y: Int, elevator: Boolean, teleporter: Boolean, w: Int = 0): Room = {
      val lowDoors = Seq(Heading.W, Heading.N)
      val teleportId = if(teleporter){ 1103 }else{ 1104 } // 1104 is teleporter empty
      val elevatorId = if(elevator){ 1101 }else{ 1102 }
      //val sg = palette.getSectorGroup(110).connectedTo(125, palette.getSectorGroup(teleportId))
      //val sg1 = sg.connectedTo(125, palette.getSectorGroup(teleportId))
      val sg1 = MapWriter.connected(sg, palette.getSectorGroup(teleportId), 125, gameCfg)
      // val sg2 = sg1.connectedTo(123, palette.getSectorGroup(elevatorId))
      val sg2 = MapWriter.connected(sg1, palette.getSectorGroup(elevatorId), 123, gameCfg)
      val sg3 = w match {
        case 0 => sg2
        case 1 => {
          val g = sg2.copy()
          g.allWalls.foreach{ w =>
            if(w.getTexture != 225 && w.getTexture != 229){
              w.setPal(PaletteList.BLUE_TO_RED)
            }
          }
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
    def modularRoom(x: Int, y: Int, elevator: Boolean, teleporter: Boolean, w: Int = 0): Room = {
      val sg = palette.getSectorGroup(110)
      modularRoom2(sg, x, y, elevator, teleporter, w)
    }

    def modularRoomHigh(x: Int, y: Int, elevator: Boolean, teleporter: Boolean, w: Int = 0): Room = {
      val sg = palette.getSectorGroup(ModularRoomHigh)
      val r = modularRoom2(sg, x, y, elevator, teleporter, w)
      r.withHighDoors(r.lowDoors).withLowDoors(Map())
    }

    val allDoors: Seq[Int] = Heading.all.asScala.map(_.toInt)
    // val circleRoom = Room.auto(palette.getSectorGroup(CircularRoom), allDoors, allDoors)
    val roomWithView = Room(palette.getSectorGroup(RoomWithView), Seq(), Seq(Heading.W, Heading.N), false, false)
    // val poolRoom = Room.auto(palette.getSectorGroup(PoolRoomTop), allDoors, Seq())
    val commandCenter = Room.auto(palette.getSG(CommandCenter), Seq(), Room.autoDoors(palette.getSectorGroup(106)))
    // val habitat = Room.auto(palette.getSectorGroup(Habitat), Seq(), Seq(Heading.W))
    val trainStop = Room(palette.getSectorGroup(TrainStation), Seq(), Seq( Heading.EAST, Heading.SOUTH ), false, false)

    val ELEVATOR_GROUP = 1101
    // val dishSg = palette.getSectorGroup(RoofAntenna).connectedTo(123, palette.getSectorGroup(ELEVATOR_GROUP))
    val dishSg = MapWriter.connected(palette.getSectorGroup(RoofAntenna), palette.getSectorGroup(ELEVATOR_GROUP), 123, gameCfg)
    val dishRoof = Room.auto(dishSg, Seq(Heading.S, Heading.W), Seq())

    val medicalBay = Room(palette.getSectorGroup(MedicalBay), Seq(), Seq(Heading.S), false, false)
    val endTrain = Room(palette.getSectorGroup(EndTrain), Seq(), Seq(Heading.S), false, false)
    val computerRoom = Room(palette.getSectorGroup(SuperComputerRoom), Seq(Heading.W), Seq(Heading.S), true, false)
    val plantRoom = Room(palette.getSectorGroup(PlantRoom), allDoors, Seq(), false, false)
    val highViewRoom = Room(palette.getSectorGroup(UpperRoomWithView), Seq(Heading.N), Seq(), false, true)

    // BOTTOM FLOOR
    builder.addRoom(trainStop, (0, 0, 0, 0))
    builder.addRoom(modularRoom(1, 0, true, true), (1, 0, 0, 0))
    builder.addRoom(modularRoom(0, 1, true, false), (0, 1, 0, 0))
    builder.addRoom(commandCenter, (1, 1, 0, 0))

    // TOP FLOOR
    builder.addRoom(roomWithView.rotateCW.rotateCW.withCarpet, (0, 0, 1, 0))
    builder.addRoom(modularRoom(1, 0, true, false).withCarpet, (1, 0, 1, 0)) // TOP RIGHT  -- TODO - will be armory
    builder.addRoom(modularRoom(0, 1, true, true).withCarpet, (0, 1, 1, 0)) // BOTTOM LEFT
    builder.addRoom(medicalBay.rotateCW, (1, 1, 1, 0))   // BOTTOM RIGHT

    // W+ ------------------------------------

    // BOTTOM FLOOR
    builder.addRoom(computerRoom.rotateCCW.paintedRed, (0, 0, 0, 1))  // TOP LEFT
    builder.addRoom(modularRoom(1, 0, false, true, w=1).withoutLowDoor(Heading.S), (1, 0, 0, 1))
    builder.addRoom(plantRoom.rotateCW.rotateCW, (0, 1, 0, 1))
    builder.addRoom(modularRoomHigh(1, 1, true, false, w=1), (1, 1, 0, 1))

    // TOP FLOOR
    builder.addRoom(dishRoof.rotateCCW, (0, 0, 1, 1))  // TOP LEFT
    builder.addRoom(endTrain, (1, 0, 1, 1))
    builder.addRoom(highViewRoom.paintedRed.withCarpet, (0, 1, 1, 1))
    builder.addRoom(modularRoom(1, 1, true, false, w=1), (1, 1, 1, 1))

    builder.autoLinkRooms()

    builder.writer.applyPaletteToAll(TextureList.SKIES.MOON_SKY, PaletteList.ALSO_NORMAL)
    builder.setPlayerStart((0, 0, 0, 0)) //builder.setAnyPlayerStart()
    builder.writer.clearMarkers()
    builder.outMap

  }

  def Filename: String = "hyper2.map"

}
