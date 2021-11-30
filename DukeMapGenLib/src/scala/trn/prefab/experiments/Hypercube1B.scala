package trn.prefab.experiments

import trn.duke.PaletteList
import trn.logic.Tile2d
import trn.logic.Tile2d.{Blocked, Conn}
import trn.math.SnapAngle
import trn.prefab.hypercube.GridCell
import trn.prefab.{AxisLock, CompassWriter, DukeConfig, GameConfig, MapWriter, PastedSectorGroup, SectorGroup}
import trn.{HardcodedConfig, Main, MapLoader, PointXYZ, RandomX, Map => DMap}

import scala.collection.mutable
import scala.collection.JavaConverters._

/**
  * Trying to rewrite Hypercube 1
  *
  *
  * This is an architecture demo:
  * - 3x3x3x3
  * - not supposed to be fun
  * - all rooms must have an anchor sprite
  * - room-to-room connectors:
  *     - must all be the same size and be axis aligned to anchor
  *     - must be at the edge of the room
  *
  * - rooms can have no more than 12 sectors?
  * - certain textures will auto palette-shift based on W coordinate
  *     (different W dimensions get different colors:  blue, red, green/yellow)
  *
  *
  * - sky textures auto swapped between orbit, moon and earth?
  *
  * - allow room shapes:
  *     - +
  *     - T
  *     - corner
  *
  */
object Hypercube1B {

  /** the "red" coordinate of the fourth spacial dimension */
  val WR = 1


  def main(args: Array[String]): Unit = {

    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    run(gameCfg)
  }

  val allCoordinates: Seq[(Int, Int, Int, Int)] = {
    val results = mutable.ArrayBuffer[(Int, Int, Int, Int)]()
    for(x <- 0 until 3; y <- 0 until 3; z <- 0 until 3; w <- 0 until 3){
    // for(x <- 0 until 3; y <- 0 until 3; z <- 0 until 3; w <- 0 until 2){
      results.append((x, y, z, w))
    }
    results
  }

  /**
    * the first/left one in the tuple is always a lower z value
    * @return
    */
  def allCoordinateZPairs: Seq[(GridCell, GridCell)] = {
    allCoordinates.flatMap { coordA =>
      allCoordinates.map { coordB =>
        (GridCell(coordA), GridCell(coordB))
      }
    }.filter{
      case (coordA, coordB) => coordA.add(0, 0, 1, 0) == coordB
    }
  }

  /** the first/left one in the tuple is always the one with a lower w value */
  def allCoordinateWPairs: Seq[(GridCell, GridCell)] = {
    allCoordinates.flatMap { coordA =>
      allCoordinates.map { coordB =>
        (GridCell(coordA), GridCell(coordB))
      }
    }.filter{
      case (coordA, coordB) => coordA.add(0, 0, 0, 1) == coordB
    }
  }

  // def allCoordinatesCrossProduct: Seq[((Int, Int, Int, Int), (Int, Int, Int, Int))] = {
  //   allCoordinates.flatMap { coordA =>
  //     allCoordinates.map { coordB =>
  //       (coordA, coordB)
  //     }
  //   }
  // }




  def add(coordA: (Int, Int, Int, Int), coordB: (Int, Int, Int, Int)): (Int, Int, Int, Int) = {
    (coordA._1 + coordB._1, coordA._2 + coordB._2, coordA._3 + coordB._3, coordA._4 + coordB._4)
  }


  def replaceTex(sg: SectorGroup, replace: Map[Int, Int]): SectorGroup = {
    val result = sg.copy()
    result.allWalls.foreach { w =>
      replace.get(w.getTex).foreach { newTex => w.setTexture(newTex)}
    }
    result.allSectorIds.map(result.getMap.getSector(_)).foreach { sector =>
      replace.get(sector.getFloorTexture).foreach { newTex => sector.setFloorTexture(newTex)}
      replace.get(sector.getCeilingTexture).foreach { newTex => sector.setCeilingTexture(newTex)}
    }

    result

  }

