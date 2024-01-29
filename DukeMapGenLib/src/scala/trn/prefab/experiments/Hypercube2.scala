package trn.prefab.experiments

import trn.prefab._
import trn.{HardcodedConfig, ScalaMapLoader, Main, BuildConstants, PointXYZ, Map => DMap}
import trn.duke.{PaletteList, TextureList}
import trn.logic.Tile2d
import trn.prefab.experiments.Hyper2MapBuilder.Cell

import scala.collection.JavaConverters._

class GridManagerV2(
  val origin: PointXYZ,
  val cellDist: Int,
  val maxGridY: Int,
  val maxGridX: Int,
) extends GridManager4D {
  def toCoordinates(gridCell: (Int, Int, Int, Int)): PointXYZ = {
    // offsets must shift the max width of the grid for each value
    val offsetForZ = gridCell._3 * cellDist * (maxGridY + 1)
    val offsetForW = gridCell._4 * cellDist * (maxGridX + 1) // TODO - actually we'll want W in the same place ...
    val x = gridCell._1 * cellDist + offsetForW
    val y = gridCell._2 * cellDist + offsetForZ

    // NOTE:  negative z is higher.  Also need to shift z
    val z = -1 * (gridCell._3 << 4) * cellDist + 1024 // raise it, so elevators work
    origin.add(new PointXYZ(x, y, z))
  }

  override def toXYZ(gridCell: (Int, Int, Int, Int)): PointXYZ = toCoordinates(gridCell)
}

object Hyper2MapBuilder {
  type Cell = (Int, Int, Int, Int)
}

class Hyper2MapBuilder(val outMap: DMap, palette: PrefabPalette, val gameCfg: GameConfig, gridManager: GridManager4D) extends MapBuilder {
  val writer = new MapWriter(this, sgBuilder)
  val grid = scala.collection.mutable.Map[Cell, PastedSectorGroup]()
  val grid2 = scala.collection.mutable.Map[Cell, Room]()

  def addRoom(room: Room, gridCell: (Int, Int, Int, Int)): PastedSectorGroup = {
    if(grid.get(gridCell).nonEmpty) throw new IllegalArgumentException("that cell already taken")
    if(grid2.get(gridCell).nonEmpty) throw new IllegalArgumentException("that cell already taken")

    //val anchor = room.sectorGroup.getAnchor.withZ(0)
    val tr = room.anchor.getTransformTo(gridManager.toXYZ(gridCell))
    val (psg, _) = writer.pasteSectorGroup2(room.sectorGroup, tr, Seq.empty, false)
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
    // water needs to be in a second pass to avoid overwriting the wrong teleporters
    grid2.foreach { case (gridCell, _) =>
      val (x, y, z, w) = gridCell
      val other= (x, y, z + 1, w)
      (grid.get(gridCell), grid.get(other)) match {
        case (Some(psg1), Some(psg2)) => HyperUtil.tryLinkTeleporters(writer, psg1, psg2)
        case _ =>
      }
    }
  }

  def tryAutoLinkTeleporer(cell1: Cell, cell2: Cell): Boolean = {
    (grid2.get(cell1), grid2.get(cell2)) match {
      case (Some(_), Some(_)) => HyperUtil.tryLinkTeleporters(writer, grid(cell1), grid(cell2))
      case _ => false
    }
  }

  def tryAutoLinkElevator(bottomCell: Cell, topCell: Cell, elevatorStartsLower: Boolean = true): Unit = {
    (grid2.get(bottomCell), grid2.get(topCell)) match {
      case (Some(bottomRoom), Some(topRoom)) => HyperUtil.tryLinkAllElevators(writer, grid(bottomCell), grid(topCell))
      case _ => {}
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
    val leftConn = leftRoom.getCompassConnectors(Heading.E).head
    val rightConn = rightRoom.getCompassConnectors(Heading.W).head
    val cdelta: PointXYZ = if(leftConn.getAnchorPoint.z < rightConn.getAnchorPoint.z){
      CompassWriter.westConnector(hallway).getTransformTo(leftConn)
    }else{
      CompassWriter.eastConnector(hallway).getTransformTo(rightConn)
    }
    val (pastedHallway, _) = writer.pasteSectorGroup2(hallway, cdelta, Seq.empty, false)
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
    val (pastedHallway, _) = writer.pasteSectorGroup2(hallway, cdelta, Seq.empty, false)
    sgBuilder.linkConnectors(CompassWriter.northConnector(pastedHallway), topConn)
    sgBuilder.linkConnectors(CompassWriter.southConnector(pastedHallway), bottomConn)
    pastedHallway
  }
}


object Room {
  val StandardDoorLength = 2048
  val RoomSize = 6 * 1024

