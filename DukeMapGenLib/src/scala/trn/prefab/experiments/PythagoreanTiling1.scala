package trn.prefab.experiments

import trn.math.SnapAngle
import trn.{AngleUtil, BuildConstants, HardcodedConfig, MapLoader, PointXY, PointXYZ, RandomX, Map => DMap}
import trn.prefab.{BoundingBox, DukeConfig, GameConfig, Heading, MapWriter, PastedSectorGroup, PrefabPalette, RedwallConnector, SectorGroup}
import trn.render.{Texture, WallPrefab}

import scala.collection.mutable.ArrayBuffer



object PythTileType {
  val BigTile = 0
  val SmallTile = 1

  def tileType(col: Int, row: Int): Int = if(Math.abs(row) % 2 == 0) { BigTile }else{ SmallTile }
}

// origin is sort of the top left, if it was rotated to be square
// tiles on the right will be above the origin
class PythagoreanTiling(origin: PointXY, bigW: Int, smallW: Int){
  require(bigW > smallW)

  def tileCoordinates(col: Int, row: Int): BoundingBox = {
    // TODO:  this is locked to big tiles being 2x the little ones
    if(row % 2 == 0){
      // big tile
      val x = bigW * col + smallW * (row/2)
      val y = bigW * (row/2) - smallW * col
      BoundingBox(x, y, x + bigW, y + bigW).translate(origin)
    } else {
      // little tile
      val x = bigW * col + smallW * ((row-1)/2)
      val y = bigW * ((row-1)/2) - smallW * col + bigW
      BoundingBox(x, y, x + smallW, y + smallW).translate(origin)
    }
  }
}

object PythagoreanTiling {
  def apply(origin: PointXY, bigW: Int, smallW: Int) = new PythagoreanTiling(origin, bigW, smallW)
}


/**
  *             |    c, r-1     |
  *             |               |
  *   c-1, r-1  +-------+-------+--
  *             |       |
  *             | (c,r) |
  *  ---+-------+-------+   c, r+1
  *     |               |
  *     |   c-1, r+1    |
  *
  */
object SmallTileEdge {
  val E = 1
  val S = 2
  val W = 3
  val N = 4
  val all = Seq(E, S, W, N)

  def edge(from: (Int, Int), to: (Int, Int)): Option[Int] = {
    require(Math.abs(from._2) % 2 == 1)
    val dx = to._1 - from._1
    val dy = to._2 - from._2
    (dx, dy) match {
      case (0, 1) => Some(E)
      case (-1, 1) => Some(S)
      case (-1, -1) => Some(W)
      case (0, -1) => Some(N)
      case _ => None
    }
  }

  def rotateCW(edgeId: Int): Int = {
    require(edgeId > 0 && edgeId < 5)
    if(edgeId == N){
      E
    }else{
      edgeId + 1
    }
  }

  def rotationToMatch(from: Int, to: Int): SnapAngle = {
    require(all.contains(from) && all.contains(to))
    // see also SnapAngle.rotateUntil()
    lazy val r90 = rotateCW(from)
    lazy val r180 = rotateCW(r90)
    if (from == to) {
      SnapAngle(0)
    } else if (r90 == to) {
      SnapAngle(1)
    } else if (r180 == to) {
      SnapAngle(2)
    } else {
      SnapAngle(3)
    }
  }

  def opposite(a: Int, b: Int): Boolean = Math.abs(a - b) == 2
}

/**
  *                   |
  *                   +-----+---------------
  *         (c, r-2)  |+1,-1|
  *             NB    | NS  |
  *  ----+------------+-----+
  *    WS|                  |
  * c,r-1|                  |
  *  ----+                  |EB (c+1, r)
  *      |                  |
  *    WB|       (c,r)      +-----+
  * c-1,r|                  |ES   |
  *      |                  |+1,+1|
  *      +-----+------------+-----+
  *      | SS  |     SB           |
  *      |c,r+1|     (c, r+2)     |
  * -----+-----+                  |
  *
  */
object BigTileEdge {
  // 1-based so that these IDs can match the connectors
  val ES = 1 // east, small
  val EB = 2 // east, big
  val SS = 3 // south, small
  val SB = 4
  val WS = 5
  val WB = 6
  val NS = 7
  val NB = 8
  val all = Seq(ES, EB, SS, SB, WS, WB, NS, NB)

