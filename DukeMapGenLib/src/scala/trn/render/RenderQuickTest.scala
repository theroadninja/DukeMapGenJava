package trn.render

import trn.prefab.{GameConfig, PrefabPalette}
import trn.render.StairPrinter.writeCurvedStairs
import trn.{BuildConstants, LineSegmentXY, Main, MapLoader, PlayerStart, PointXY, Sector, WallView, Map => DMap}
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
    //val WallTex = Texture(791, gameCfg.textureWidth(791)) // or 791, 1097

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

    val verticalDrop = DefaultSectorHeight * 2
    //val verticalDrop = 1024 // for testing nearly flat steps
    //val verticalDrop = 0
    val floor2 = BuildConstants.DefaultFloorZ
    val ceil2 = BuildConstants.DefaultCeilZ - BuildConstants.ZStepHeight * 40
    val floor0 = DefaultFloorZ - verticalDrop
    println(s"using vertical drop: ${verticalDrop}, ${verticalDrop >> 2}")
    val ceil0 = floor0 - BuildConstants.DefaultSectorHeight

    def eastMostWall(sectorId: Int): WallView = {
      val walls = map.getAllWallLoopsAsViews(sectorId).asScala.map(_.asScala).flatten
      walls.filter(_.isAlignedY).sortBy(w => w.getLineSegment.getP1.x).last
    }
    def westMostWall(sectorId: Int): WallView = {
      val walls = map.getAllWallLoopsAsViews(sectorId).asScala.map(_.asScala).flatten
      walls.filter(_.isAlignedY).sortBy(w => w.getLineSegment.getP1.x).head
    }

    val start = -2048
    val boxWidth = 2048
    val stairLength = 4096
    // val stairLength = 16384 // good for 1
    //val stairLength = 5120 // in 2 range
    //val stairLength = 6144 // definitely in the 2 range

    val vd = BuildConstants.ztoxy(verticalDrop)
    val slope = vd.toDouble / stairLength.toDouble
    println(s"vertical drop: ${vd} vs length: ${stairLength} slope: ${slope} angle: ${Math.toDegrees(Math.atan(slope))}")

    //val stairLength = 2560 - ok
    //val stairLength = 3072 - ok

    val sector0 = MiscPrinter.box(map, p(start, 0), p(start + boxWidth, 2048), floor0, ceil0)
    val wall0 = eastMostWall(sector0)
    val e0 = StairEntrance(wall0, map.getSector(sector0), sector0)

    val box1start = start + boxWidth + stairLength
    val sector1 = MiscPrinter.box(map, p(box1start, 0), p(box1start + boxWidth, 2048), floor2, ceil2)
    val wall1 = westMostWall(sector1)
    val e1 = StairEntrance(wall1, map.getSector(sector1), sector1)
    StairPrinter.straightStairs(map, e0, e1, StairTex(WallTex))

    // TODO - idea: separate construction of sectors and walls from how they are painted?

    map.setPlayerStart(new PlayerStart(512, 512, 0, PlayerStart.NORTH))
    Main.deployTest(map)
  }

}
