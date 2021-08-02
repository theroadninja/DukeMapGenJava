package trn.bespoke

import com.sun.javafx.geom.Edge
import trn.bespoke.Enemy.{AssaultCmdr, Drone, Enforcer, LizTroop, LizTroopCmdr, OctaBrain, PigCop}
import trn.{HardcodedConfig, Main, MapLoader, PointXY, PointXYZ, Sprite, SpriteFilter}
import trn.prefab.{CompassWriter, DukeConfig, GameConfig, Heading, MapWriter, PastedSectorGroup, PrefabUtils, RandomX, RedwallConnector, SectorGroup}
import trn.render.{MiscPrinter, Texture}

import scala.collection.mutable
import scala.collection.JavaConverters._ // this is the good one

case class Enemy(
  picnum: Int,
  palette: Int = 0,
  crouch: Option[Int] = None,
  jump: Option[Int] = None,
  stay: Option[Int] = None // stayput
)

// TODO add stayput
object Enemy {
  val LizTroop = Enemy(1680, crouch=Some(1744))
  val LizTroopCmdr = Enemy(1680, palette=21, crouch=Some(1744))
  val OctaBrain = Enemy(1820)
  val Drone = Enemy(1880)
  val AssaultCmdr = Enemy(1920)
  val PigCop = Enemy(2000)
  val Enforcer = Enemy(2120, jump=Some(2165), stay=Some(2121))
  val MiniBattlelord = Enemy(2630, palette=21) // Battlelord Sentry

}

// TODO really need a generic "grid" or "logical" point that I can reuse
case class Point(x: Int, y: Int, z: Int) extends Ordered[Point] {
  def compare(other: Point): Int ={
    Seq(x.compare(other.x), y.compare(other.y), z.compare(other.z)).find(_ != 0).getOrElse(0)
  }

  def n: Point = Point(x, y-1, z)
  def s: Point= Point(x, y+1, z)
  def e: Point = Point(x+1, y, z)
  def w: Point = Point(x-1, y, z)

  def adj: Seq[Point] = Seq(n, s, e, w)
}

object Edge {
  def sorted(a: Point, b: Point): Edge = {
    val p = Seq(a, b).sorted
    Edge(p(0), p(1))
  }
}
case class Edge(p1: Point, p2: Point) {
  def isHorizontal: Boolean = p1.y == p2.y && p1.x != p2.x
  def isVertical: Boolean = p1.x == p2.x && p1.y != p2.y
}

object LogicalMap {
  def apply[V, E](): LogicalMap[V, E] = new LogicalMap
}

class LogicalMap[V, E] {
  val nodes = mutable.Map[Point, V]()
  val edges = mutable.Map[Edge, E]()

  def get(p: Point): Option[V] = nodes.get(p)

  def contains(p: Point): Boolean = nodes.contains(p)

  def center: Point = {
    def midpoint(values: Iterable[Int]) = {
      val minval = values.min
      minval + (values.max - minval) / 2
    }
    val xs = nodes.keys.map(_.x)
    val ys = nodes.keys.map(_.y)
    val zs = nodes.keys.map(_.z)
    Point(midpoint(xs), midpoint(ys), midpoint(zs))
  }

  def emptyAdj(p: Point): Seq[Point] = {
    p.adj.filterNot(contains)
  }

  def put(p: Point, value: V, overwrite: Boolean = false): Unit = {
    require(overwrite || !nodes.contains(p))
    nodes.put(p, value)
  }

  def putEdge(a: Point, b: Point, edgeValue: E): Unit = {
    require(nodes.isDefinedAt(a) && nodes.isDefinedAt(b))
//    val p = Seq(a, b).sorted
//    edges.put(Edge(p(0), p(1)), edgeValue)
    edges.put(Edge.sorted(a, b), edgeValue)
  }

