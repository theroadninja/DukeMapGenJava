package trn.bespoke.moonbase2

import trn.bespoke.moonbase2.Enemy._
import trn.logic.{Point3d, Tile2d, VariableGridLayout}
import trn.logic.Tile2d._
import trn.math.RotatesCW
import trn.prefab._
import trn.render.{MiscPrinter, Texture}
import trn.{HardcodedConfig, Main, MapLoader, PointXY, PointXYZ, Sprite, SpriteFilter}

import scala.collection.JavaConverters._
import scala.collection.mutable // this is the good one

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

  def printHallway(
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
    val wallTex = Texture(258, gameCfg.textureWidth(258))

    val wallLoop = (wallPointsA ++ wallPointsB).map (p => MiscPrinter.wall(p, wallTex))
    val sectorId = writer.getMap.createSectorFromLoop(wallLoop: _*)

    connA.getSectorIds.asScala.map { sectorIdA =>
      MiscPrinter.autoLinkRedWalls(writer.getMap, sectorIdA, sectorId)
    }
    connB.getSectorIds.asScala.map { sectorIdB =>
      MiscPrinter.autoLinkRedWalls(writer.getMap, sectorIdB, sectorId)
    }


    // TODO interpolate, automatically make it ramp, etc
    val copyFrom = writer.getMap.getSector(connA.getSectorIds.get(0))
    val copyFrom2 = writer.getMap.getSector(connB.getSectorIds.get(0))

    val sector = writer.getMap.getSector(sectorId)
    sector.setFloorTexture(181)
    sector.setCeilingTexture(182)
    sector.setFloorZ((copyFrom.getFloorZ + copyFrom2.getFloorZ)/2)
    sector.setCeilingZ(copyFrom.getCeilingZ)
    // TODO link them

  }

}

