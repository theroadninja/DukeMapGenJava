package trn.render

import trn.prefab.{GameConfig, PrefabPalette}
import trn.render.StairPrinter.writeCurvedStairs
import trn.{BuildConstants, LineSegmentXY, Main, MapLoader, PlayerStart, PointXY, Map => DMap}
import trn.BuildConstants._

import scala.collection.JavaConverters._

/** functions as a main class for experimenting with render code */
object RenderQuickTest {

  // TODO - constants for number of steps duke can walk without jumping, and max number he can jump
  val ZStepHeight = BuildConstants.ZStepHeight

  // reads a test map and prints stuff, for reverse engineering xrepeat and xpan
  def xrepeatTest(loader: MapLoader, gameCfg: GameConfig): Unit = {
    val sourceMap = loader.load("xrepeat.map")
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true);



    // For measuring x-repeat:
    val allWalls = palette.allSectorGroups().asScala.flatMap(sg => sg.getAllWallViews).toSeq
    // allWalls.filter(_.tex() != 0).foreach(w => println(s"${w.tex}, xrepeat=${w.xRepeat} length=${w.length}"))
    allWalls.filter(_.tex != 0).foreach { w =>
      println(s"tex=${w.tex} xrepeat=${w.xRepeat} xpan=${w.xPan()}")
    }
  }

  def otherTest(loader: MapLoader, gameCfg: GameConfig): Unit = {

    val sourceMap = loader.load("output.map")
    sourceMap.sectors.asScala.foreach(s => println(s"floor=${s.getFloorZ} ceil=${s.getCeilingZ}"))
  }

  def quickTest(loader: MapLoader, gameCfg: GameConfig): Unit = {

    // otherTest(loader, gameCfg)

    val sourceMap = loader.load("stairs.map")
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap, true);

    def p(x: Int, y: Int): PointXY = new PointXY(x, y)

    val map: DMap = DMap.createNew

    val WallTex = Texture(1097, gameCfg.textureWidth(1097)) // or 791, 1097


    // // nice half-circle
    // val leftP0 = p(0, 0)
    // val leftP1 = p(2048, 0)
    // val rightP0 = p(4096 + 1024, 0)
    // val rightP1 = p(5120 + 1024, 0)

    // // straight stair using curved -- problems
    // // val leftP0 = p(0, 0)
    // // val leftP1 = p(0, 2048)
    // // val rightP0 = p(4096, 2048)
    // // val rightP1 = p(4096, 0)

    // writeCurvedStairs(map, leftP0, leftP1, rightP0, rightP1, WallTex)


    // // Strait Stair Test
    // val floor0 = BuildConstants.DefaultFloorZ
    // val ceil0 = BuildConstants.DefaultCeilZ
    // val floor2 = DefaultFloorZ - DefaultSectorHeight * 2
    // val ceil2 = floor2 - BuildConstants.DefaultSectorHeight
    val floor2 = BuildConstants.DefaultFloorZ
    val ceil2 = BuildConstants.DefaultCeilZ
    val floor0 = DefaultFloorZ - DefaultSectorHeight * 2
    val ceil0 = floor0 - BuildConstants.DefaultSectorHeight

    val sector0 = MiscPrinter.box(map, p(-2048, 0), p(0, 2048), floor0, ceil0)
    val e0 = StairEntrance(p(0, 0), p(0, 2048), map.getSector(sector0))


    val sector1 = MiscPrinter.box(map, p(4096, 0), p(4096 + 2048, 2048), floor2, ceil2)
    // val e1 = StairEntrance(p(4096, 2048), p(4096, 0), floor2, ceil2, None)
    val e1 = StairEntrance(p(4096, 2048), p(4096, 0), map.getSector(sector1))
    StairPrinter.straightStairs(map, e0, e1, WallTex)

    MiscPrinter.lazyAutoLinkEverything(map)

    // TODO - idea: separate construction of sectors and walls from how they are painted?


    //val sector0 = MiscPrinter.box(map, p(-2048, -2048), p(0, 0), BuildConstants.DefaultFloorZ, BuildConstants.DefaultCeilZ)
    //val wall0 = map.getAllWallLoopsAsViews(sector0).asScala.flatMap(_.asScala).find{ w =>
    //  w.getLineSegment.equals(new LineSegmentXY(p(0, -2048), p(0, 0)))
    //}.get.getWallId

    //val floor = BuildConstants.DefaultFloorZ - BuildConstants.ZStepHeight * 40
    //val sector1 = MiscPrinter.box(map, p(4096, -2048), p(4096 + 2048, 0), floor, floor - BuildConstants.DefaultSectorHeight)
    //val wall1 = map.getAllWallLoopsAsViews(sector1).asScala.flatMap(_.asScala).find{ w =>
    //  w.getLineSegment.equals(new LineSegmentXY(p(4096, 0), p(4096, -2048)))
    //}.get.getWallId
    //StairPrinter.straightStairs(map, sector0, wall0, sector1, wall1, StairTextures(WallTex))


    map.setPlayerStart(new PlayerStart(512, 512, 0, PlayerStart.NORTH))
    Main.deployTest(map)
  }

}