  /**
    * If `to` is next to `from`, it returns which edge on `from` that `to` is touching
    * @param from (col, row)
    * @param to (col, row)
    * @return
    */
  def edge(from: (Int, Int), to: (Int, Int)): Option[Int] = {
    require(from._2 % 2 == 0)
    val dx = to._1 - from._1
    val dy = to._2 - from._2
    (dx, dy) match {
      case (1, 0) => Some(EB)
      case (1, 1) => Some(ES)
      case (0, 2) => Some(SB)
      case (0, 1) => Some(SS)
      case (-1, 0) => Some(WB)
      case (0, -1) => Some(WS)
      case (0, -2) => Some(NB)
      case (1, -1) => Some(NS)
      case _ => None
    }
  }
}

trait TileMaker {
  def makeTile(edges: Seq[Int]): SectorGroup

}

object TileMaker {
  def attachHallway(gameConfig: GameConfig, sg: SectorGroup, attachments: Map[Int, SectorGroup], attachId: Int): SectorGroup = {
    val hallwaySg = attachments(attachId)
    val connId = attachId
    sg.withGroupAttached(gameConfig, sg.getRedwallConnectorsById(connId).head, hallwaySg, hallwaySg.getRedwallConnectorsById(connId).head)
  }

}

class BigTile1(gameConfig: GameConfig, palette: PrefabPalette) extends TileMaker {
  def makeTile(edges: Seq[Int]): SectorGroup = {
    // TODO should attachments() be a standard part of the trait?
    val attachments = Map(
      BigTileEdge.ES -> palette.getSG(2),
      BigTileEdge.EB -> palette.getSG(3),
      BigTileEdge.SS -> palette.getSG(4),
      BigTileEdge.SB -> palette.getSG(5),
      BigTileEdge.WS -> palette.getSG(6),
      BigTileEdge.WB -> palette.getSG(7),
      BigTileEdge.NS -> palette.getSG(8),
      BigTileEdge.NB -> palette.getSG(9),
    )

    var center = palette.getSG(1)
    edges.foreach { attachId =>
      center = TileMaker.attachHallway(gameConfig, center, attachments, attachId)
    }
    center
  }
}

class BigTile2(gameConfig: GameConfig, palette: PrefabPalette) extends TileMaker {
  def makeTile(edges: Seq[Int]): SectorGroup = palette.getSG(15)
}

class SmallTile1(gameConfig: GameConfig, palette: PrefabPalette) extends TileMaker {
  def makeTile(edges: Seq[Int]): SectorGroup = {
    val attachments = Map(
      SmallTileEdge.E -> palette.getSG(11),
      SmallTileEdge.S -> palette.getSG(12),
      SmallTileEdge.W -> palette.getSG(13),
      SmallTileEdge.N -> palette.getSG(14),
    )
    var center = palette.getSG(10)
    edges.foreach { attachId =>
      center = TileMaker.attachHallway(gameConfig, center, attachments, attachId)
    }
    center
  }
}

class SmallTile2(gameConfig: GameConfig, palette: PrefabPalette) extends TileMaker {

  val DeadEnd = palette.getSG(20)
  val Corner = palette.getSG(16)
  val Straight = palette.getSG(17)
  val T = palette.getSG(18)
  val Fourway = palette.getSG(19)

  def makeTile(edges: Seq[Int]): SectorGroup = {
      edges.size match {
        case 1 => {
          // the prefab starts facing east
          val angle = SmallTileEdge.rotationToMatch(SmallTileEdge.E, edges.head)
          angle * DeadEnd
        }
        case 2 if SmallTileEdge.opposite(edges(0), edges(1)) => {
          // the prefab starts E-W
          if(edges.contains(SmallTileEdge.E)){
            Straight
          }else{
            Straight.rotatedCW
          }
        }
        case 2 => {
          // it starts out with connections a N and E
          if(edges.contains(SmallTileEdge.N) && edges.contains(SmallTileEdge.E)){
            // E is Less than N so we hardcode this one
            Corner
          } else {
            val angle = SmallTileEdge.rotationToMatch(SmallTileEdge.N, edges.sorted.head)
            angle * Corner
          }
        }
        case 3 => {
          // the missing edge stats off S
          val angle = SmallTileEdge.rotationToMatch(SmallTileEdge.S ,SmallTileEdge.all.filterNot(edges.contains).head)
          angle * T
        }
        case 4 => Fourway
        case _ => throw new Exception(s"invalid edges: ${edges}")
      }
  }

}

object PythagoreanTiling1 {

  def renderBox(gameCfg: GameConfig, map: DMap, box: BoundingBox): Unit = {
    val w = WallPrefab(gameCfg.tex(461))
    val walls = Seq(
      box.topLeft, box.topRight, box.bottomRight, box.bottomLeft
    ).map(w.create)

    val sectorId = map.createSectorFromLoop(walls: _*)
  }



