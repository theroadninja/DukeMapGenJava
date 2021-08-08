package trn.bespoke.moonbase2

import trn.duke.Lotags
import trn.{BuildConstants, DukeConstants, FVectorXY, HardcodedConfig, LineSegmentXY, LineXY, Main, MapLoader, MapUtil, PlayerStart, PointXY, PointXYZ, Sector, Sprite, Wall, WallView, Map => DMap}
import trn.prefab.{CompassWriter, DukeConfig, GameConfig, MapWriter, PastedSectorGroup, RandomX, RedwallConnector}
import trn.render.{MiscPrinter, Texture, WallAnchor, WallPrefab}


/**
  * All of the textures needed to paint the elevator.  Maybe also shading rules?
  * @param wallTex
  */
case class ElevatorPassageConfig(  // TODO change 'Config' to something else
  wallTex: WallPrefab,
  floorTex: Texture,
  ceilTex: Texture,
  elevatorFloorTex: Texture,
  elevatorCeilTex: Texture,

  /** the texture on the walls of the moving part of the elevator */
  elevatorSideTex: Texture,

  /** the texture you face as you enter the elevator (on the opposite wall of the elevator side tex) */
  rails: WallPrefab,

  /** (start sound, stop sound) */
  sounds: (Int, Int)

)

object ElevatorPassageConfig {

  /** from E2L6 */
  val DefaultSounds = (71, 73)

  /** for unit tests */
  def testDefaults(): ElevatorPassageConfig = {
    ElevatorPassageConfig(WallPrefab(Texture(0, 128)), Texture(0, 128), Texture(0, 128), Texture(0, 128), Texture(0, 128), Texture(0, 128), WallPrefab(Texture(0, 128)), DefaultSounds)
  }
}

case class ResultAnchor(wall: WallView, sectorId: Int)

/**
  * Draws a passage from wall A to wall B, creating stairs (ramps?) or a lift to nicely adjust the z height.
  *
  * TODO: rename this to "strait" or something
  *
  * Previous I was going to call this "DeltaZPassagePrinter"
  */
object SpacePassagePrinter {

  def sfxSprite(p: PointXY, z: Int, sectorId: Int, hitag: Int, lotag: Int): Sprite = {
    val sfx = new Sprite(p, z, sectorId)
    sfx.setTexture(5) // TODO 5 is hardcoded picnum for MUSICANDSFX sprite
    sfx.setHiTag(hitag)
    sfx.setLotag(lotag)
    sfx
  }

  def printHallway(
    gameCfg: GameConfig,
    writer: MapWriter,
    psgA: PastedSectorGroup,
    connA: RedwallConnector,
    psgB: PastedSectorGroup,
    connB: RedwallConnector
  ): Unit = {
    // MapUtil.getFloorZAtWall()
    ???
  }


  /**
    *
    *     1                  0
    *     /\                 |
    *   A |                  | B
    *     |                 \/
    *     0                 1
    *
    * @param gameCfg
    * @param map
    * @param wallA
    * @param wallB
    */
  def printSpacePassage(
    gameCfg: GameConfig,
    map: DMap,
    wallA: WallAnchor,
    wallB: WallAnchor,
  ): Unit = {

    if (wallA.width < BuildConstants.MinHallwayWidth || wallB.width < BuildConstants.MinHallwayWidth){
      throw new Exception("entrace width too narrow")
    }
    require(wallA.axisAligned && wallB.axisAligned, "wall anchors must be axis-aligned")


    ???
  }

