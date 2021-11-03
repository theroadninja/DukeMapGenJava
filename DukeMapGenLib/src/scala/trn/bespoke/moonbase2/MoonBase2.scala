package trn.bespoke.moonbase2

import trn.bespoke.moonbase2.Enemy._
import trn.logic.{Point3d, Tile2d, VariableGridLayout}
import trn.logic.Tile2d._
import trn.math.RotatesCW
import trn.prefab._
import trn.render.{MiscPrinter, Texture, WallAnchor}
import trn.{HardcodedConfig, Main, MapLoader, PointXY, PointXYZ, RandomX, Sprite, SpriteFilter}

import trn.PointImplicits._
import scala.collection.JavaConverters._
import scala.collection.mutable

case class Enemy(
  picnum: Int,
  palette: Int = 0,
  crouch: Option[Int] = None,
  jump: Option[Int] = None,
  stay: Option[Int] = None // stayput
)

object Enemy {
  // XXX add stayput?
  val LizTroop = Enemy(1680, crouch=Some(1744))
  val LizTroopCmdr = Enemy(1680, palette=21, crouch=Some(1744))
  val OctaBrain = Enemy(1820)
  val Drone = Enemy(1880)
  val AssaultCmdr = Enemy(1920)
  val PigCop = Enemy(2000)
  val Enforcer = Enemy(2120, jump=Some(2165), stay=Some(2121))
  val MiniBattlelord = Enemy(2630, palette=21) // Battlelord Sentry

}

object RoomTags {
  val Start = "START"
  val End = "END"
  val Key = "KEY"
  val Gate = "GATE"
  val OneWay = "ONEWAY"
  val Unique = "UNIQUE"

  val Special = Set(Start, End, Key, Gate, OneWay)
}


/** consider moving to trn.render package */
object HallwayPrinter {

  /**
    * returns all of the points of the walls (include the last one, which is defined a wall thats not on the connector
    * A connector with 2 walls will return 3 points:
    *    P -----> P -----> P
    */
  def wallPoints(psg: PastedSectorGroup, conn: RedwallConnector): Seq[PointXY] = {
    val wallIds: Seq[Int] = conn.getWallIds.asScala.map(_.intValue)
    val nextWallId = psg.getMap.getWall(wallIds.last).getPoint2Id
    val points = (wallIds ++ Seq(nextWallId)).map(wid => psg.getMap.getWall(wid).getLocation)
    points
  }

  /** this is called to fill in a passage between the nodes */
  def printHallway(
    r: RandomX,
    gameCfg: GameConfig,
    writer: MapWriter,
    psgA: PastedSectorGroup,
    connA: RedwallConnector,
    psgB: PastedSectorGroup,
    connB: RedwallConnector
  ): Unit = {
    // see also StairPrinter.straightStairs()
    val wallPointsA = wallPoints(psgA, connA).reverse
    val wallPointsB = wallPoints(psgB, connB).reverse
    // TODO interpolate, automatically make it ramp, etc
    val copyFrom = writer.getMap.getSector(connA.getSectorIds.get(0))
    val copyFrom2 = writer.getMap.getSector(connB.getSectorIds.get(0))

    require(wallPointsA.length == 2) // TODO support connectors with more than 1 segment
    require(wallPointsB.length == 2)

    val wallAnchorA = WallAnchor(wallPointsA(0), wallPointsA(1), copyFrom.getFloorZ, copyFrom.getCeilingZ)
    val wallAnchorB = WallAnchor(wallPointsB(0), wallPointsB(1), copyFrom2.getFloorZ, copyFrom2.getCeilingZ)

    if(false && LoungePrinter.canPrintLounge(wallAnchorA, wallAnchorB)){
      try {

        val (r0, r1) = LoungePrinter.printLounge(r, gameCfg, writer.getMap, wallAnchorA, wallAnchorB)
        Seq(r0.sectorId, r1.sectorId).map{ sectorId =>
          connA.getSectorIds.asScala.map( sectorA => MiscPrinter.autoLinkRedWalls(writer.getMap, sectorId, sectorA))
          connB.getSectorIds.asScala.map( sectorA => MiscPrinter.autoLinkRedWalls(writer.getMap, sectorId, sectorA))
        }
      }catch {
        case e: Exception => println(e)
      }

    }else{
      // just print a shitty hallway
      val wallTex = Texture(258, gameCfg.textureWidth(258))
      val wallLoop = (wallPointsA ++ wallPointsB).map (p => MiscPrinter.wall(p, wallTex))
      val sectorId = writer.getMap.createSectorFromLoop(wallLoop: _*)

      connA.getSectorIds.asScala.map { sectorIdA =>
        MiscPrinter.autoLinkRedWalls(writer.getMap, sectorIdA, sectorId)
      }
      connB.getSectorIds.asScala.map { sectorIdB =>
        MiscPrinter.autoLinkRedWalls(writer.getMap, sectorIdB, sectorId)
      }

      val sector = writer.getMap.getSector(sectorId)
      sector.setFloorTexture(181)
      sector.setCeilingTexture(182)
      sector.setFloorZ((copyFrom.getFloorZ + copyFrom2.getFloorZ)/2)
      sector.setCeilingZ(copyFrom.getCeilingZ)
    }

  }

}