  def apply(
    sectorGroup: SectorGroup,
    highDoors: Seq[Int],
    lowDoors: Seq[Int],
    elevator: Boolean,
    teleporter: Boolean): Room = {
    apply2(sectorGroup, highDoors.map(d => (d, true)).toMap, lowDoors.map(d => (d, true)).toMap, elevator, teleporter)
  }

  def apply2(
    sectorGroup: SectorGroup,
    highDoors: Map[Int, Boolean],
    lowDoors: Map[Int, Boolean],
    elevator: Boolean,
    teleporter: Boolean,
  ): Room = {
    val highDoors2 = Room.toTile(highDoors)
    val lowDoors2 = Room.toTile(lowDoors)
    new Room(sectorGroup, highDoors2, lowDoors2, elevator, teleporter)
  }

  def autoDoors(sectorGroup: SectorGroup): Seq[Int] = {
    def hasHeading(h: Int): Boolean = sectorGroup.getCompassConnectors(h).exists(_.totalManhattanLength(sectorGroup) == StandardDoorLength)
    val headings: Seq[Int] = Heading.all.asScala.flatMap(h => if(hasHeading(h)){ Some(h.toInt)}else{ None })
    headings
  }

  def auto(sectorGroup: SectorGroup, highDoors: Seq[Int], lowDoors: Seq[Int]): Room = {
    // TODO - maybe the best thing is for the rooms to not even understand high vs low and do it at a lower level...
    val hasTeleport: Boolean = sectorGroup.getTeleportConnectors().filter(t => !t.isWater).size > 0
    val hasElevator: Boolean = sectorGroup.getElevatorConnectors().size > 0
    Room(sectorGroup, highDoors, lowDoors, hasElevator, hasTeleport)
  }

  def toTile(doors: Map[Int, Boolean]): Tile2d = Tile2d(
    doors.get(Heading.E).getOrElse(false),
    doors.get(Heading.S).getOrElse(false),
    doors.get(Heading.W).getOrElse(false),
    doors.get(Heading.N).getOrElse(false),
  )

  /** @return true if the redwall connector is a room "door" */
  def isDoor(sg: SectorGroup, conn: RedwallConnector, standardDoorLength: Int): Boolean = {
    val w = sg.getMap.getWallView(conn.getWallIds.get(0)).getLineSegment
    conn.totalManhattanLength == standardDoorLength && conn.getWallIds.size() == 1 && w.isAxisAligned
  }

}

case class Room(
  sectorGroup: SectorGroup,
  highDoors: Tile2d,
  lowDoors: Tile2d,
  elevator: Boolean = false,
  teleporter: Boolean = false
) {

  def anchor: PointXYZ = if(sectorGroup.getGroupId == 122) {
    sectorGroup.getAnchor.withZ(sectorGroup.getAnchor.z + 24 * BuildConstants.ZStepHeight)
  }else {
    // TODO we are hardcoding the Z to 0, which means we dont properly translate Z
    sectorGroup.getAnchor.withZ(0)
  }

  def hasLowDoor(direction: Int): Boolean = lowDoors.side(direction) == Tile2d.Conn
  def hasHighDoor(direction: Int): Boolean = highDoors.side(direction) == Tile2d.Conn

  // def withoutLowDoor(direction: Int): Room = {
  //   Room(sectorGroup, highDoors, lowDoors.withSide(direction, Tile2d.Blocked), elevator, teleporter)
  // }
  // def withLowDoors(lowDoors2: Tile2d): Room = this.copy(lowDoors = lowDoors2)
  // def withHighDoors(highDoors2: Tile2d): Room = this.copy(highDoors = highDoors2)

  def flipY: Room = {
    Room(sectorGroup.flippedY(), highDoors.flippedY, lowDoors.flippedY, elevator, teleporter)
  }
  def flipX: Room = {
    Room(sectorGroup.flippedX(), highDoors.flippedX, lowDoors.flippedX, elevator, teleporter)
  }
  def rotateCW: Room = {
    val sg = sectorGroup.rotateAroundCW(sectorGroup.getAnchor.asXY())
    Room(sg, highDoors.rotatedCW, lowDoors.rotatedCW, elevator, teleporter)
  }

  def rotateCCW: Room = this.rotateCW.rotateCW.rotateCW

  def paintedForW(w: Int): Room = {
    if(w == 1){
      val sg = this.sectorGroup
      val g = sg.withTexturesReplaced(Map(898 -> 899)).copy()
      g.allWalls.foreach{ w =>
        if(w.getTexture != 225 && w.getTexture != 229){
          w.setPal(PaletteList.BLUE_TO_RED)
        }
      }
      this.copy(sectorGroup = MapWriter.painted(g, PaletteList.BLUE_TO_RED, Seq(225, 229)))
    }else{
      this
    }
  }

  // needs to be done before paintedForW
  def paintedForZ(z: Int): Room = if(z == 1){
    this.copy(sectorGroup = sectorGroup.withTexturesReplaced(Map(181 -> 898)))
  }else{
    this
  }

}

object Hypercube2 {

