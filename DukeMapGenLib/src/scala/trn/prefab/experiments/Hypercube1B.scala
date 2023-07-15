package trn.prefab.experiments

import trn.duke.PaletteList
import trn.logic.Tile2d
import trn.logic.Tile2d.{Conn, Blocked}
import trn.math.SnapAngle
import trn.prefab.hypercube.GridCell
import trn.prefab.{MapWriter, DukeConfig, GameConfig, SectorGroup, RedwallConnector, AxisLock}
import trn.{PointXYZ, ScalaMapLoader, Main, WallView, HardcodedConfig, Map => DMap}

import scala.collection.JavaConverters._

class GridManagerV1(
  val origin: PointXYZ,
  val cellDist: Int,  // the distance between the centers of two grid nodes
  val maxGridY: Int = 4, // maximum grid spaces in the y direction; used to place z rows
  val maxGridX: Int = 4
) extends GridManager4D {
  /**
    * transforms grid cell index (XYZW) to raw xyz coordinates
    */
  def cellPosition(x: Int, y: Int, z: Int, w: Int): PointXYZ = {
    val xx = x * cellDist
    val yy = y * cellDist + z * ((maxGridY + 1) * cellDist)
    val zz = -1 * (z << 4) * cellDist + 1024 // raise it, so elevators work
    //val zz = z * + 1024 // raise it, so elevators work

    val xxx = xx + (w * ((maxGridX + 1) * cellDist)) // adjust for w coordinate
    origin.add(new PointXYZ(xxx, yy, zz))
  }

  override def toXYZ(gridCell: (Int, Int, Int, Int)): PointXYZ = cellPosition(gridCell._1, gridCell._2, gridCell._3, gridCell._4)
}

/**
  * Trying to rewrite Hypercube 1
  *
  *
  * This is an architecture demo:
  * - 3x3x3x3
  * - all rooms must have an anchor sprite
  * - room-to-room connectors:
  *     - must all be the same size and be axis aligned to anchor
  *     - must be at the edge of the room
  *
  * - rooms can have no more than 12 sectors?
  * - certain textures will auto palette-shift based on W coordinate
  *     (different W dimensions get different colors:  blue, red, green/yellow)
  *

// TODO ideas for later...
    // - auto floor number next to elevators?
    // - one of the elevator shafts is broken and you can fall all the way down
    //    - it is on the green dimension, and can be used to access the bottom floor of green
    // - one of the rooms, maybe on green, has a nuke button that is only accessible if you grab a key from somewhere
    // - one of the center rooms is blocked by force fields and you have to fall into it
    // - need randomly placed enemies and power ups
    // - have a normal staircase to go up a Z level (probably needs to be more like ramps)
    // - have some twisty hallway where you can walk to the next W
  */
