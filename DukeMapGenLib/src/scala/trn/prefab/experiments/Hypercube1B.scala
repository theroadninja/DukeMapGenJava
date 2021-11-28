package trn.prefab.experiments

import trn.duke.PaletteList
import trn.logic.Tile2d
import trn.math.SnapAngle
import trn.prefab.{AxisLock, DukeConfig, GameConfig, MapWriter, PastedSectorGroup, SectorGroup}
import trn.{HardcodedConfig, MapLoader, PointXYZ, RandomX}
import trn.{Map => DMap}

import scala.collection.mutable

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


  def main(args: Array[String]): Unit = {

    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    run(gameCfg)
  }

  /**
    * change the sector group to look different for each w dimension
    */
  def changeForW(sg: SectorGroup, w: Int): SectorGroup = {

    // TODO:  also change water to slime or lava

    w match {
      case 0 => sg
      case 1 => MapWriter.painted2(sg, PaletteList.BLUE_TO_RED)
      case 2 => MapWriter.painted2(sg, PaletteList.BLUE_TO_GREEN)
    }
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
    val rooms = Seq(sg104, sg105, HyperSectorGroup(hyperPalette.getSG(106)), loadRoom(107))

    // TODO validate() step here to check you have the right rooms...

    // TODO ideas...
    // - auto floor number next to elevators?
    // - one uniqe T junction starts with the lights shot out and flickrs?
    // - one of the elevator shafts is broken and you can fall all the way down
    // - Elevators:  "elevator-enabled" rooms have a connector for an elevator, but not the actual elevator
    // - at the lowest level, the doors are halfway up
    // - one of the center rooms is blocked by force fields and you have to fall into it
    // - the starting, blue level, is mostly open
    // - one of the center rooms is circular/hexoganal and has the duke symbol on the floor (like the unused room in hyper2)


    // val sideEast = Tile2d(Tile2d.Blocked, Tile2d.Conn, Tile2d.Conn, Tile2d.Conn)
    // val sideSouth = sideEast.rotatedCW
    // val sideWest = sideSouth.rotatedCW

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


    // val r = rooms.find(r => r.tile.couldMatch(sideEast)).get

    def getRoom(x: Int, y: Int, z: Int, w: Int): SectorGroup = {
      val tile = getTile(x, y)
      // TODO add a note somewhere that this doesnt randomly shuffle anything

      val matchesTile = rooms.filter(r => r.tile.couldMatch(tile))
      if(matchesTile.isEmpty){
        throw new Exception(s"no room can fit ${tile}")
      }

      // def lockFilter(r: HyperSectorGroup): Boolean = {
      //   r.sg.props.axisLocks.isEmpty || AxisLock.matchAll(r.sg.props.axisLocks, x, y, z, w)
      // }
      // val matchesLocks = matchesTile.filter(lockFilter)
      val roomsWithLocks = matchesTile.filter(r => r.sg.props.axisLocks.size > 0 && AxisLock.matchAll(r.sg.props.axisLocks, x, y, z, w))
      val roomsWithoutLocks = matchesTile.filter(_.sg.props.axisLocks.isEmpty)

      // must do it this way so that rooms with axis locks take precedence
      val matches = if(roomsWithLocks.nonEmpty){
        roomsWithLocks
      }else{
        roomsWithoutLocks
      }


      //val matches = roomsWithoutLocks.filter(r => r.tile.couldMatch(tile))
      val r = matches.minBy(sg => sg.sg.groupIdOpt.get)
      changeForW(r.rotatedSG(tile), w)
    }
    // val psg1 = writer.pasteSectorGroupAt(sg104.sg, gridManager.cellPosition(1, 1, 0, 0), mustHaveAnchor = true)
    // val psg2 = writer.pasteSectorGroupAt(r.rotatedSG(sideEast), gridManager.cellPosition(2, 1, 0, 0), mustHaveAnchor = true)
    // val psg3 = writer.pasteSectorGroupAt(getRoom(sideSouth), gridManager.cellPosition(1, 2, 0, 0), mustHaveAnchor = true)

    // TODO the green level has an infested room?


    val pastedGroups = mutable.ArrayBuffer[PastedSectorGroup]()
    for(x <- 0 until 3; y <- 0 until 3; z <- 0 until 3; w <- 0 until 3){
      val r = getRoom(x, y, z, w)
      val psg = writer.pasteSectorGroupAt(r, gridManager.cellPosition(x, y, z, w), mustHaveAnchor = true)
      pastedGroups.append(psg)
    }
    require(pastedGroups.size == 3*3*3*3)

    // val pastedGroups = Seq(psg1, psg2, psg3)
    pastedGroups.foreach { r1 =>
      pastedGroups.foreach { r2 =>
        writer.autoLink(r1, r2)
      }
    }

    ExpUtil.finishAndWrite(writer, forcePlayerStart = false)
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
