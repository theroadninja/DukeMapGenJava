package trn.bespoke.moonbase2

import trn.duke.Lotags
import trn.{BuildConstants, DukeConstants, FVectorXY, HardcodedConfig, LineSegmentXY, LineXY, Main, MapLoader, MapUtil, PlayerStart, PointXY, PointXYZ, Sector, Sprite, Wall, WallView, render, Map => DMap}
import trn.prefab.{CompassWriter, DukeConfig, GameConfig, Heading, MapWriter, PastedSectorGroup, RandomX, RedwallConnector}
import trn.render.{HorizontalBrush, MiscPrinter, ResultAnchor, StairEntrance, StairPrinter, StairTex, Texture, WallAnchor, WallPrefab}

import collection.JavaConverters._

/**
  * All of the textures needed to paint the elevator.  Maybe also shading rules?
  * @param wallTex
  */
case class ElevatorPassageConfig(  // TODO change 'Config' to something else
  wallTex: WallPrefab,
  floorTex: HorizontalBrush,
  ceilTex: Texture,
  elevatorFloor: HorizontalBrush,
  elevatorCeil: HorizontalBrush,

  /** the texture on the walls of the moving part of the elevator */
  elevatorSide: WallPrefab,

  /** the texture you face as you enter the elevator (on the opposite wall of the elevator side tex) */
  rails: WallPrefab,

  /** (start sound, stop sound) */
  sounds: (Int, Int),

  ceilingMoves: Boolean

) {

  // just hardcoding for now
  val warningFloor = HorizontalBrush(355)

  def sfxSprite(p: PointXY, z: Int, sectorId: Int): Sprite = {
    val sfx = new Sprite(p, z, sectorId)
    sfx.setTexture(5) // TODO 5 is hardcoded picnum for MUSICANDSFX sprite
    sfx.setHiTag(sounds._2)
    sfx.setLotag(sounds._1)
    sfx
  }
}

object ElevatorPassageConfig {

  /** from E2L6 */
  val DefaultSounds = (71, 73)

  /** for unit tests */
  def testDefaults(): ElevatorPassageConfig = {
    ElevatorPassageConfig(WallPrefab(Texture(0, 128)), HorizontalBrush(), Texture(0, 128), HorizontalBrush(), HorizontalBrush(), WallPrefab(Texture(0, 128)), WallPrefab(Texture(0, 128)), DefaultSounds, true)
  }
}



/**
  * Draws a passage from wall A to wall B, creating stairs (ramps?) or a lift to nicely adjust the z height.
  *
  * TODO: rename this to "strait" or something
  *
  * Previous I was going to call this "DeltaZPassagePrinter"
  */
object SpacePassagePrinter {