  def containsEdge(a: Point, b: Point): Boolean = {
//    val p = Seq(a, b).sorted
//    edges.get(Edge(p(0), p(1))).isDefined
    edges.get(Edge.sorted(a, b)).isDefined
  }

  /** returns all edges that include this point */
  def adjacentEdges(p: Point): Map[Int, Edge] = {
    def edge(heading: Int): Edge = heading match {
      case Heading.N => Edge.sorted(p, p.n)
      case Heading.S => Edge.sorted(p, p.s)
      case Heading.E => Edge.sorted(p, p.e)
      case Heading.W => Edge.sorted(p, p.w)
      case _ => throw new Exception(s"invalid heading: ${heading}")
    }
    Heading.all.asScala.toSeq.map(_.intValue).flatMap { heading =>
      val e = edge(heading)
      if(edges.contains(e)){
        Some(heading -> e)
      }else{
        None
      }
    }.toMap
  }


  override def toString: String = {
    if(nodes.isEmpty){
      return ""
    }

    def getxy(x: Int, y: Int): Option[V] ={
      val k = nodes.keys.filter(p => p.x == x && p.y == y).toSeq.sortBy(_.z).headOption
      k.flatMap(p => nodes.get(p))
    }

    // TODO print edges
    // remember, positive Y goes down
    val width = nodes.values.map(_.toString.length).max
    val xs = nodes.keys.map(_.x)
    val ys = nodes.keys.map(_.y)
    val lines = (ys.min to ys.max).map{ y =>
      val nodeline = (xs.min to xs.max).map{ x =>
        val node: String = getxy(x, y).map(_.toString).getOrElse("")
        val s = node.padTo(width, ' ')

        val rightEdge = if(containsEdge(Point(x,y,0), Point(x+1,y,0))){ "-" }else{ "" }
        s + rightEdge.padTo(width, ' ')
      }.mkString("")
      val verticalEdges = (xs.min to xs.max).map { x =>
        val e = if (containsEdge(Point(x,y, 0), Point(x,y+1, 0))){ "|" }else{""}
        e.padTo(width, ' ') + "".padTo(width, ' ')
      }.mkString("")

      s"${nodeline}\n${verticalEdges}"
    }
    lines.mkString("\n")
  }
}

/**
  * Prototype that generates a "logical" map (a Graph) of locations and key/gate rooms.
  */
class RandomWalkGenerator(r: RandomX) {

  private def randomStep(current: Point, map: LogicalMap[String, String]): Option[Point] = {
    r.randomElementOpt(map.emptyAdj(current))
  }

  private def randomWalk(start: Point, map: LogicalMap[String, String], value: String, stepCount: Int): Point = {
    var current = start
    for(_ <- 0 until stepCount){
      val next = randomStep(current, map).get // TODO: DFS and backtrack if no options
      map.put(next, value)
      map.putEdge(current, next, "")
      current = next
    }
    current
  }

  private def attachGate(searchLevels: Seq[String], gate: String, map: LogicalMap[String, String]): Point = {
    val current = r.randomElement(map.nodes.filter { case (p, v) => searchLevels.contains(v) && map.emptyAdj(p).nonEmpty }.keys)
    val gateLoc = randomStep(current, map).get
    map.put(gateLoc, gate)
    map.putEdge(current, gateLoc, "")
    gateLoc
  }

  private def addGateKey(at: String, gatekey: String, map: LogicalMap[String, String]): Point = {
    val p = r.randomElement(map.nodes.filter{case (p, v) => v == at })._1
    map.put(p, gatekey, overwrite = true)
    p
  }



  def generate(): LogicalMap[String, String] = {
    val map = LogicalMap[String, String]()

    var current = Point(0, 0, 0)
    map.put(current, "S")
    randomWalk(current, map, "0", 3)

    // pick place to add gateway
    current = attachGate(Seq("0"), "G1", map)
    randomWalk(current, map, "1", 3)

    current = attachGate(Seq("1", "0"), "G2", map)
    randomWalk(current, map, "2", 3)

    current = attachGate(Seq("2", "1", "0"), "G3", map)
    val last = randomWalk(current, map, "3", 3)

    val end = randomStep(last, map).get
    map.put(end, "E")
    map.putEdge(last, end, "")

    addGateKey("0", "K1", map)
    addGateKey("1", "K2", map)
    addGateKey("2", "K3", map)


    map
  }

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

object MoonBase2 {