  val UpperRoomTag = 1

  val BasicRoom = 100
  // val PoolRoomTop = 101
  // 102 - pool room bottom
  // 103 - reactor room top
  val CircularRoom = 104 // 104 - circular nuke sign room (can connect on both top or bottom)
  val RoomWithView = 105 // must be top level
  val CommandCenter = 106
  val Habitat = 107
  val TrainStation = 108
  val TvRoom = 109
  // 110 - basic room that can be assembled (high version is 118)
  // 1101 - basic room assembly - elevator
  // 1102 - basic room assembly - empty
  // 1103 - basic room assembly - teleporter
  // 1104 - basic room assembly - teleporter empty
  val RoofAntenna = 111
  val MedicalBay = 113
  val EndTrain = 114
  val SuperComputerRoom = 115
  val PlantRoom = 116
  val UpperRoomLongWindow = 117
  // val ModularRoomHigh = 118  No Longer Used
  val PenultimateRoom = 119
  val RoomWithViewAndStuff = 120
  val TankTop = 121
  val CrateRoom = 122
  val TankBottom = 123

  // Modular Connectors
  // 123 - ELEVATOR
  // 125 - TELEPORTER
  // 150 - elevator sector
  // Connection Groups
  // 200 - east-west hallway
  // 201 - north-south hallway
  // 202 - east-west simple elevator, with east=high part
  // 203 - low south to night north elevator
  // 204 - hallway for troubleshooting
  // 205 - some other hallway for troubleshooting (probably unused)
  // 206 - locked hallway
  // 301 - underwater connector
  // 303 - reactor underwater

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    // val writer = MapWriter(gameCfg)
    val palette = ScalaMapLoader.loadPalette(HardcodedConfig.getEduke32Path(Filename))

    // rooms are six big grid cells wide and anchor is in the middle
    // hallways are one grid cell wide
    val margin: Int = 10 * 1024
    val gridManager = new GridManagerV2(
      new PointXYZ(
        DMap.MIN_X + margin + (14 * 1024),  // extra for the train
        DMap.MIN_Y + margin, 0
      ),
      cellDist = Room.RoomSize + 1024, // need 1024 between edges of rooms for the doors
      maxGridX = 2, // max number of rooms, used to offset z and w
      maxGridY = 2,
    )

    val builder = new Hyper2MapBuilder(DMap.createNew(), palette, gameCfg, gridManager)

    try {
      val outMap = run(gameCfg, palette, builder)
      val writer = MapWriter(outMap, gameCfg)
      ExpUtil.finishAndWrite(writer, forcePlayerStart = false)
    } catch {
      case e => {
        builder.writer.setAnyPlayerStart(true)
        Main.deployTest(builder.writer.outMap, "error.map", HardcodedConfig.getEduke32Path("error.map"))
        throw e
      }
    }
  }