  /** minimum height difference needed to avoid a stupid looking elevator (it does function at smaller heights) */
  val MinElevatorDrop = 10240


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
  ): (ResultAnchor, ResultAnchor) = {

    if (wallA.width < BuildConstants.MinHallwayWidth || wallB.width < BuildConstants.MinHallwayWidth){
      throw new Exception("entrace width too narrow")
    }
    require(wallA.axisAligned && wallB.axisAligned, "wall anchors must be axis-aligned")

    // TODO the elevator needs a minimum vertical drop of 10240 in order to not be stupid

    // useful for testing:
    // val niceElevatorDrop = MinElevatorDrop + (1024 * 2)
    if(Math.abs(wallA.floorZ - wallB.floorZ) >= MinElevatorDrop){
      val opts = ElevatorPassageConfig(
        WallPrefab(gameCfg.tex(258)).copy(shade=Some(16)),
        HorizontalBrush(gameCfg.tex(183)),
        gameCfg.tex(181),
        HorizontalBrush(380).withShade(6).withRelative(true),
        HorizontalBrush(126).withRelative(true),
        WallPrefab(gameCfg.tex(354)).copy(xTileRepeat = Some(1), xrepeat = None, yrepeat=Some(16)),
        WallPrefab(gameCfg.tex(353)),
        ElevatorPassageConfig.DefaultSounds,
        ceilingMoves = false
      )
      printSpacePassageElevator(gameCfg, map, wallA, wallB, opts)

    }else{

      val stairTex = StairTex(
        WallPrefab(gameCfg.tex(258)),
        riser=WallPrefab(gameCfg.tex(349)),
        tread=HorizontalBrush(183),
        ceil=HorizontalBrush(181)
      )
      StairPrinter.printStraight(map, wallA, wallB, stairTex)

    }

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

    val liftSize = if(wallA.width < 512) {
      512
    }else{ // if(wallA.width < 3072) {
      1024
    }
    // This was a nice idea, but I dont like how it increases the min length
    // }else{
    //   2048
    // }
    require(wallA.p0.distanceTo(wallB.p1) > liftSize, "passage must be longer than lift size")


    // TODO enforce a min hallway LENGTH based on the required elevator size

    val (c2, c3, c4, c5, lengthA, lengthB) = elevatorControlPoints(wallA, wallB, liftSize)
    val wp = config.wallTex
    val ep = config.elevatorSide
    val travelDown = wallA.floorZ < wallB.floorZ // < means wallA is HIGHER
    val liftTag = gameConfig.ST.elevatorFor(travelDown, config.ceilingMoves)
    // val liftTag = if(wallA.floorZ < wallB.floorZ){
    //   // means wallA is HIGHER
    //   gameConfig.ST.elevatorDown
    // }else {
    //   gameConfig.ST.elevatorUp
    // }
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
      val left: Seq[Wall] = Seq((wallA.p0, wp), (wallA.p1, wp), (c2, ep), (c5, wp)).map{case (p, tex) => MiscPrinter.wall(p, tex)}
      val leftId = MiscPrinter.createSector(map, left, wallA.floorZ, wallA.ceilZ)
      val leftSector = map.getSector(leftId)
      config.floorTex.writeToFloor(leftSector)
      leftSector.setCeilingTexture(config.ceilTex.picnum)

      val eSideWidth = (liftSize - width)/2
      val t0 = c2.add(toI(wallA.vector.toF.normalized.multipliedBy(eSideWidth)))
      val t1 = c3.add(toI(wallA.vector.toF.normalized.multipliedBy(eSideWidth)))
      val b0 = c4.add(toI(wallB.vector.toF.normalized.multipliedBy(eSideWidth)))
      val b1 = c5.add(toI(wallB.vector.toF.normalized.multipliedBy(eSideWidth)))

      // val lift = Seq((c2, wp), (t0, wp), (t1, wp), (c3, config.rails), (c4, wp), (b0, wp), (b1, wp), (c5, config.rails)).map{ case (p, prefab) => MiscPrinter.wall(p, prefab)}
      // need to do this in a different order b/c of first wall problems
      val littleWall = wp.copy(xrepeat=Some(4)) // TODO calculate correct value based on xrepeat of the longer walls
      val lift = Seq((t0, wp), (t1, littleWall), (c3, config.rails), (c4, littleWall), (b0, wp), (b1, littleWall), (c5, config.rails), (c2, littleWall)).map{ case (p, prefab) => MiscPrinter.wall(p, prefab)}
      val liftId = MiscPrinter.createSector(map, lift, wallA.floorZ, wallA.ceilZ)
      MiscPrinter.autoLinkRedWalls(map, leftId, liftId)
      val liftSector = map.getSector(liftId)
      config.elevatorFloor.writeToFloor(liftSector) // TODO maybe could put this in a makeLiftSector(map, liftId, config)
      config.elevatorCeil.writeToCeil(liftSector)
      map.getSector(liftId).setLotag(liftTag)

      val elevatorCenter = new LineSegmentXY(c2, c4).midpoint()
      map.addSprite(config.sfxSprite(elevatorCenter, liftSector.getFloorZ, liftId))

      val right = Seq((wallB.p0, wp), (wallB.p1, wp), (c4, ep), (c3, wp)).map{case (p, prefab) => MiscPrinter.wall(p, prefab)}
      val rightId = MiscPrinter.createSector(map, right, wallB.floorZ, wallB.ceilZ)
      val rightSector = map.getSector(rightId)
      config.floorTex.writeToFloor(rightSector)
      rightSector.setCeilingTexture(config.ceilTex.picnum)
      MiscPrinter.autoLinkRedWalls(map, liftId, rightId)

      (render.ResultAnchor(CompassWriter.westWalls(map, leftId).head, leftId), render.ResultAnchor(CompassWriter.eastWalls(map, rightId).head, rightId))
    }else if(width == liftSize){
      /*
       *     1-------C2------C3-------0
       *     /\      |       |        |
       *   A |       |  E    |        | B
       *     |       |       |       \/
       *     0-------C5------C4------1
       */
      val left: Seq[Wall] = Seq((wallA.p0, wp), (wallA.p1, wp), (c2, ep), (c5, wp)).map{case (p, tex) => MiscPrinter.wall(p, tex)}
      val leftId = MiscPrinter.createSector(map, left, wallA.floorZ, wallA.ceilZ)
      val leftSector = map.getSector(leftId)
      config.floorTex.writeToFloor(leftSector)
      leftSector.setCeilingTexture(config.ceilTex.picnum)

      val lift = Seq((c2, wp), (c3, config.rails), (c4, wp), (c5, config.rails)).map{ case (p, prefab) => MiscPrinter.wall(p, prefab)}
      val liftId = MiscPrinter.createSector(map, lift, wallA.floorZ, wallA.ceilZ)
      MiscPrinter.autoLinkRedWalls(map, leftId, liftId)
      val liftSector = map.getSector(liftId)
      config.elevatorFloor.writeToFloor(liftSector)
      config.elevatorCeil.writeToCeil(liftSector)
      map.getSector(liftId).setLotag(liftTag)

      val elevatorCenter = new LineSegmentXY(c2, c4).midpoint()
      map.addSprite(config.sfxSprite(elevatorCenter, liftSector.getFloorZ, liftId))

      val right = Seq((wallB.p0, wp), (wallB.p1, wp), (c4, ep), (c3, wp)).map{case (p, prefab) => MiscPrinter.wall(p, prefab)}
      val rightId = MiscPrinter.createSector(map, right, wallB.floorZ, wallB.ceilZ)
      val rightSector = map.getSector(rightId)
      config.floorTex.writeToFloor(rightSector)
      rightSector.setCeilingTexture(config.ceilTex.picnum)
      MiscPrinter.autoLinkRedWalls(map, liftId, rightId)

      (render.ResultAnchor(CompassWriter.westWalls(map, leftId).head, leftId), render.ResultAnchor(CompassWriter.eastWalls(map, rightId).head, rightId))

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

      val eSideWidth = (width - liftSize)/2
      require(eSideWidth > 0)
      val bn = wallB.vector.toF.normalized
      val i0 = c2.add(toI(bn.multipliedBy(eSideWidth)))
      val i1 = c3.add(toI(bn.multipliedBy(eSideWidth)))
      val i3 = i0.add(toI(bn.multipliedBy(liftSize)))
      val i2 = i1.add(toI(bn.multipliedBy(liftSize)))

      // left
      val left: Seq[Wall] = Seq((wallA.p0, wp), (wallA.p1, wp), (c2, wp), (i0, ep), (i3, wp), (c5, wp)).map{case (p, tex) => MiscPrinter.wall(p, tex)}
      val leftId = MiscPrinter.createSector(map, left, wallA.floorZ, wallA.ceilZ)
      val leftSector = map.getSector(leftId)
      config.floorTex.writeToFloor(leftSector)
      leftSector.setCeilingTexture(config.ceilTex.picnum)

      // upper
      val upper = Seq((c2, wp), (c3, wp), (i1, ep), (i0, wp)).map{case (p, tex) => MiscPrinter.wall(p, tex)}
      val upperId = MiscPrinter.createSector(map, upper, wallA.floorZ, wallA.ceilZ)
      val upperSector = map.getSector(upperId)
      config.warningFloor.writeToFloor(upperSector)
      upperSector.setCeilingTexture(config.ceilTex.picnum)
      MiscPrinter.autoLinkRedWalls(map, leftId, upperId)

      // lower
      val lower = Seq((c5, wp), (i3, ep), (i2, wp), (c4, wp)).map{ case (p, tex) => MiscPrinter.wall(p, tex)}
      val lowerId = MiscPrinter.createSector(map, lower, wallA.floorZ, wallA.ceilZ)
      val lowerSector = map.getSector(lowerId)
      config.warningFloor.writeToFloor(lowerSector)
      lowerSector.setCeilingTexture(config.ceilTex.picnum)
      MiscPrinter.autoLinkRedWalls(map, leftId, lowerId)
      MiscPrinter.autoLinkRedWalls(map, upperId, lowerId)

      // elevator
      val lift = Seq((i0, wp), (i1, config.rails), (i2, wp), (i3, config.rails)).map{ case (p, prefab) => MiscPrinter.wall(p, prefab)}
      val liftId = MiscPrinter.createSector(map, lift, wallA.floorZ, wallA.ceilZ)
      MiscPrinter.autoLinkRedWalls(map, leftId, liftId)
      MiscPrinter.autoLinkRedWalls(map, upperId, liftId)
      MiscPrinter.autoLinkRedWalls(map, lowerId, liftId)
      val liftSector = map.getSector(liftId)
      config.elevatorFloor.writeToFloor(liftSector)
      config.elevatorCeil.writeToCeil(liftSector)
      map.getSector(liftId).setLotag(liftTag)

      val elevatorCenter = new LineSegmentXY(c2, c4).midpoint()
      map.addSprite(config.sfxSprite(elevatorCenter, liftSector.getFloorZ, liftId))

      // right
      val right = Seq((wallB.p0, wp), (wallB.p1, wp), (c4, wp), (i2, ep), (i1, wp), (c3, wp)).map{case (p, prefab) => MiscPrinter.wall(p, prefab)}
      val rightId = MiscPrinter.createSector(map, right, wallB.floorZ, wallB.ceilZ)
      val rightSector = map.getSector(rightId)
      config.floorTex.writeToFloor(rightSector)
      rightSector.setCeilingTexture(config.ceilTex.picnum)
      MiscPrinter.autoLinkRedWalls(map, liftId, rightId)
      MiscPrinter.autoLinkRedWalls(map, upperId, rightId)
      MiscPrinter.autoLinkRedWalls(map, lowerId, rightId)

      (render.ResultAnchor(CompassWriter.westWalls(map, leftId).head, leftId), render.ResultAnchor(CompassWriter.eastWalls(map, rightId).head, rightId))
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


    val lowerFloor = 73728
    val ridiculousLowerFloor = BuildConstants.DefaultFloorZ + (1024 * 11)
    // hallway matches elevator width
    // val leftSectorId = MiscPrinter.box(writer.getMap, new PointXY(0, 0), new PointXY(2048, 1024), BuildConstants.DefaultFloorZ, BuildConstants.DefaultCeilZ);
    // val rightSectorId = MiscPrinter.box(writer.getMap, new PointXY(6144, 0), new PointXY(8192, 1024), lowerFloor, BuildConstants.DefaultCeilZ);

    // hallway larger than elevator width
    // val changeMe = 2048
    // val leftSectorId = MiscPrinter.box(writer.getMap, new PointXY(0, 0), new PointXY(2048, changeMe), BuildConstants.DefaultFloorZ, BuildConstants.DefaultCeilZ);
    // val rightSectorId = MiscPrinter.box(writer.getMap, new PointXY(6144, 0), new PointXY(8192 - 1048, changeMe), ridiculousLowerFloor, BuildConstants.DefaultCeilZ);


    // testing stairs
    val changeMe = 2048
    val rightSideStart = 2048 + 256
    val verticalDrop = 1024 * 3
    val leftSectorId = MiscPrinter.box(writer.getMap, new PointXY(0, 0), new PointXY(2048, changeMe), BuildConstants.DefaultFloorZ, BuildConstants.DefaultCeilZ);
    val rightSectorId = MiscPrinter.box(writer.getMap, new PointXY(rightSideStart, 0), new PointXY(rightSideStart + 2048, changeMe), BuildConstants.DefaultFloorZ + verticalDrop, BuildConstants.DefaultCeilZ);

    val wallA = WallAnchor.fromExistingWall(CompassWriter.eastWalls(writer.getMap, leftSectorId).head)
    val wallB = WallAnchor.fromExistingWall2(CompassWriter.westWalls(writer.getMap, rightSectorId).head, writer.getMap.getSector(rightSectorId))

    // val ep = WallPrefab(config.elevatorSideTex).copy(xTileRepeat = Some(1), xrepeat = None, yrepeat=Some(16))
    val textures = ElevatorPassageConfig(
      WallPrefab(gameCfg.tex(258)).copy(shade=Some(16)),
      HorizontalBrush(gameCfg.tex(183)),
      gameCfg.tex(181),
      HorizontalBrush(380).withShade(6).withRelative(true),
      HorizontalBrush(126).withRelative(true),
      WallPrefab(gameCfg.tex(354)).copy(xTileRepeat = Some(1), xrepeat = None, yrepeat=Some(16)),
      WallPrefab(gameCfg.tex(353)),
      ElevatorPassageConfig.DefaultSounds,
      ceilingMoves = false
    )
    // val (resultLeft, resultRight) = SpacePassagePrinter.printSpacePassageElevator(gameCfg, writer.outMap, wallA, wallB, textures)
    val (resultLeft, resultRight) = SpacePassagePrinter.printSpacePassage(gameCfg, writer.outMap, wallA, wallB)

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