  def printTiles(gameCfg: GameConfig, writer: MapWriter, tiling: PythagoreanTiling): Unit = {
    for(col <- 0 until 8){
      for (row <- 0 until 16){
        renderBox(gameCfg, writer.outMap, tiling.tileCoordinates(col, row))
      }
    }

  }

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile, HardcodedConfig.getAtomicHeightsFile)
    run(gameCfg)
  }
  def run(gameCfg: GameConfig): Unit = {
    val random = new RandomX()

    val pyth1 = MapLoader.loadPalette(HardcodedConfig.getEduke32Path("PYTH1.MAP"))
    val width = 4096 // * 3 = 12,288

    val writer = MapWriter(gameCfg)
    val tiling = PythagoreanTiling(
      new PointXY(BuildConstants.MIN_X, 0),
      3 * width, // 2 * width,
      width
    )

    val psgs: ArrayBuffer[PastedSectorGroup] = ArrayBuffer()

    val bigtilemaker1 = new BigTile1(gameCfg, pyth1)
    val bigtilemaker2 = new BigTile2(gameCfg, pyth1)
    val smalltilemaker1 = new SmallTile1(gameCfg, pyth1)
    val smalltilemaker2 = new SmallTile2(gameCfg, pyth1)

    val coords = Seq(
      (0, -1), (1, -1), // some extra little ones for testing
      (0, 0), (1, 0), (2, 0),
      (0, 1), (1, 1), (2, 1),
      (0, 2), (1, 2), (2, 2),
      (0, 3), (1, 3), (2, 3),
      (0, 4), (1, 4), (2, 4),
      (1, 5), (0, 6) // extra big one to make a T with the little one
    )
    coords.foreach { case (col, row) =>
      if (PythTileType.tileType(col, row) == PythTileType.BigTile){
        val edges = coords.filterNot(_ == (col, row)).flatMap(coord => BigTileEdge.edge((col, row), coord))
        val maker = random.randomElement(Seq(bigtilemaker1, bigtilemaker2))
        val t = maker.makeTile(edges)
        psgs += writer.pasteSectorGroupAt(t, tiling.tileCoordinates(col, row).center.withZ(0), true)
      }else{
        val edges = coords.flatMap(coord => SmallTileEdge.edge((col, row), coord))
        // val t = smalltilemaker1.makeTile(edges)
        val t = smalltilemaker2.makeTile(edges)
        psgs += writer.pasteSectorGroupAt(t, tiling.tileCoordinates(col, row).center.withZ(0), true)
      }
    }

    // psgs += writer.pasteSectorGroupAt(bigtile, tiling.tileCoordinates(1, 0).center.withZ(0), true)
    // psgs += writer.pasteSectorGroupAt(bigtile, tiling.tileCoordinates(2, 0).center.withZ(0), true)
    // // println(psgs.last.redwallConnectors.filter(! _.isLinked(writer.getMap)).size)

    //  psgs += writer.pasteSectorGroupAt(smalltile, tiling.tileCoordinates(0, 1).center.withZ(0), true)
    //  psgs += writer.pasteSectorGroupAt(smalltile, tiling.tileCoordinates(1, 1).center.withZ(0), true)
    //  psgs += writer.pasteSectorGroupAt(smalltile, tiling.tileCoordinates(2, 1).center.withZ(0), true)

    // psgs += writer.pasteSectorGroupAt(bigtile2, tiling.tileCoordinates(0, 2).center.withZ(0), true)
    // psgs += writer.pasteSectorGroupAt(bigtile2, tiling.tileCoordinates(1, 2).center.withZ(0), true)
    // psgs += writer.pasteSectorGroupAt(bigtile2, tiling.tileCoordinates(2, 2).center.withZ(0), true)

    // psgs += writer.pasteSectorGroupAt(smalltile, tiling.tileCoordinates(0, 3).center.withZ(0), true)
    // psgs += writer.pasteSectorGroupAt(smalltile, tiling.tileCoordinates(1, 3).center.withZ(0), true)
    // psgs += writer.pasteSectorGroupAt(smalltile, tiling.tileCoordinates(2, 3).center.withZ(0), true)

    // psgs += writer.pasteSectorGroupAt(bigtile, tiling.tileCoordinates(0, 4).center.withZ(0), true)
    // psgs += writer.pasteSectorGroupAt(bigtile, tiling.tileCoordinates(1, 4).center.withZ(0), true)
    // psgs += writer.pasteSectorGroupAt(bigtile, tiling.tileCoordinates(2, 4).center.withZ(0), true)

    psgs.foreach { a =>
      psgs.foreach { b =>
        writer.autoLink(a, b)
      }
    }

    ExpUtil.finishAndWrite(writer)
  }
}