  private[moonbase2] def printSpacePassageElevator(
    gameConfig: GameConfig,
    map: DMap,
    wallA: WallAnchor,
    wallB: WallAnchor,
    config: ElevatorPassageConfig
  ): (ResultAnchor, ResultAnchor) = {
    require(wallA.width >= BuildConstants.MinHallwayWidth && wallB.width >= BuildConstants.MinHallwayWidth)
    require(wallA.axisAligned && wallB.axisAligned, "wall anchors must be axis-aligned")
    val width: Int = wallA.width.toInt
    require(width == wallB.width.toInt)

    val aVector = wallA.p1.subtractedBy(wallA.p0)
    val whatever = wallB.p1.subtractedBy(wallA.p0)
    require(aVector.crossProduct2d(whatever) > 0) // the vector from A0->B1 should be right 90 degrees from A0->A1

    // val liftFloorText = gameCfg.tex(380)
    // side tex with rails:  353
    // St 18 is elevator down (with ceiling), St 19 is elevator up (with ceiling)

    val liftSize = if(wallA.width < 512) {
      512
    }else if(wallA.width < 3072){
      1024
    }else{
      2048
    }

    val (c2, c3, c4, c5, lengthA, lengthB) = elevatorControlPoints(wallA, wallB, liftSize)
    if(width < liftSize){
      /*
       *             T0------T1
       *             |       |
       *     1 ....  C2      C3...... 0
       *     /\      |       |        |
       *   A |       |  E    |        | B
       *     |       |       |       \/
       *     0 ..... C5      C4..... 1
       *             |       |
       *             B1------B0
       */

      ???
    }else if(width == liftSize){
      /*
       *     1-------C2------C3-------0
       *     /\      |       |        |
       *   A |       |  E    |        | B
       *     |       |       |       \/
       *     0-------C5------C4------1
       */
      val wp = config.wallTex //WallPrefab(config.wallTex)
      val ep = WallPrefab(config.elevatorSideTex).copy(xTileRepeat = Some(1), xrepeat = None, yrepeat=Some(16))
      val left: Seq[Wall] = Seq((wallA.p0, wp), (wallA.p1, wp), (c2, ep), (c5, wp)).map{case (p, tex) => MiscPrinter.wall(p, tex)}
      val leftId = map.createSectorFromLoop(left: _*)
      val leftSector = map.getSector(leftId)
      leftSector.setFloorZ(wallA.floorZ)
      leftSector.setCeilingZ(wallA.ceilZ)
      leftSector.setFloorTexture(config.floorTex.picnum)
      leftSector.setCeilingTexture(config.ceilTex.picnum)

      val lift = Seq((c2, wp), (c3, config.rails), (c4, wp), (c5, config.rails)).map{ case (p, prefab) => MiscPrinter.wall(p, prefab)}
      val liftId = map.createSectorFromLoop(lift: _*)
      MiscPrinter.autoLinkRedWalls(map, leftId, liftId)
      val liftSector = map.getSector(liftId)
      liftSector.setFloorTexture(config.elevatorFloorTex.picnum)
      liftSector.setFloorRelative(true)
      liftSector.setCeilingTexture(config.elevatorCeilTex.picnum)
      liftSector.setCeilingRelative(true)
      liftSector.setFloorZ(wallA.floorZ)
      liftSector.setCeilingZ(wallA.ceilZ)

      val sfx = sfxSprite(
        new LineSegmentXY(c2, c4).midpoint(),
        liftSector.getFloorZ,
        liftId,
        config.sounds._2,
        config.sounds._1
      )
      // sfx.setTexture(5) // TODO 5 is hardcoded picnum for MUSICANDSFX sprite
      map.addSprite(sfx)


      val right = Seq((wallB.p0, wp), (wallB.p1, wp), (c4, ep), (c3, wp)).map{case (p, prefab) => MiscPrinter.wall(p, prefab)}
      val rightId = map.createSectorFromLoop(right: _*)
      val rightSector = map.getSector(rightId)
      rightSector.setFloorZ(wallB.floorZ)
      rightSector.setCeilingZ(wallB.ceilZ)
      rightSector.setFloorTexture(config.floorTex.picnum)
      rightSector.setCeilingTexture(config.ceilTex.picnum)
      MiscPrinter.autoLinkRedWalls(map, liftId, rightId)

      val liftTag = if(wallA.floorZ < wallB.floorZ){
        // means wallA is HIGHER
        gameConfig.ST.elevatorDown
      }else {
        gameConfig.ST.elevatorUp
      }
      map.getSector(liftId).setLotag(liftTag)

      // TODO:  shading!

      (ResultAnchor(CompassWriter.westWalls(map, leftId).head, leftId), ResultAnchor(CompassWriter.eastWalls(map, rightId).head, rightId))


    }else{
      /*
       *     1 ....  C2..... C3...... 0
       *     /\      .       |        |
       *     |       I0------I1       |
       *     |       |       |        |
       *   A |       |  E    |        | B
       *     |       |       |        |
       *     |       I3------I2       |
       *     |       .       |       \/
       *     0 ..... C5..... C4..... 1
       */

      ???
    }

  }

