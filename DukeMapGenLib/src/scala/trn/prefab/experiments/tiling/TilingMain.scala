package trn.prefab.experiments.tiling

import trn.{BuildConstants, HardcodedConfig, PointXY, RandomX}
import trn.prefab.{BoundingBox, DukeConfig, GameConfig, MapWriter, PastedSectorGroup, SectorGroup}
import trn.prefab.experiments.ExpUtil
import trn.prefab.experiments.tiling.hex.HexMap1
import trn.prefab.experiments.tiling.pyth.PythMap2

import scala.collection.mutable
import scala.collection.mutable.ArrayBuffer


/**
  * Any tiling where the tiles can be mapped onto 2d row & column coordinates
  */
trait Tiling {

  /** translate from the column & row in "tilespace" to the x,y coordinates in 2d Build space */
  def tileCoordinates(col: Int, row: Int): BoundingBox

  def edge(from: (Int, Int), to: (Int, Int)): Option[Int]

  def allEdges(shape: Int): Iterable[Int] = ??? // TODO make this required

  // TODO unit test all of these (pass the results of neightboors() through edge and make sure each is a valid and different edge
  def neighboors(coord: (Int, Int)): Seq[(Int, Int)]

  /** for tilings that use multiple shapes, identifies which shape */
  def shapeType(coords: (Int, Int)): Int = 0

}

object Tiling {
  /**
    * convenience method to add two coordinates together
    */
  final def add(coordA: (Int, Int))(coordB: (Int, Int)): (Int, Int) = {
    (coordA._1 + coordB._1, coordA._2 + coordB._2)
  }

}

/**
  *
  * @param name string that identifies the type of tile, e.g. "MedBay" or "Theatre"
  * @param edges  map of EdgeId ->  coordinate of neighboor
  */
case class TileNode(coord: (Int, Int), shape: Int, name: String, edges: Map[Int, TileEdge], plan: PlanNode) {
  // its col/row coordinate is its ID?

  // DONT store tiletype ... the "tiling" can tell ou that

  /** adds or replaces an edge */
  def withEdge(edge: TileEdge): TileNode = this.copy(edges = edges + (edge.edgeId -> edge))

  def findEdgeTo(neighboor: (Int, Int)): Option[Int] = edges.find(_._2.neighboorCoord == neighboor).map(_._1)

}

/**
  *
  * @param edgeId which edge of the tile -- the possible values are different for each tile shape
  * @param info  tells you something about the edge, e.g. whether is it special
  * @param neighboorCoord the 2d coordinate in tilespace of the neighboor on the other side of the edge
  */
case class TileEdge(edgeId: Int, neighboorCoord: (Int, Int), info: Option[String]) {
  def withInfo(newinfo: Option[String]): TileEdge = this.copy(info=newinfo)
}

object TileEdge {
  def apply(edgeId: Int, neighboorCoord: (Int, Int)): TileEdge = TileEdge(edgeId, neighboorCoord, None)
}






case class RenderedTile (
  tile: TileNode,
  psg: PastedSectorGroup
)

/**
  * Main algorithm for (hopefully) all types of tiling.
  */
