package trn.prefab.experiments.pythtile

import trn.{BuildConstants, HardcodedConfig, PointXY, RandomX}
import trn.prefab.{BoundingBox, DukeConfig, GameConfig, MapWriter, PastedSectorGroup, SectorGroup}
import trn.prefab.experiments.ExpUtil

import scala.collection.mutable.ArrayBuffer


/**
  * Any tiling where the tiles can be mapped onto 2d row & column coordinates
  */
trait Tiling {
  def tileCoordinates(col: Int, row: Int): BoundingBox

  def calcEdges(coord: (Int, Int), neighboors: Seq[(Int, Int)]): Seq[Int]

  // TODO calcEdges should have a default implementation, and this one should be required:
  // def edge(from: (Int, Int), to: (Int, Int)): Option[Int] = {
}

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

    // val which = "pyth"
    val which = "pyth"

    if(which == "pyth"){
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
      val tiling = new HexTiling(6144)
      run2(gameCfg, random, new HexOutline(tiling), tiling)
    }




  }

  def run2(gameCfg: GameConfig, random: RandomX, inputmap: TileFactory, tiling: Tiling): Unit = {
    // val pyth1 = MapLoader.loadPalette(PythMap1.inputMap)
    val writer = MapWriter(gameCfg)

    // Step 1 these are the places where we will put sector groups
    val coords = Seq(
      (0, -1), (1, -1), // some extra little ones for testing
      (0, 0), (1, 0), (2, 0),
      (0, 1), (1, 1), (2, 1),
      (0, 2), (1, 2), (2, 2),
      (0, 3), (1, 3), (2, 3),
      (0, 4), (1, 4), (2, 4),
      (1, 5), (0, 6) // extra big one to make a T with the little one
    )
    // val coords = Seq(
    //   (0, 0), (1, 0), (2, 0),
    // )

    // Step 2 assign sector groups by name
    // TODO info about key/gate/level status goes here
    val tileEdges = coords.map { tileCoord =>
      tileCoord -> tiling.calcEdges(tileCoord, coords)
    }.toMap
    val groupNames  = coords.map { tileCoord =>
      tileCoord -> inputmap.chooseTile(random, tileCoord, PythTileType.tileType(tileCoord), tileEdges(tileCoord))
    }.toMap


    // TODO Step 3 - figure out connections (based on which groups are next to each other...have special connections, etc)


    // Step 4 ...
    def makeSectorGroup(coord: (Int, Int)): SectorGroup = {
      val tileType = PythTileType.tileType(coord)
      val edges = tileEdges(coord)
      val name = groupNames(coord)
      val maker = inputmap.getTileMaker(gameCfg, name, tileType)
      maker.makeTile(gameCfg, name, tileType, edges)
    }

    val psgs: ArrayBuffer[PastedSectorGroup] = ArrayBuffer()
    coords.foreach { case (col, row) =>
      val t = makeSectorGroup((col, row))
      psgs += writer.pasteSectorGroupAt(t, tiling.tileCoordinates(col, row).center.withZ(0), true)
    }

    writer.autoLinkAll(psgs)

    ExpUtil.finishAndWrite(writer)
  }

}