  /**
    * change the sector group to look different for each w dimension
    */
  def changeForW(sg: SectorGroup, w: Int): SectorGroup = {

    val MoonSky1 = 80
    val BigOrbit1 = 84
    val La = 89
    val Stars = 95
    val Water = 336
    val Lava = 1082
    val Slime = 200
    // TODO:  also change water to slime or lava

    val sg2 = w match {
      case 0 => sg
      case 1 => MapWriter.painted2(sg, PaletteList.BLUE_TO_RED) // TODO need to ignore big sky textures!
      case 2 => MapWriter.painted2(sg, PaletteList.BLUE_TO_GREEN)
    }

    w match {
      case 0 => replaceTex(sg2, Map(MoonSky1 -> Stars))
      case 1 => replaceTex(sg2, Map(MoonSky1 -> BigOrbit1, Water -> Lava))
      case 2 => replaceTex(sg2, Map(MoonSky1 -> Stars, Water -> Slime))
    }
  }


  def getTile(x: Int, y: Int): Tile2d = {
    val (west, east) = x match {
      case 0 => (Tile2d.Blocked, Tile2d.Conn) // west edge of grid
      case 1 => (Tile2d.Conn, Tile2d.Conn) // center
      case 2 => (Tile2d.Conn, Tile2d.Blocked) // east edge
      case _ => throw new Exception(s"invalid x coord: ${x}")
    }
    val (north, south) = y match {
      case 0 => (Tile2d.Blocked, Tile2d.Conn)
      case 1 => (Tile2d.Conn, Tile2d.Conn)
      case 2 => (Tile2d.Conn, Tile2d.Blocked)
      case _ => throw new Exception(s"invalid y coord: ${y}")
    }
    Tile2d(east, south, west, north)
  }