object TilingMain {
  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile, HardcodedConfig.getAtomicHeightsFile)
    run(gameCfg)
  }
  def run(gameCfg: GameConfig): Unit = {
    val random = new RandomX()

    // val which = "hex"
    val which = "pyth2"

    if(which == "pyth") {
      //
      // Pythagorean Tiling
      //
      val inputmap: TileFactory = PythMap1
      val tiling = PythagoreanTiling(
        new PointXY(BuildConstants.MIN_X, 0),
        PythMap1.bigWidth, //3 * width, // 3 * 4096 == 12288
        PythMap1.smallWidth, //width
      )
      val input2 = new Outline(tiling)
      run2(gameCfg, random, inputmap, tiling)

    }else if(which == "pyth2"){
      val tileFactory = PythMap2()
      val tiling = tileFactory.tiling
      // val tilePlan = TilePlanner.fromHardcoded(
      //   tiling, PythMap2.TestMap
      // )
      val tilePlan = PythMap2.testMap(tiling)
      run2(gameCfg, random, tileFactory, tiling, Some(tilePlan))

    }else if(which == "pythoutline"){
      val tiling = PythagoreanTiling(
        new PointXY(BuildConstants.MIN_X, 0),
        PythMap1.bigWidth, //3 * width, // 3 * 4096 == 12288
        PythMap1.smallWidth, //width
      )
      run2(gameCfg, random, new Outline(tiling), tiling)


    }else if (which == "rect"){
      //
      // RectangleTiling
      //
      val tiling = new RectangleTiling(4096, 2 * 4096)
      run2(gameCfg, random, new RectOutline(tiling), tiling)

    }else if(which == "octo"){
      val tiling = new OctoDiamondTiling(2048, 1024)
      run2(gameCfg, random, new OctoDiOutline(tiling), tiling)

    }else if(which == "brick"){
      val tiling = new BrickTiling(2048, 1024, 1024)
      run2(gameCfg, random, new BrickOutline(tiling), tiling)
    }else if(which == "triangle"){
      val tiling = new TriangleTiling(4096)
      run2(gameCfg, random, new TriangleOutline(tiling), tiling)
    }else if(which == "hex"){
      // val tiling = new HexTiling(12288)
      val hex1 = HexMap1()
      run2(gameCfg, random, hex1, hex1.tiling)
    }
  }

  def run2(gameCfg: GameConfig, random: RandomX, inputmap: TileFactory, tiling: Tiling, tilePlanOpt: Option[TilePlan] = None): Unit = {
    // val pyth1 = MapLoader.loadPalette(PythMap1.inputMap)
    val writer = MapWriter(gameCfg)

    val tilePlan = tilePlanOpt.getOrElse(TilePlanner.generate(random, tiling))
    // val tilePlan = TilePlanner.fromHardcoded(
    //   tiling,
    //   Seq(
    //     (0, 0), (1, 0), (2, 0),
    //     // (0, 1), (1, 1), (2, 1),
    //     (0, 2), (1, 2), (2, 2),
    //     // (0, 3), (1, 3), (2, 3),
    //     (0, 4), (1, 4), (2, 4),
    //   )
    // )

    // Step 1 these are the places where we will put sector groups
    // val coords = Seq(
    //   (0, -1), (1, -1), // some extra little ones for testing
    //   (0, 0), (1, 0), (2, 0),
    //   (0, 1), (1, 1), (2, 1),
    //   (0, 2), (1, 2), (2, 2),
    //   (0, 3), (1, 3), (2, 3),
    //   (0, 4), (1, 4), (2, 4),
    //   (1, 5), (0, 6) // extra big one to make a T with the little one
    // )

    val coords = tilePlan.nodes.keys.toSeq

    // val coords = Seq(
    //   (0, 0), (1, 0), (2, 0),
    //   (0, 2), (1, 2), (2, 2),
    // )
    // def calcEdges(tiling: Tiling, coord: (Int, Int), neighboors: Seq[(Int, Int)]): Seq[Int] = {
    //   neighboors.flatMap(n => tiling.edge(coord, n))
    // }

    // Step 2 assign sector groups by name
    // TODO info about key/gate/level status goes here

    // THIS IS USEFUL FOR DEBUGGING!
    def allEdges(tileCoord: (Int, Int), coords: Seq[(Int, Int)]) = {
      coords.flatMap { neighboor =>
        val edgeIdOpt = tiling.edge(tileCoord, neighboor)
        edgeIdOpt.map(edgeId => TileEdge(edgeId, neighboor, None))
      }.map(edge => edge.edgeId -> edge).toMap
    }


    val tileNodes0 = coords.map { tileCoord =>
      val edges = tilePlan.getTileEdges(tileCoord)

      val planNode = tilePlan.get(tileCoord).get

      val name = inputmap.chooseTile(random, tileCoord, tiling.shapeType(tileCoord), planNode, edges.keys.toSeq)
      TileNode(tileCoord, tiling.shapeType(tileCoord), name, edges, planNode)
    }.map(t => t.coord -> t).toMap

    // Step 3 figure out special connections
    def addSpecialEdges(tile: TileNode, allTiles: Map[(Int, Int), TileNode]): TileNode ={
      tile.edges.foldLeft(tile) { case (t, (edgeId, edge)) =>
        val edgeInfo = inputmap.edgeInfo(t, edge, allTiles(edge.neighboorCoord))
        t.withEdge(edge.withInfo(edgeInfo))
      }
    }
    val tileNodes = tileNodes0.map {
      case(coord, tile) => coord -> addSpecialEdges(tile, tileNodes0)
    }


    // Step 4A:  render tiles
    val psgs = mutable.Map[(Int, Int), PastedSectorGroup]()
    coords.foreach { case (col, row) =>
      val tile = tileNodes((col, row))
      val t = inputmap.makeTile(gameCfg, tile)
      val psg = writer.pasteSectorGroupAt(t, tiling.tileCoordinates(col, row).center.withZ(0), true)
      psgs.put((col, row), psg)
    }
    // Step 4B: render special connections between tiles
    // cross product of all coords that could be connected (this thing's only job is to prevent processing the same edge twice)
    val distinctPairs = tileNodes.keys.flatMap { coord1 =>
      tileNodes.keys.filter(coord2 => tiling.edge(coord1, coord2).isDefined).map(coord2 => Seq(coord1, coord2).sorted).map(seq => (seq(0), seq(1)))
    }.toSet

    // TODO handle not just nodes with explicit connections, but also nodes that are next to each other with no connection,
    //  because we can put a "window connection" there later
    val edgePsgs = distinctPairs.flatMap {
      case (coordA, coordB) =>{
        val tileA = RenderedTile(tileNodes(coordA), psgs(coordA))
        val tileB = RenderedTile(tileNodes(coordB), psgs(coordB))
        (tileA.tile.findEdgeTo(coordB), tileB.tile.findEdgeTo(coordA)) match {
          case (Some(edgeA), Some(edgeB)) => {
            inputmap.makeEdge(writer, tileA, edgeA, tileB, edgeB)
          }
          case (None, None) => None
          case _ => {
            // TODO dont know why this isnt working, but it might be because of holes in the logical plan
            None
            // throw new RuntimeException("edge mismatch!")
          }
        }
      }
    }

    writer.autoLinkAll(psgs.values ++ edgePsgs)
    ExpUtil.finishAndWrite(writer)
  }

}