  def getMoon2Map(): String = HardcodedConfig.getEduke32Path("moon2.map")



  def main(args: Array[String]): Unit = {
    // Testing Logical Map
//     val r = new RandomX()
//     val map = new RandomWalkGenerator(r).generate()
//     println(map)

    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    run(gameCfg)
  }

  // a single is a room with one entry
  def rotateSingleToEdge(sg: SectorGroup, headings: Iterable[Int]): SectorGroup = {
    // TODO this is a hack:  all singles simply have their entrace on the west side
    if(headings.isEmpty){
      throw new Exception("headings is empty")
    }
    // println(headings.head)
    headings.head match {
      case Heading.N => sg.rotateCW
      case Heading.E => sg.rotate180
      case Heading.S => sg.rotateCCW
      case Heading.W => sg
    }
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
    println(logicalMap)

    val startSg = moonPalette.getSG(1)
    val fourWay = moonPalette.getSG(2) // actually supports any number of connections
    val endSg = moonPalette.getSG(3)
    val keySg = moonPalette.getSG(4)
    val gateSgLeftEdge = moonPalette.getSG(5)
    val gateSgTopEdge = moonPalette.getSG(6)
    val gateSg3 = moonPalette.getSG(7)
    val windowRoom1 = moonPalette.getSG(8)
    val conferenceRoom = moonPalette.getSG(9)
    //val conferenceRoomVertical = moonPalette.getSG(9).rotateCW
    //writer.pasteSectorGroup(startSg, new PointXYZ(0, 0, 0))

    // NOTE:  some of my standard space doors are 2048 wide

    val gridSize = 12 * 1024 // TODO this will be different for every row and column
    val marginSize = 1024 // TODO will be different
    val originPoint = logicalMap.center // this point goes at 0, 0

    val keycolors: Seq[Int] = random.shuffle(DukeConfig.KeyColors).toSeq

    val pastedGroups = mutable.Map[Point, PastedSectorGroup]()
    logicalMap.nodes.foreach { case (gridPoint, nodeType) =>
      val x = gridPoint.x - originPoint.x
      val y = gridPoint.y - originPoint.y
      val z = gridPoint.z - originPoint.z
      val mapPoint = new PointXYZ(x, y, z).multipliedBy(gridSize + marginSize)

      val sg = nodeType match {
        case "S" => {
          rotateSingleToEdge(startSg, logicalMap.adjacentEdges(gridPoint).keys)
        }
        case "E" => {
          rotateSingleToEdge(endSg, logicalMap.adjacentEdges(gridPoint).keys)
        }
        case s if s.startsWith("K") => {
          val keycolor: Int = keycolors(s(1).toString.toInt - 1)
          keySg.withKeyLockColor(gameCfg, keycolor)
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
          gateSg.withKeyLockColor(gameCfg, keycolor) // TODO add a withkeylockcolor() function just like MoonBase1 withLockColor
        }
        case _ => {
          val edges = logicalMap.adjacentEdges(gridPoint)
          if(edges.size == 2 && edges.contains(Heading.W) && edges.contains(Heading.E)) {
            random.randomElement(Seq(windowRoom1, conferenceRoom))
          }else if(edges.size == 2 && edges.contains(Heading.N) && edges.contains(Heading.S)){
            fourWay // conferenceRoomVertical
          }else{
            fourWay

          }
        }
      }
      // println(mapPoint)
      val psg = writer.pasteSectorGroupAt(adjustEnemies(random, sg), mapPoint)
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