  def run(gameCfg: GameConfig): Unit = {
    val hyperPalette = MapLoader.loadPalette(HardcodedConfig.getEduke32Path("hyper1.map"))
    val random = new RandomX()
    val writer = MapWriter(gameCfg)

    val gridManager = new GridManager(
      new PointXYZ(DMap.MIN_X + 10*1024, DMap.MIN_Y + 10*1024, 0),
      cellDist = 7 * 1024
    )

    def loadRoom(i: Int): HyperSectorGroup = HyperSectorGroup(hyperPalette.getSG(i))

    val sg104 = HyperSectorGroup(hyperPalette.getSG(104))
    val sg105 = HyperSectorGroup(hyperPalette.getSG(105))

    // TODO need to ignore the elevator connections on the east side...maybe because they have nonzero ids?


    // val sg108 = addElevator(gameCfg, hyperPalette.getSG(108), 100, hyperPalette.getSG(109))
    val sg108 = hyperPalette.getSG(108)

    // TODO fix scanning for elevator rooms
    val room108 = HyperSectorGroup(sg108, Tile2d(Blocked, Conn, Conn, Conn)) // elevator top

    val room110 = HyperSectorGroup(hyperPalette.getSG(110), Tile2d(Blocked, Conn, Conn, Conn)) // elevator middle

    val room111 = HyperSectorGroup(hyperPalette.getSG(111), Tile2d(Blocked, Conn, Conn, Conn)) // elevator bottom

    val teleporterT = loadRoom(112)

    // val elevatorT = hyperPalette.getSG(109)


    val rooms = Seq(sg104, sg105, HyperSectorGroup(hyperPalette.getSG(106)), room108, room110, room111, teleporterT,
      loadRoom(113),
      loadRoom(115), // nuke-button-shaped center room with holes, top
      loadRoom(107), // nuke-button-shaped center room with holes, bottom
      loadRoom(116), // green infested center room
      loadRoom(117), // center room on bottom

      loadRoom(114),
      loadRoom(118), // flooded teleporter
      loadRoom(119), // center room teleporter
      loadRoom(120), // same as 119 but without the teleporter
    )


    val cornerElevator = loadRoom(114)

    val RedTeleportLower = (2, 2, 1, WR)
    val RedTeleportUpper = (2, 2, 2, WR)

    val hardcodedRooms: Map[(Int, Int, Int, Int), HyperSectorGroup] = Map(
      // red
      // RedTeleportLower -> cornerElevator,
      // RedTeleportUpper -> cornerElevator,

      // teleporters
      (0, 1, 1, 0) -> teleporterT,
      (0, 1, 1, 1) -> teleporterT,
      (2, 1, 1, 1) -> teleporterT,
      (2, 1, 1, 2) -> teleporterT,
    )

    // TODO validate() step here to check you have the right rooms...

    // TODO ideas...
    // - auto floor number next to elevators?
    // - one of the elevator shafts is broken and you can fall all the way down
    //    - it is on the green dimension, and can be used to access the bottom floor of green
    // - one of the rooms, maybe on green, has a nuke button that is only accessible if you grab a key from somewhere
    // - one of the center rooms is blocked by force fields and you have to fall into it
    // - the starting, blue level, is mostly open
    // - finish the nuke-button-shaped falling teleporters
    // - need randomly placed enemies and power ups


    // val sideEast = Tile2d(Tile2d.Blocked, Tile2d.Conn, Tile2d.Conn, Tile2d.Conn)
    // val sideSouth = sideEast.rotatedCW
    // val sideWest = sideSouth.rotatedCW


    def getRoom(x: Int, y: Int, z: Int, w: Int): SectorGroup = {
      val tile = getTile(x, y)

      val room = hardcodedRooms.get(x, y, z, w).getOrElse {
        // TODO add a note somewhere that this doesnt randomly shuffle anything

        val matchesTile = rooms.filter(r => r.tile.couldMatch(tile))
        if(matchesTile.isEmpty){
          throw new Exception(s"no room can fit ${tile}")
        }
        val roomsWithLocks = matchesTile.filter(r => r.sg.props.axisLocks.size > 0 && AxisLock.matchAll(r.sg.props.axisLocks, x, y, z, w))
        val roomsWithoutLocks = matchesTile.filter(_.sg.props.axisLocks.isEmpty)

        // must do it this way so that rooms with axis locks take precedence
        val matches = if(roomsWithLocks.nonEmpty){
          roomsWithLocks
        }else{
          roomsWithoutLocks
        }

        matches.minBy(sg => sg.sg.groupIdOpt.get)
      }
      changeForW(room.rotatedSG(tile), w)
    }

    val pastedGroups = allCoordinates.map {
      case (x, y, z, w) => {
        val r = getRoom(x, y, z, w)
        val psg = writer.pasteSectorGroupAt(r, gridManager.cellPosition(x, y, z, w), mustHaveAnchor = true)
        (x, y, z, w) -> psg
      }
    }.toMap

    linkAndAddElevators(
      writer,
      pastedGroups((1, 0, 1, 0)),
      pastedGroups((1, 0, 2, 0)),
      171, // TODO at least put this in a constant near the top
      hyperPalette.getSG(109)
    )

    linkAndAddElevators(
      writer,
      pastedGroups((1, 0, 0, 0)),
      pastedGroups((1, 0, 1, 0)),
      172,
      hyperPalette.getSG(109)
    )

    linkAndAddElevators(
      writer,
      pastedGroups((1, 0, 0, 0)),
      pastedGroups((1, 0, 2, 0)),
      173,
      hyperPalette.getSG(109)
    )

    // Elevators
    allCoordinateZPairs.foreach {
      case (coordA, coordB) =>
        val psgA = pastedGroups(coordA.asTuple)
        val psgB = pastedGroups(coordB.asTuple)
        tryLinkAllElevators(writer, psgA, psgB)
    }

    // Fourth Dimension Teleporters
    allCoordinateWPairs.foreach {
      case (coordA, coordB) =>
        tryLinkTeleporters(writer, pastedGroups(coordA.asTuple), pastedGroups(coordB.asTuple))
    }

    // Falling Teleporters
    allCoordinateZPairs.foreach {
      case (coordA, coordB) =>
        tryLinkTeleporters(writer, pastedGroups(coordA.asTuple), pastedGroups(coordB.asTuple))
    }

    // All Doorways
    pastedGroups.values.foreach { r1 =>
      pastedGroups.values.foreach { r2 =>
        writer.autoLink(r1, r2)
      }
    }

    ExpUtil.finishAndWrite(writer, forcePlayerStart = false)
  }