// SectorGroup decorated with extra info for this algorithm
case class TileSectorGroup(tile: Tile2d, sg: SectorGroup) extends RotatesCW[TileSectorGroup] {
  override def rotatedCW: TileSectorGroup = TileSectorGroup(tile.rotatedCW, sg.rotatedCW)
}

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

  def run(gameCfg: GameConfig): Unit = {
    val random = new RandomX()
    val writer = MapWriter(gameCfg)
    val spacePalette = MapLoader.loadPalette(HardcodedConfig.getMapDataPath("SPACE.MAP"))
    val moonPalette = MapLoader.loadPalette(HardcodedConfig.getEduke32Path("moon2.map"))

    val logicalMap = new RandomWalkGenerator(random).generate()
    val keycolors: Seq[Int] = random.shuffle(DukeConfig.KeyColors).toSeq
    println(logicalMap)

    val startSg = TileSectorGroup(Tile2d(Blocked).withSide(Heading.W, Conn), moonPalette.getSG(1))
    val fourWay = TileSectorGroup(Tile2d(Conn), moonPalette.getSG(2))
    val endSg = TileSectorGroup(Tile2d(Blocked).withSide(Heading.W, Conn), moonPalette.getSG(3))
    val keySg = TileSectorGroup(Tile2d(Conn), moonPalette.getSG(4))
    val gateSgLeftEdge = TileSectorGroup(Tile2d(Conn), moonPalette.getSG(5))
    val gateSgTopEdge = TileSectorGroup(Tile2d(Conn), moonPalette.getSG(6))
    val gateSg3 = TileSectorGroup(Tile2d(Conn, Conn, Blocked, Blocked), moonPalette.getSG(7))
    val windowRoom1 = TileSectorGroup(Tile2d(Conn, Blocked, Conn, Blocked), moonPalette.getSG(8))
    val conferenceRoom = TileSectorGroup(Tile2d(Conn, Blocked, Conn, Blocked), moonPalette.getSG(9))
    //val conferenceRoomVertical = moonPalette.getSG(9).rotateCW

    // NOTE:  some of my standard space doors are 2048 wide

    val sgChoices: Map[Point3d, TileSectorGroup] = logicalMap.nodes.map { case (gridPoint: Point3d, nodeType: String) =>

      val sg = nodeType match {
        case "S" => {
          rotateToMatch(startSg, logicalMap.getTile(gridPoint))
        }
        case "E" => {
          rotateToMatch(endSg, logicalMap.getTile(gridPoint))
        }
        case s if s.startsWith("K") => {
          val keycolor: Int = keycolors(s(1).toString.toInt - 1)
          TileSectorGroup(keySg.tile, keySg.sg.withKeyLockColor(gameCfg, keycolor))
        }
        case s if s.startsWith("G") => {
          val keycolor: Int =  keycolors(s(1).toString.toInt - 1)
          val gateSg = if(logicalMap.containsEdge(gridPoint, gridPoint.w)){
            gateSgLeftEdge
          }else if(logicalMap.containsEdge(gridPoint, gridPoint.n)){
            gateSgTopEdge
          }else{
            gateSg3
          }
          TileSectorGroup(gateSg.tile, gateSg.sg.withKeyLockColor(gameCfg, keycolor))
        }
        case _ => {
          val target = logicalMap.getTile(gridPoint, Tile2d.Blocked)
          val target2 = logicalMap.getTile(gridPoint, Tile2d.Wildcard) // fourWay has 4 connections
          val room = random.shuffle(Seq(windowRoom1, conferenceRoom)).find(_.tile.couldMatch(target)).getOrElse(fourWay)
          rotateToMatch(room, target2)
        }
      }
      gridPoint -> sg
    }.toMap

    val columns = sgChoices.keys.map(_.x).toSet
    val rows = sgChoices.keys.map(_.y).toSet
    val columnWidths = columns.map{ col =>
      val maxWidth = sgChoices.collect{ case(point, tsg) if col ==  point.x => tsg.sg.bbWidth }.max
      col -> maxWidth
    }.toMap
    val rowHeights = rows.map { row =>
      val maxHeight = sgChoices.collect { case(point, tsg) if row == point.y => tsg.sg.bbHeight }.max
      row -> maxHeight
    }.toMap
    val vgrid = VariableGridLayout(columnWidths, rowHeights, 0, 0)

    val gridSize = 12 * 1024 // TODO this will be different for every row and column
    val marginSize = 1024 // TODO will be different
    val originPoint = logicalMap.center // this point goes at 0, 0
    // TODO val originPoint = vgrid.center.withZ(0) // this point goes at 0, 0


    val pastedGroups = mutable.Map[Point3d, PastedSectorGroup]()
    sgChoices.foreach { case(gridPoint, sg) =>
      val x = gridPoint.x - originPoint.x
      val y = gridPoint.y - originPoint.y
      val z = gridPoint.z - originPoint.z
      val mapPoint = new PointXYZ(x, y, z).multipliedBy(gridSize + marginSize)
      val psg = writer.pasteSectorGroupAt(adjustEnemies(random, sg.sg), mapPoint)
      pastedGroups.put(gridPoint, psg)
    }

    logicalMap.edges.foreach { case (edge, _) =>
      require(edge.isHorizontal || edge.isVertical)
      val psgA = pastedGroups(edge.p1)
      val psgB = pastedGroups(edge.p2)
      if(edge.isHorizontal){
        HallwayPrinter.printHallway(
          gameCfg,
          writer,
          psgA,
          CompassWriter.east(psgA).get,
          psgB,
          CompassWriter.west(psgB).get
        )

      }else if(edge.isVertical){
        HallwayPrinter.printHallway(
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

    // ////////////////////////
    writer.disarmAllSkyTextures()
    writer.setAnyPlayerStart(force = true)
    writer.sgBuilder.clearMarkers()
    writer.checkSectorCount()
    Main.deployTest(writer.outMap, "output.map", HardcodedConfig.getEduke32Path("output.map"))
  }

}
