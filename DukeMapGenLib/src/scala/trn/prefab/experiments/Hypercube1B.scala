package trn.prefab.experiments

import trn.duke.PaletteList
import trn.logic.Tile2d
import trn.logic.Tile2d.{Blocked, Conn}
import trn.math.SnapAngle
import trn.prefab.hypercube.GridCell
import trn.prefab.{AxisLock, CompassWriter, DukeConfig, GameConfig, MapWriter, PastedSectorGroup, PrefabPalette, RedwallConnector, SectorGroup}
import trn.{HardcodedConfig, Main, MapLoader, PointXY, PointXYZ, RandomX, WallView, Map => DMap}

import scala.collection.mutable
import scala.collection.JavaConverters._

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
    val xx = x * cellDist
    val yy = y * cellDist + z * ((maxGridY + 1) * cellDist)
    val zz = -1 * (z << 4) * cellDist + 1024 // raise it, so elevators work
    //val zz = z * + 1024 // raise it, so elevators work

    val xxx = xx + (w * ((maxGridX + 1) * cellDist)) // adjust for w coordinate
    origin.add(new PointXYZ(xxx, yy, zz))
  }
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
    * Return a copy of a sector group with textures replaced.
    *
    * @param sg the sector group to modify
    * @param replace a map of textures: keys are textures to replace, and values are their replacements
    * @return a copy of sg with textures replaced
    */
  def replaceTextures(sg: SectorGroup, replace: Map[Int, Int]): SectorGroup = {
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
      case 0 => replaceTextures(sg2, Map(MoonSky1 -> Stars))
      case 1 => replaceTextures(sg2, Map(MoonSky1 -> BigOrbit1, Water -> Lava))
      case 2 => replaceTextures(sg2, Map(MoonSky1 -> Stars, Water -> Slime))
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

  def run(gameCfg: GameConfig, writer: MapWriter): Unit = {
    val hyperPalette = MapLoader.loadPalette(HardcodedConfig.getEduke32Path("hyper1.map"))

    def loadRoom(i: Int): HyperSectorGroup = HyperSectorGroup(hyperPalette.getSG(i))
    val rooms: Seq[HyperSectorGroup] = hyperPalette.numberedSectorGroupIds.asScala.map(loadRoom(_)).toSeq

    val cellDist = validate(rooms) // 7 * 1024
    val gridManager = new GridManager(
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
          writer.sgBuilder.linkTeleporters(connA, psgLower, connB, psgHigher)
        }
      }
    }
  }

  def tryLinkAllElevators(writer: MapWriter, psgLower: PastedSectorGroup, psgHigher: PastedSectorGroup): Unit = {
    for(lower <- psgLower.allElevatorConnectors; higher <- psgHigher.allElevatorConnectors){
      if(lower.getConnectorId == higher.getConnectorId){
        writer.linkElevators(lower, higher, true)
      }
    }
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