  def tryLinkTeleporters(writer: MapWriter, psgLower: PastedSectorGroup, psgHigher: PastedSectorGroup): Unit = {
    psgLower.allTeleportConnectors.foreach { connA =>
      psgHigher.allTeleportConnectors.foreach { connB =>
        if(connA.getConnectorId == connB.getConnectorId){
          // println(psgA, connA.getSectorId)
          // println(psgB, connB.getSectorId)
          try {
            writer.sgBuilder.linkTeleporters(connA, psgLower, connB, psgHigher)
          } catch {
            case e => {
              writer.setAnyPlayerStart(true)
              Main.deployTest(writer.outMap, "error.map", HardcodedConfig.getEduke32Path("error.map"))
              throw e
            }
          }
        }
      }
    }

  }


  // def linkElevators(writer: MapWriter, psgLower: PastedSectorGroup, psgHigher: PastedSectorGroup): Unit = {
  //   // we could support move then 1 elevator; I'm just doing this to make it easier for now
  //   require(psgLower.allElevatorConnectors.size == 1)
  //   require(psgHigher.allElevatorConnectors.size == 1)

  //   writer.linkElevators(psgLower.allElevatorConnectors.head, psgHigher.allElevatorConnectors.head, true)
  // }

  def tryLinkAllElevators(writer: MapWriter, psgLower: PastedSectorGroup, psgHigher: PastedSectorGroup): Unit = {
    for(lower <- psgLower.allElevatorConnectors; higher <- psgHigher.allElevatorConnectors){
      if(lower.getConnectorId == higher.getConnectorId){
        writer.linkElevators(lower, higher, true)
      }
    }
  }


  /** link elevator rooms that dont have an elevator yet */
  def linkAndAddElevators(writer: MapWriter, psgLower: PastedSectorGroup, psgHigher: PastedSectorGroup, connId: Int, elevatorSg: SectorGroup): Unit = {
    val lowerElevator = addElevator(writer, psgLower, connId, elevatorSg)
    val higherElevator = addElevator(writer, psgHigher, connId, elevatorSg)

    val e0 = lowerElevator.getElevatorConn(1).get
    val e1 = higherElevator.getElevatorConn(1).get
    writer.linkElevators(e0, e1, true)

  }

  def addElevator(writer: MapWriter, psg: PastedSectorGroup, connId: Int, elevatorSg: SectorGroup): PastedSectorGroup = {
    require(elevatorSg.allRedwallConnectors.size == 1)
    require(psg.redwallConnectors.filter(_.getConnectorId == connId).size == 1)

    val existingConn = psg.redwallConnectors.find(_.getConnectorId == connId).get

    // TODO the problem is rotation!  need to auto rotate to fit!
    val elevatorSg2 = elevatorSg.rotateCCW
    val newConn = elevatorSg2.allRedwallConnectors.head
    writer.pasteAndLink(existingConn, elevatorSg2, newConn, Seq.empty)
  }


}



case class HyperSectorGroup(sg: SectorGroup, tile: Tile2d) {
  def rotatedSG(rotation: Tile2d): SectorGroup = {
    val snapAngle: SnapAngle = tile.rotationTo(rotation).getOrElse(throw new Exception(s"no rotate to ${rotation}"))
    snapAngle * sg
  }

}

object HyperSectorGroup {
  def apply(sg: SectorGroup): HyperSectorGroup = HyperSectorGroup(
    sg,
    ExpUtil.autoReadTile(sg),
  )
}