  /**
    *
    *     1 ....  C2..... C3...... 0
    *     /\                       |
    *   A |                        | B
    *     |                       \/
    *     0 ..... C5..... C4..... 1
    *
    * @param wallA
    * @param wallB
    * @returns (C2, C3, C4, C5, lengthA, lengthB)
    */
  private[moonbase2] def elevatorControlPoints(
    wallA: WallAnchor,
    wallB: WallAnchor,
    elevatorSize: Int
  ): (PointXY, PointXY, PointXY, PointXY, Int, Int) = {

    require(! new LineSegmentXY(wallA.p1, wallB.p0).intersects(new LineSegmentXY(wallB.p1, wallA.p0)))

    // make sure the anchors are "facing" each other
    val aVector = wallA.p1.subtractedBy(wallA.p0)
    require(aVector.crossProduct2d(wallB.p1.subtractedBy(wallA.p0)) > 0) // the vector from A0->B1 should be right 90 degrees from A0->A1
    val bVector = wallB.p1.subtractedBy(wallB.p0)
    require(bVector.crossProduct2d(wallA.p1.subtractedBy(wallB.p0)) > 0)

    val b2a = wallA.p0.subtractedBy(wallB.p1).toF
    val a2b = wallB.p0.subtractedBy(wallA.p1).toF
    require(b2a.length.toInt == a2b.length.toInt)

    val totalLength: Int = a2b.length.toInt
    val lengthA = (totalLength - elevatorSize) / 2
    val lengthB = totalLength - elevatorSize - lengthA

    val vc2 = a2b.normalized.multipliedBy(lengthA)
    val c2 = wallA.p1.add(toI(vc2))

    val vc3 = a2b.normalized.multipliedBy(lengthA + elevatorSize)
    val c3 = wallA.p1.add(toI(vc3))

    val vc4 = b2a.normalized.multipliedBy(lengthB)
    val c4 = wallB.p1.add(toI(vc4))

    val vc5 = b2a.normalized.multipliedBy(lengthB + elevatorSize)
    val c5 = wallB.p1.add(toI(vc5))

    (c2, c3, c4, c5, lengthA, lengthB)
  }

  private def toI(f: FVectorXY): PointXY = new PointXY(f.x.toInt, f.y.toInt)

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    val writer = MapWriter(gameCfg)


    val leftSectorId = MiscPrinter.box(writer.getMap, new PointXY(0, 0), new PointXY(2048, 1024), BuildConstants.DefaultFloorZ, BuildConstants.DefaultCeilZ);
    val rightSectorId = MiscPrinter.box(writer.getMap, new PointXY(6144, 0), new PointXY(8192, 1024), 73728, BuildConstants.DefaultCeilZ);

    val wallA = WallAnchor.fromExistingWall(CompassWriter.eastWalls(writer.getMap, leftSectorId).head)
    val wallB = WallAnchor.fromExistingWall2(CompassWriter.westWalls(writer.getMap, rightSectorId).head, writer.getMap.getSector(rightSectorId))

    val textures = ElevatorPassageConfig(WallPrefab(gameCfg.tex(258)), gameCfg.tex(183), gameCfg.tex(181), gameCfg.tex(380), gameCfg.tex(126), gameCfg.tex(354), WallPrefab(gameCfg.tex(353)), ElevatorPassageConfig.DefaultSounds)
    val (resultLeft, resultRight) = SpacePassagePrinter.printSpacePassageElevator(gameCfg, writer.outMap, wallA, wallB, textures)

    MiscPrinter.autoLinkRedWalls(writer.getMap, leftSectorId, resultLeft.sectorId)
    MiscPrinter.autoLinkRedWalls(writer.getMap, rightSectorId, resultRight.sectorId)


    // ////////////////////////
    writer.disarmAllSkyTextures()
    writer.setAnyPlayerStart(force = true)
    writer.sgBuilder.clearMarkers()
    writer.checkSectorCount()
    Main.deployTest(writer.outMap, "output.map", HardcodedConfig.getEduke32Path("output.map"))
  }
}