object Hypercube1B {

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    val writer = MapWriter(gameCfg)
    try {
      run(gameCfg, writer)
    } catch {
      case e => {
        writer.setAnyPlayerStart(true)
        Main.deployTest(writer.outMap, "error.map", HardcodedConfig.getEduke32Path("error.map"))
        throw e
      }
    }
  }

  lazy val allCoordinates = GridUtil.all4dGridCoordinates(3, 3, 3, 3)

  /**
    * the first/left one in the tuple is always a lower z value
    * @return
    */
  lazy val allCoordinateZPairs: Seq[(GridCell, GridCell)] = {
    allCoordinates.flatMap { coordA =>
      allCoordinates.map { coordB =>
        (GridCell(coordA), GridCell(coordB))
      }
    }.filter{
      case (coordA, coordB) => coordA.add(0, 0, 1, 0) == coordB
    }
  }

  /** the first/left one in the tuple is always the one with a lower w value */
  lazy val allCoordinateWPairs: Seq[(GridCell, GridCell)] = {
    allCoordinates.flatMap { coordA =>
      allCoordinates.map { coordB =>
        (GridCell(coordA), GridCell(coordB))
      }
    }.filter{
      case (coordA, coordB) => coordA.add(0, 0, 0, 1) == coordB
    }
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
      case 0 => sg2.withTexturesReplaced(Map(MoonSky1 -> Stars))
      case 1 => sg2.withTexturesReplaced(Map(MoonSky1 -> BigOrbit1, Water -> Lava))
      case 2 => sg2.withTexturesReplaced(Map(MoonSky1 -> Stars, Water -> Slime))
    }
  }

  /** @returns a Tile for a grid position */
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

  /**
    * Make sure we have all the kinds of rooms that we need
    * @return the "cell distance":  the distance from the center of one room to the center of another room
    */
  def validate(rooms: Seq[HyperSectorGroup]): Int = {
    val tiles = rooms.map(_.tile).toSet

    // TODO this logic doesnt properly deal with axis locks...
    require(tiles.exists(t => t.couldMatch(Tile2d(Blocked, Conn, Conn, Conn)))) // must have T
    require(tiles.exists(t => t.couldMatch(Tile2d(Blocked, Blocked, Conn, Conn)))) // must have corner
    require(tiles.exists(t => t.couldMatch(Tile2d(Conn, Conn, Conn, Conn)))) // must have +

    val distances = rooms.flatMap(r => HyperSectorGroup.centerToHallway(r.sg)).toSet
    if(distances.size == 1){
      distances.head * 2
    }else{
      throw new Exception(s"Different rooms have different sizes: ${distances}")
    }
  }

  def getRoom(rooms: Seq[HyperSectorGroup], x: Int, y: Int, z: Int, w: Int): SectorGroup = {
    val tile = getTile(x, y)
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
    val room = matches.minBy(sg => sg.sg.groupIdOpt.get)
    changeForW(room.rotatedSG(tile), w)
  }

  val Filename: String = "hyper1.map"

  def run(gameCfg: GameConfig, writer: MapWriter): Unit = {
    val hyperPalette = ScalaMapLoader.loadPalette(HardcodedConfig.getEduke32Path(Filename))

    def loadRoom(i: Int): HyperSectorGroup = HyperSectorGroup(hyperPalette.getSG(i))
    val rooms: Seq[HyperSectorGroup] = hyperPalette.numberedSectorGroupIds.asScala.map(loadRoom(_)).toSeq

    val cellDist = validate(rooms) // 7 * 1024
    val gridManager = new GridManagerV1(
      new PointXYZ(DMap.MIN_X + 10*1024, DMap.MIN_Y + 10*1024, 0),
      cellDist = cellDist
    )

    // Select and Print the rooms
    val pastedGroups = allCoordinates.map {
      case (x, y, z, w) => {
        val r = getRoom(rooms, x, y, z, w)
        val psg = writer.pasteSectorGroupAt(r, gridManager.cellPosition(x, y, z, w), mustHaveAnchor = true)
        (x, y, z, w) -> psg
      }
    }.toMap

    // Elevators
    allCoordinateZPairs.foreach {
      case (coordA, coordB) =>
        val psgA = pastedGroups(coordA.asTuple)
        val psgB = pastedGroups(coordB.asTuple)
        HyperUtil.tryLinkAllElevators(writer, psgA, psgB)
    }

    // Fourth Dimension Teleporters
    allCoordinateWPairs.foreach {
      case (coordA, coordB) =>
        HyperUtil.tryLinkTeleporters(writer, pastedGroups(coordA.asTuple), pastedGroups(coordB.asTuple))
    }

    // Falling Teleporters
    allCoordinateZPairs.foreach {
      case (coordA, coordB) =>
        HyperUtil.tryLinkTeleporters(writer, pastedGroups(coordA.asTuple), pastedGroups(coordB.asTuple))
    }

    // All Doorways
    pastedGroups.values.foreach { r1 =>
      pastedGroups.values.foreach { r2 =>
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

  /**
    * measures distance from the anchor point of the room to each axis-aligned redwall connector
    * (throws an exception if they are not the same)
    * returns non if there are no suitable redwall connector
    */
  def centerToHallway(sg: SectorGroup): Option[Int] = {
    val anchor = sg.getAnchor.asXY

    def walls(c: RedwallConnector, sg: SectorGroup): Seq[WallView] = {
      val map = sg.getMap
      c.getWallIds.asScala.map(map.getWallView(_))
    }

    val distances = sg.allRedwallConnectors
      .filter(c => c.getWallIds.size == 1 && !c.isLinked(sg.getMap))
      .map(c => walls(c, sg)(0))
      .filter(w => w.getLineSegment.isAxisAligned)
      .map(w => w.getLineSegment.midpoint.distanceTo(anchor).toInt).toSet

    distances.size match {
      case 0 => None
      case 1 => distances.headOption
      case _ => throw new Exception(s"sector group ${sg.sectorGroupId} has connectors with different distances to anchor")
    }
  }
}