// SectorGroup decorated with extra info for this algorithm
case class TileSectorGroup(
  id: String, // used to identify which logical room, to prevent unique rooms being used more than once
  tile: Tile2d,
  sg: SectorGroup,
  tags: Set[String]
) extends RotatesCW[TileSectorGroup] {
  override def rotatedCW: TileSectorGroup = TileSectorGroup(id, tile.rotatedCW, sg.rotatedCW, Set.empty)

  def withKeyLockColor(gameCfg: GameConfig, color: Int): TileSectorGroup = copy(sg=sg.withKeyLockColor(gameCfg, color))

  // TODO plotzone: Int  (0, 1, 2, K, G, ...) ??  (more like PlacedTileSectorGroup)
}

object TileSectorGroup {
  /**
    * Returns true if the tsg is marked as Unique AND a tsg with the same id has already been placed
    * @param alreadyPlaced set of ids of tsgs that have already been placed
    * @param tsg the tsg to check
    * @return
    */
  def uniqueViolation(alreadyPlaced: collection.Set[String], tsg: TileSectorGroup): Boolean = {
    tsg.tags.contains(RoomTags.Unique) && alreadyPlaced.contains(tsg.id)
  }
}

// TODO case class PlacedTileSectorGroup

object MoonBase2 {

  def getMoon2Map(): String = HardcodedConfig.getEduke32Path("moon2.map")

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    run(gameCfg)
  }

  def rotateToMatch(sg: TileSectorGroup, target: Tile2d): TileSectorGroup = {
    val angle = sg.tile.rotationTo(target).get
    angle * sg
  }

  /**
    * Creates a new sector group with the enemy sprites modified.
    *
    * Deleting sprites out of a map is hard because their ids are their
    * position in a single map-wide ordered list.   So instead of deleting them
    * one by one, we save their positions and delete them all.
    * @param sg
    * @return
    */
  def adjustEnemies(random: RandomX, sg: SectorGroup): SectorGroup = {
    /**
      * Concept I invented for this algorithm.  These are enemies that:
      * - make sense in a "land" grouping (even though some can fly)
      * - are not incredibly powerful
      * - are somewhat interchangeable
      * - do not depend on terrain, like sentry guns or even eggs
      */
    val LandEnemies = Seq(
      LizTroop, LizTroop, LizTroop,
      LizTroopCmdr,
      OctaBrain, OctaBrain,
      Drone,
      AssaultCmdr,
      PigCop,
      Enforcer, Enforcer, Enforcer
    )

    val positions = sg.allSprites.filter(s => s.getTex == Enemy.LizTroop.picnum).map { sprite =>
      (sprite.getLocation, sprite.getSectorId)
    }

    // TODO make use of crouching spots
    val crouchingEnemies = sg.allSprites.filter(s => s.getTex == Enemy.LizTroop.crouch.get).map(_.getLocation)

    val result = sg.copy()
    result.getMap.deleteSprites(SpriteFilter.texture(Enemy.LizTroop.picnum))
    result.getMap.deleteSprites(SpriteFilter.texture(Enemy.LizTroop.crouch.get))

    // for simpliciy, the crouching enemies dont affect the size (I guess this design encourages putting
    // copious markers in the sector group so the algorithm has plenty of room)
    val enemyCount = Math.min(positions.size, random.nextInt(6)) // nextInt() is exclusive

    // Next:  choose which enemies it will be

    val enemies = (0 until enemyCount).map(_ => random.randomElement(LandEnemies))
    val positions2 = random.shuffle(positions).toSeq.take(enemies.size)

    enemies.zip(positions2).foreach { case (enemy, (position, sectorId)) =>
      val s = new Sprite(position, sectorId, enemy.picnum, 0, 0)
      s.setPal(enemy.palette)
      result.getMap.addSprite(s)
    }

    result
  }

  /**
    * Calculates how large the column needs to be to include all of the points of the sector group if the sector group's
    * anchor is at the center of the column.
    */
  def columnWidth(tsg: TileSectorGroup): Int = {
    val bb = tsg.sg.boundingBox
    val anchor = tsg.sg.getAnchor.asXY
    2 * Math.max(Math.abs(anchor.x - bb.xMin), Math.abs(anchor.x - bb.xMax))
  }

  def rowHeight(tsg: TileSectorGroup): Int = {
    val bb = tsg.sg.boundingBox
    val anchor = tsg.sg.getAnchor.asXY
    2 * Math.max(Math.abs(anchor.y - bb.yMin), Math.abs(anchor.y - bb.yMax))
  }

  def getTileSpec(logicalMap: LogicalMap[LogicalRoom, String], p: Point3d): TileSpec = {
    TileSpec(
      LogicalRoom.readSide(p, Heading.E, logicalMap),
      LogicalRoom.readSide(p, Heading.S, logicalMap),
      LogicalRoom.readSide(p, Heading.W, logicalMap),
      LogicalRoom.readSide(p, Heading.N, logicalMap)
    )
  }

  def run(gameCfg: GameConfig): Unit = {
    println("starting run()")
    val random = new RandomX()
    val writer = MapWriter(gameCfg)
    val spacePalette = MapLoader.loadPalette(HardcodedConfig.getMapDataPath("SPACE.MAP"))
    val moonPalette = MapLoader.loadPalette(HardcodedConfig.getEduke32Path("moon2.map"))
    println("loaded moon2.map")

    val logicalMap = new RandomWalkGenerator(random).generate()
    val keycolors: Seq[Int] = random.shuffle(DukeConfig.KeyColors).toSeq
    println(logicalMap)

    def getTsg(tile: Tile2d, sgNumber: Int, tags: Set[String] = Set.empty): TileSectorGroup = TileSectorGroup(sgNumber.toString, tile, moonPalette.getSG(sgNumber), tags)

    val startSg = MoonBase3.readTileSectorGroup(gameCfg, moonPalette, 1)
    val fourWay = MoonBase3.readTileSectorGroup(gameCfg, moonPalette, 2)
    val endSg = MoonBase3.readTileSectorGroup(gameCfg, moonPalette, 3)
    val keySg = MoonBase3.readTileSectorGroup(gameCfg, moonPalette, 4)

    val gateSgLeftEdge = getTsg(Tile2d(Conn), 5)
    val gateSgTopEdge = getTsg(Tile2d(Conn), 6)
    val gateSg3 = getTsg(Tile2d(Conn, Conn, Blocked, Blocked), 7)

    val windowRoom1 = MoonBase3.readTileSectorGroup(gameCfg, moonPalette, 8)
    val conferenceRoom = MoonBase3.readTileSectorGroup(gameCfg, moonPalette, 9)
    val conveyor = MoonBase3.readTileSectorGroup(gameCfg, moonPalette, 11)
    val bar = MoonBase3.readTileSectorGroup(gameCfg, moonPalette, 12)
    val lectureHall = MoonBase3.readTileSectorGroup(gameCfg, moonPalette, 13)
    val blastDoorsRoom = MoonBase3.readTileSectorGroup(gameCfg, moonPalette, 14)

    val blueRoom = getTsg(Tile2d(Wildcard), 15) // modular room

    val fanRoom = MoonBase3.readTileSectorGroup(gameCfg, moonPalette, 20)

    val standardRooms = Seq(windowRoom1, conferenceRoom, conveyor, bar, blastDoorsRoom, fanRoom, blueRoom, fourWay)


    // ONE-WAY
    val area51 = getTsg(Tile2d(Conn, Conn, Conn, 2), 10, Set("ONEWAY"))

    // NOTE:  some of my standard space doors are 2048 wide

    val allRooms = Seq(startSg, fourWay, endSg, keySg, gateSgLeftEdge, gateSgTopEdge, gateSg3, windowRoom1,
      conferenceRoom, conveyor, bar, lectureHall, blastDoorsRoom, blueRoom, fanRoom) ++ (21 to 23).map(i => MoonBase3.readTileSectorGroup(gameCfg, moonPalette, i))
    // TODO MoonBase3.sanityCheck(allRooms)

    // val uniqueTiles = Set(bar, conveyor, conferenceRoom, windowRoom1).map(_.id)
    require(standardRooms.filter(_.tags.contains(RoomTags.Unique)).size == 4)
    val usedTiles = mutable.Set[String]()


    def getKeyTile(gameCfg: GameConfig, r: RandomX, target: Tile2d, keycolor: Int): TileSectorGroup = {
      val options = r.shuffle(Seq(keySg, lectureHall)).filter(t => t.tile.couldMatch(target)).toSeq
      rotateToMatch(options.head, target).withKeyLockColor(gameCfg, keycolor)
    }

    /**
      * @param tileSpec the shape (connector vs no conn on each side) it needs to fit
      */
    def getTile(
      r: RandomX,
      node: LogicalRoom,
      tileSpec: TileSpec, // target: Tile2d,
      wildcardTarget: Tile2d, // stupid hack so that tiles with too many connections are still allowed
      tag: Option[String]
    ): TileSectorGroup = {

      val target = tileSpec.toTile2d(Tile2d.Blocked)
      val rooms = Seq(startSg, endSg, area51) ++ standardRooms // TODO more here

      // TODO "START" should be a constant
      if(tag == Some("KEY")){
        val keycolor = keycolors(node.keyindex.get)

        getKeyTile(gameCfg, random, wildcardTarget, keycolor) // TODO getKeyTile already rotated it...

      }else if(tag == Some("GATE")) {
        //val keycolor: Int =  keycolors(s(1).toString.toInt - 1)
        val keycolor = keycolors(node.keyindex.get)


        // TODO cant do a simply rotation because it matters wich conns are blocked by the gate
        // TODO this is a good first adopter for the code that will calculate each tile based on neighboors
        val gateSg = if (target.w == Tile2d.Conn) {
          gateSgLeftEdge
        } else if (target.n == Tile2d.Conn) {
          gateSgTopEdge
        } else {
          gateSg3
        }
        gateSg.withKeyLockColor(gameCfg, keycolor)

      }else if(tag == Some("ONEWAY")){
        r.shuffle(rooms.filter(_.tags.contains(tag.get))).toSeq.head
      }else if(tag.isDefined){
        val t = r.shuffle(rooms.filter(_.tags.contains(tag.get))).toSeq.head
        rotateToMatch(t, target)
      }else{
        // TODO make sure to exclude rooms with tags (because of end room, and oneway rooms)
        // val room = random.shuffle(standardRooms).find(t => t.tile.couldMatch(target) && !(uniqueTiles.contains(t.id) && usedTiles.contains(t.id))).getOrElse(fourWay)
        val room = random.shuffle(standardRooms).find(t => t.tile.couldMatch(target) && !TileSectorGroup.uniqueViolation(usedTiles, t)).getOrElse(fourWay)
        usedTiles.add(room.id)
        rotateToMatch(room, wildcardTarget)
      }

    }


    println("generating map")
    val sgChoices: Map[Point3d, TileSectorGroup] = logicalMap.nodes.map { case (gridPoint: Point3d, node: LogicalRoom) =>
      val nodeType = node.s

      val tileSpec = getTileSpec(logicalMap, gridPoint)
      val target = logicalMap.getTile(gridPoint, Tile2d.Blocked)
      val wildcardTarget = logicalMap.getTile(gridPoint, Tile2d.Wildcard)

      require(target == tileSpec.toTile2d(Tile2d.Blocked))

      // TODO this fails because getTileSpec() is the only one that enforces the blocked thing based on node presence (
      // TODO getTile() only cares if there is an edge)
      //require(wildcardTarget == tileSpec.toTile2d(Tile2d.Wildcard), s"${wildcardTarget} != ${tileSpec.toTile2d(Tile2d.Wildcard)}")


      val sg = nodeType match {
        case "S" => {
          getTile(random, node, tileSpec, wildcardTarget, node.tag)
        }
        case "E" => {
          getTile(random, node, tileSpec, wildcardTarget, node.tag)
        }
        case s if s.startsWith("K") => {
          getTile(random, node, tileSpec, wildcardTarget, node.tag)
        }
        case s if s.startsWith("G") => {
          getTile(random, node, tileSpec, wildcardTarget, node.tag)
        }
        case s if s.endsWith("<") => {
          val onewaySg = getTile(random, node, tileSpec, wildcardTarget, node.tag)
          // so the edges have a value that is a string, usually z-length but set to the node level when near a oneway...
          // TODO calculate this from tileSpec instead
          val targetOneway = logicalMap.getTileForOneway(gridPoint, node.higherZone.get)
          rotateToMatch(onewaySg, targetOneway)
        }
        case _ => {
          getTile(random, node, tileSpec, wildcardTarget, node.tag)
        }
      }
      gridPoint -> sg
    }.toMap

    pasteRooms(gameCfg, writer, random, logicalMap, sgChoices)

    finishAndWrite(writer)
  }

  def finishAndWrite(writer: MapWriter): Unit = {
    // ////////////////////////
    writer.disarmAllSkyTextures()
    writer.setAnyPlayerStart(force = true)
    writer.sgBuilder.clearMarkers()
    writer.checkSectorCount()
    Main.deployTest(writer.outMap, "output.map", HardcodedConfig.getEduke32Path("output.map"))
  }

  def pasteRooms(gameCfg: GameConfig, writer: MapWriter, random: RandomX, logicalMap: LogicalMap[LogicalRoom, String], sgChoices: Map[Point3d, TileSectorGroup]): Unit = {

    val columns = sgChoices.keys.map(_.x).toSet
    val rows = sgChoices.keys.map(_.y).toSet
    val columnWidths = columns.map{ col =>
      val maxWidth = sgChoices.collect{case (point, tsg) if col == point.x => columnWidth(tsg)}.max
      col -> maxWidth
    }.toMap
    val rowHeights = rows.map { row =>
      val maxHeight = sgChoices.collect { case(point, tsg) if row == point.y => rowHeight(tsg) }.max
      row -> maxHeight
    }.toMap

    // controls the gaps between the grids:
    val marginSize = 1024 * 2 // TODO will be different
    val vgrid = VariableGridLayout(columnWidths, rowHeights, marginSize, marginSize)

    val gridSize = 12 * 1024 // TODO this will be different for every row and column
    val originPoint = logicalMap.center // this point goes at 0, 0

    val gridTopLeft = PointXY.ZERO.subtractedBy(vgrid.center)


    val pastedGroups = mutable.Map[Point3d, PastedSectorGroup]()
    sgChoices.foreach { case(gridPoint, sg) =>
      // val x = gridPoint.x - originPoint.x
      // val y = gridPoint.y - originPoint.y
      // val z = gridPoint.z - originPoint.z
      // val mapPoint = new PointXYZ(x, y, z).multipliedBy(gridSize + marginSize)

      val mapPoint: PointXYZ = gridTopLeft.withZ(0).add(vgrid.boundingBox(gridPoint.x, gridPoint.y).center.withZ(0))

      val psg = writer.pasteSectorGroupAt(adjustEnemies(random, sg.sg), mapPoint) // TODO this uses the anchor
      pastedGroups.put(gridPoint, psg)
    }

    logicalMap.edges.foreach { case (edge, _) =>
      require(edge.isHorizontal || edge.isVertical)
      val psgA = pastedGroups(edge.p1)
      val psgB = pastedGroups(edge.p2)
      if(edge.isHorizontal){
        HallwayPrinter.printHallway(
          random,
          gameCfg,
          writer,
          psgA,
          CompassWriter.east(psgA).get,
          psgB,
          CompassWriter.west(psgB).get
        )

      }else if(edge.isVertical){
        HallwayPrinter.printHallway(
          random,
          gameCfg,
          writer,
          psgA,
          CompassWriter.south(psgA).get,
          psgB,
          CompassWriter.north(psgB).get
        )

      }else{
        throw new Exception(s"cant handle edge ${edge}")
      }
    }
  }

}