  //def run(sourceMap: DMap): DMap = {
  def run(gameCfg: GameConfig, palette: PrefabPalette, builder: Hyper2MapBuilder): DMap = {


    def modularRoom2(sg: SectorGroup, x: Int, y: Int, elevator: Boolean, teleporter: Boolean, w: Int = 0): Room = {
      val lowDoors = Seq(Heading.W, Heading.N)
      val teleportId = if(teleporter){ 1103 }else{ 1104 } // 1104 is teleporter empty
      val elevatorId = if(elevator){ 1101 }else{ 1102 }
      val sg1 = MapWriter.connected(sg, palette.getSectorGroup(teleportId), 125, gameCfg)
      val sg2 = MapWriter.connected(sg1, palette.getSectorGroup(elevatorId), 123, gameCfg)
      val sg3 = w match {
        case 0 => sg2
        case 1 => sg2
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

    def autoDoors(sg: SectorGroup, upperRoomOnly: Boolean): (Seq[Int], Seq[Int]) = {
      def connectorSectorZ(sg: SectorGroup, c: RedwallConnector): Int = {
        sg.getMap.getSector(c.getSectorIds.get(0)).getFloorZ
      }
      def toHeadings(conns: Seq[RedwallConnector]) =  Heading.all.asScala.flatMap { heading =>
        conns.find(c => c.isCompassConn(heading)).map(_ => heading.intValue)
      }
      val allDoors = sg.allRedwallConnectors.filter { c =>
        Room.isDoor(sg, c, Room.StandardDoorLength)
      }
      if(upperRoomOnly){
        (Seq.empty, toHeadings(allDoors)) // (lowDoors, highDoors)
      }else{
        val low = allDoors.filter(c => connectorSectorZ(sg, c) == sg.getAnchor.z)
        val high = allDoors.filter(c => connectorSectorZ(sg, c) != sg.getAnchor.z)
        (toHeadings(low), toHeadings(high))
      }
    }

    def loadRoom(sectorGroupId: Int): Room = {
      val sg = palette.getSectorGroup(sectorGroupId)
      loadRoom2(sg)
    }
    def loadRoom2(sg: SectorGroup): Room = {
      val upperRoomOnly = sg.containsSprite(s => PrefabUtils.isMarker(s, UpperRoomTag, Marker.Lotags.ALGO_HINT))
      val (lowDoors, highDoors) = autoDoors(sg, upperRoomOnly)
      Room.auto(sg, highDoors, lowDoors)
    }

    val trainStop = loadRoom(TrainStation)
    val commandCenter = loadRoom(CommandCenter)

    val ELEVATOR_GROUP = 1101
    val dishSg = MapWriter.connected(palette.getSectorGroup(RoofAntenna), palette.getSectorGroup(ELEVATOR_GROUP), 123, gameCfg)
    val dishRoof = Room.auto(dishSg, Seq(Heading.S, Heading.W), Seq())
    // TODO this fails badly:
    // val dishRoof = loadRoom2(dishSg) // Room.auto(dishSg, Seq(Heading.S, Heading.W), Seq())

    val medicalBay = loadRoom(MedicalBay)
    val endTrain = loadRoom(EndTrain)
    val computerRoom = loadRoom(SuperComputerRoom)
    val plantRoom = loadRoom(PlantRoom)
    val longWindowRoom = loadRoom(UpperRoomLongWindow)
    val roomWithView = loadRoom(RoomWithView) // Room(palette.getSectorGroup(RoomWithView), Seq(), Seq(Heading.W, Heading.N), false, false)

    val hardcodedRooms = Map[(Int, Int, Int, Int), Room](
      // BOTTOM FLOOR
      (0, 0, 0, 0) -> trainStop,
      (1, 0, 0, 0) -> loadRoom(TankBottom), // modularRoom(1, 0, true, true),   // Tank Bottom
      (0, 1, 0, 0) -> modularRoom(0, 1, true, false),
      (1, 1, 0, 0) -> commandCenter,
      // TOP FLOOR
      (0, 0, 1, 0) -> roomWithView.rotateCW.rotateCW,
      // (1, 0, 1, 0) -> modularRoom2(palette.getSG(TankTop), 1, 0, true, false), // tank top
      (1, 0, 1, 0) -> loadRoom(TankTop),
      (0, 1, 1, 0) -> loadRoom(RoomWithViewAndStuff),
      (1, 1, 1, 0) -> medicalBay.rotateCW,
      // BOTTOM FLOOR
      // (0, 0, 0, 1) -> computerRoom.rotateCCW.paintedRed,  // TOP LEFT
      (0, 0, 0, 1) -> computerRoom.rotateCCW,  // TOP LEFT
      (1, 0, 0, 1) -> loadRoom(TvRoom), // modularRoom(1, 0, false, true, w=1).withoutLowDoor(Heading.S),
      (0, 1, 0, 1) -> plantRoom.rotateCW.rotateCW,
      (1, 1, 0, 1) -> loadRoom(CrateRoom),
      // TOP FLOOR
      (0, 0, 1, 1) -> dishRoof.rotateCCW, // TOP LEFT
      (1, 0, 1, 1) -> endTrain,
      (0, 1, 1, 1) -> longWindowRoom,
      (1, 1, 1, 1) -> loadRoom(PenultimateRoom)
    )

    def getRoom(coords: (Int, Int, Int, Int)): Room = {
      hardcodedRooms.get(coords).getOrElse(throw new Exception(s"missing room at ${coords}"))
    }

    val allCoordinates = GridUtil.all4dGridCoordinates(2, 2, 2, 2)
    allCoordinates.foreach { coords =>
      val room = getRoom(coords).paintedForZ(coords._3).paintedForW(coords._4)
      builder.addRoom(room, coords)
    }
    builder.autoLinkRooms()

    builder.writer.applyPaletteToAll(TextureList.SKIES.MOON_SKY, PaletteList.ALSO_NORMAL)
    builder.writer.setPlayerStart(builder.grid((0, 0, 0, 0)))
    builder.writer.clearMarkers()
    builder.outMap
  }

  val Filename: String = "hyper2.map"
}
