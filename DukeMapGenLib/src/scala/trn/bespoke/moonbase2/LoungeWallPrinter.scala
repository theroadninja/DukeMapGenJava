package trn.bespoke.moonbase2

import trn.{AngleUtil, BuildConstants, PointXY, Sprite, Wall, Map => DMap}
import trn.PointImplicits._
import trn.prefab.GameConfig
import trn.render.MiscPrinter._
import trn.render.{HorizontalBrush, MiscPrinter, WallPrefab}

object LoungeWallPrinter {
  val E = WallPrefab.Empty

  def emptyWall(p0: PointXY, p1: PointXY, length: Int, loungeWall: WallPrefab): (Seq[Int], Seq[Wall], PointXY)  = {
    val p = (p0 + p0.vectorTo(p1).toF.normalized * length)
    (Seq.empty, Seq(wall(p0, loungeWall)), p)
  }

  /**
    * --->p0     c5--->   p1
    *     |      |
    *     c1     c4
    *     |      |
    *     |      |
    *     c2----c3
    *
    * @param p0
    * @param p1
    * @param chairCount
    * @return
    */
  def chairControlPoints(p0: PointXY, p1: PointXY, chairCount: Int): Seq[PointXY] = {
    require(axisAligned(p0, p1))
    require(chairCount >= 2)
    val ChairLength = 256 // width of single chair
    val across = p0.vectorTo(p1).toF.normalized
    val down = across.rotatedCW

    val c1 = p0.add(toI(down.multipliedBy(128)))
    val c2 = c1.add(toI(down.multipliedBy(256)))

    val c5 = p0.add(toI(across.multipliedBy(ChairLength * chairCount)))
    val c4 = c5.add(toI(down.multipliedBy(128)))
    val c3 = c4.add(toI(down.multipliedBy(256)))

    Seq(p0, c1, c2, c3, c4, c5)
  }


  /**
    *
    *        c4 ----- c5
    *         |        |
    *         |        |
    *        c3 ----- c6
    *         |        |
    *        c2 ----- c7
    *         |        |
    * p0 --> c1 ----- c8 --- c9 ..> p1
    *
    * @param p0 starting control point (wall is p0 -> p1)
    * @param p1 end control point
    * @param edgeDepth distance from outer wall to edge of door
    * @param doorDepth how thick the door is
    * @return control points
    */
  def cabinetCtrlPoints(p0: PointXY, p1: PointXY, edgeDepth: Int, doorDepth: Int, cabinetDepth: Int): Seq[PointXY] = {
    val across = p0.vectorTo(p1).toF.normalized
    val up = across.rotatedCCW()
    val c1 = p0 + across * 128
    val c8 = c1 + across * 512
    val c9 = c8 + across * 128

    val c2 = c1 + up * edgeDepth
    val c3 = c2 + up * doorDepth
    val c4 = c3 + up * cabinetDepth

    val c7 = c8 + up * edgeDepth
    val c6 = c7 + up * doorDepth
    val c5 = c6 + up * cabinetDepth
    Seq(c1, c2, c3, c4, c5, c6, c7, c8, c9)

  }

  /**
    */
  def medCabinetControlPoints(p0: PointXY, p1: PointXY): Seq[PointXY] = {
    cabinetCtrlPoints(p0, p1, 32, 32, 192)
  }

  /** one of these in E2L2 */
  def medCabinet(gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, floorZ: Int, loungeWall: WallPrefab): (Seq[Int], Seq[Wall], PointXY) = {
    val cabinetFloor = floorZ - (BuildConstants.ZStepHeight * 6)
    val cabinetTop = cabinetFloor - (BuildConstants.ZStepHeight * 6)

    val FirstAid = WallPrefab(gameCfg.tex(463)).withXRepeatForNRepetitions(1).withShade(2).copy(yrepeat=Some(22))
    val Light = WallPrefab(gameCfg.tex(128)).copy(xrepeat = Some(1)).withShade(2).withPal(2)
    val EdgeFloor = HorizontalBrush(128).withSmaller(true).withRelative(true).withShade(2).withPal(2)
    val DoorWall = WallPrefab(gameCfg.tex(355)).copy(xrepeat = Some(1)).withShade(15).withAlignBottom(true)
    val DoorFloor = HorizontalBrush(355).withSmaller(true).withRelative(true)
    val CabinetWall = WallPrefab(gameCfg.tex(349)).copy(xrepeat=Some(2)).withShade(15)
    val CabinetFloor = HorizontalBrush(349).withRelative(true).withShade(15)

    val c1 :: c2 :: c3 :: c4 :: c5 :: c6 :: c7 :: c8 :: c9 :: Nil = medCabinetControlPoints(p0, p1)

    val edge = Seq(wall(c2, FirstAid), wall(c7, Light), wall(c8, WallPrefab.Empty), wall(c1, Light))
    val edgeId = createAndPaintSector(map, edge, cabinetFloor, cabinetTop, EdgeFloor, EdgeFloor)

    val door = Seq(wall(c3, E), wall(c6, DoorWall), wall(c7, E), wall(c2, DoorWall))
    val doorId = createAndPaintSector(map, door, cabinetFloor, cabinetFloor, DoorFloor, DoorFloor)
    map.getSector(doorId).setLotag(gameCfg.ST.ceilingDoor)

    map.addSprite(new Sprite(((c3 + c7)/2).withZ(cabinetFloor), doorId, 5, 0, 167)) // SFX
    map.addSprite(new Sprite(((c3 + c7)/2).withZ(cabinetFloor), doorId, 1, 64, 10)) // door close delay
    map.addSprite(new Sprite(((c3 + c7)/2).withZ(cabinetFloor), doorId, 10, 0, 96)) // GPSPEED; lower is slower (64 perfectly matches the sound but is too slow

    val cabinet = Seq(wall(c4, WallPrefab(gameCfg.tex(462)).copy(xrepeat=Some(8), yrepeat=Some(22))), wall(c5, CabinetWall), wall(c6, E), wall(c3, CabinetWall))
    val cabinetId = createAndPaintSector(map, cabinet, cabinetFloor, cabinetTop, CabinetFloor, CabinetFloor)

    val medkit = new Sprite((c4 + c6) / 2, cabinetFloor, cabinetId)
    medkit.setTexture(53)
    map.addSprite(medkit)

    // SFX Sprite:  0, 167

    val outerWalls = Seq(wall(p0, loungeWall), wall(c1, loungeWall.withAlignBottom(true)), wall(c8, loungeWall))
    (Seq(edgeId, doorId, cabinetId), outerWalls, c9)
  }

  // E2L7 and E2L6(security area)
  def powerCabinet(gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, itemPicNum: Int, floorZ: Int, loungeWall: WallPrefab): (Seq[Int], Seq[Wall], PointXY) = {
    val cabinetFloor = floorZ - (BuildConstants.ZStepHeight * 6)
    val cabinetTop = cabinetFloor - (BuildConstants.ZStepHeight * 6)

    val DoorSide = WallPrefab(gameCfg.tex(353)).withXPan(13).copy(xrepeat = Some(1), yrepeat=Some(16)).withAlignBottom(true) // it's adjusted so that only the part in the middle is showing
    val DoorFloor = HorizontalBrush(355).withSmaller(true).withRelative(true)

    val CabinetWall = WallPrefab(gameCfg.tex(227)).withShade(11).copy(xrepeat=Some(8), yrepeat=Some(16))
    val CabinetFloor = HorizontalBrush(241).withShade(15)

    val c1 :: c2 :: c3 :: c4 :: c5 :: c6 :: c7 :: c8 :: c9 :: Nil = cabinetCtrlPoints(p0, p1, 64, 64, 256)

    val EdgeWall = WallPrefab(gameCfg.tex(797)).withShade(15).copy(xrepeat=Some(1), yrepeat=Some(8))
    val EdgeFloor = HorizontalBrush(726).withRelative(true).withSmaller(true).withShade(22)
    val edge = Seq(wall(c2, WallPrefab(gameCfg.tex(221)).copy(xrepeat=Some(8), yrepeat=Some(22))), wall(c7, EdgeWall), wall(c8, WallPrefab.Empty), wall(c1, EdgeWall))
    val edgeId = createAndPaintSector(map, edge, cabinetFloor, cabinetTop, EdgeFloor, EdgeFloor)

    val door = Seq(wall(c3, E), wall(c6, DoorSide), wall(c7, E), wall(c2, DoorSide))
    val doorId = createAndPaintSector(map, door, cabinetFloor, cabinetFloor, DoorFloor, DoorFloor)
    map.getSector(doorId).setLotag(gameCfg.ST.ceilingDoor)

    map.addSprite(new Sprite(((c3 + c7)/2).withZ(cabinetFloor), doorId, 5, 0, 256)) // SFX
    map.addSprite(new Sprite(((c3 + c7)/2).withZ(cabinetFloor), doorId, 10, 0, 50)) // GPSPEED; lower is slower (64 perfectly matches the sound but is too slow

    val cabinet = Seq(wall(c4, CabinetWall.copy(xrepeat=Some(8), yrepeat=Some(22))), wall(c5, CabinetWall), wall(c6, E), wall(c3, CabinetWall))
    val cabinetId = createAndPaintSector(map, cabinet, cabinetFloor, cabinetTop, CabinetFloor, CabinetFloor)

    map.addSprite(new Sprite(((c4 + c6)/2).withZ(cabinetFloor), cabinetId, itemPicNum, 0, 0))

    val outerWalls: Seq[Wall] = Seq(wall(p0, loungeWall), wall(c1, loungeWall.withAlignBottom(true)), wall(c8, loungeWall))
    (Seq(edgeId, doorId, cabinetId), outerWalls, c9)
  }

  /**
    *        c3-------c4
    *         |        |
    *        c2-------c5
    *         |        |
    * p0 --> c1-------c6 --> c7 ..> p1
    *
    * @param p0
    * @param p1
    * @return
    */
  def securityScreenCtrlPoints(p0: PointXY, p1: PointXY): Seq[PointXY] = {
    val across = p0.vectorTo(p1).toF.normalized
    val up = across.rotatedCCW()
    val c1 = p0 + across * 128
    val c6 = c1 + across * 512
    val c7 = c6 + across * 128
    val c2 = c1 + up * 32
    val c3 = c2 + up * 32
    val c5 = c6 + up * 32
    val c4 = c5 + up * 32
    Seq(c1, c2, c3, c4, c5, c6, c7)
  }

  def securityScreen(gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, floorZ: Int, loungeWall: WallPrefab): (Seq[Int], Seq[Wall], PointXY) = {
    val cabinetFloor = floorZ - (BuildConstants.ZStepHeight * 6)
    val cabinetTop = cabinetFloor - (BuildConstants.ZStepHeight * 8)

    val c1 :: c2 :: c3 :: c4 :: c5 :: c6 :: c7 :: Nil = securityScreenCtrlPoints(p0, p1)

    val EdgeWall = WallPrefab(gameCfg.tex(372)).withShade(5).copy(xrepeat=Some(1), yrepeat=Some(16))
    val EdgeFloor = HorizontalBrush(372).withShade(5)
    // this is a visible force field
    val forceField = WallPrefab(gameCfg.tex(663)).withOverpic(gameCfg.tex(663)).withBlockable().withMask().withHitscan().copy(xrepeat=Some(8), yrepeat=Some(16))   // 87 = 64(Hitscan) + 16(mask) + 4 + 2 + 1(blockable)
    val edge = Seq(wall(c2, forceField), wall(c5, EdgeWall), wall(c6, E), wall(c1, EdgeWall))
    val edgeId = createAndPaintSector(map, edge, cabinetFloor, cabinetTop, EdgeFloor, EdgeFloor)

    val ScreenSide = WallPrefab(gameCfg.tex(198)).withShade(5).withPal(1).copy(xrepeat = Some(1), yrepeat = Some(16))
    val ScreenBack = WallPrefab(gameCfg.tex(198)).withShade(5).withPal(1).copy(xrepeat = Some(16), yrepeat = Some(16))
    val ScreenFloor = HorizontalBrush(198).withShade(5).withPal(1)
    val screen = Seq(wall(c3, ScreenBack), wall(c4, ScreenSide), wall(c5, forceField.withXflip()), wall(c2, ScreenSide))
    val screenId = createAndPaintSector(map, screen, cabinetFloor, cabinetTop, ScreenFloor, ScreenFloor)

    val viewscreen = new Sprite(((c3 + c4)/2).withZ(cabinetFloor), screenId, 499, 0, 0)
    viewscreen.setXRepeat(21)
    viewscreen.setYRepeat(21)
    viewscreen.setCstat((Sprite.CSTAT_FLAGS.PLACED_ON_WALL | Sprite.CSTAT_FLAGS.PLACED_ON_FLOOR).toShort)
    viewscreen.setAngle(AngleUtil.angleOf(p0.vectorTo(p1).toF.rotatedCW().toI))

    map.addSprite(viewscreen)


    val outerWalls: Seq[Wall] = Seq(wall(p0, loungeWall), wall(c1, loungeWall.withAlignBottom()), wall(c6, loungeWall))
    (Seq(edgeId, screenId), outerWalls, c7)
  }

  /**
    *        c2 -------- c3
    *         |          |
    * p0 --> c1 -------- c4 -- c5 ...> p1
    *
    * @param p0
    * @param p1
    * @return
    */
  def waterFountainCtrlPoints(p0: PointXY, p1: PointXY): Seq[PointXY] = {
    val across = p0.vectorTo(p1).toF.normalized
    val up = across.rotatedCCW()
    val c1 = p0 + across * 192
    val c4 = c1 + across * 384
    val c5 = c4 + across * 192
    val c2 = c1 + up * 192
    val c3 = c4 + up * 192
    Seq(c1, c2, c3, c4, c5)
  }

  def waterFountain(gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, floorZ: Int, loungeWall: WallPrefab): (Seq[Int], Seq[Wall], PointXY) = {
    val c1 :: c2 :: c3 :: c4 :: c5 :: Nil = waterFountainCtrlPoints(p0, p1)
    val SideWall = WallPrefab(gameCfg.tex(883)).withShade(11).withRepeats(4, 8)
    val BackWall = SideWall.withRepeats(8, 8)
    val walls = Seq(wall(c2, BackWall), wall(c3, SideWall), wall(c4, E), wall(c1, SideWall))
    val z = floorZ - BuildConstants.ZStepHeight * 6
    val sectorId = createAndPaintSector(
      map,
      walls,
      z,
      z - BuildConstants.ZStepHeight * 6,
      HorizontalBrush(372).withShade(15),
      HorizontalBrush(128).withSmaller()
    )

    val fountain = new Sprite(((c2 + c4)/2).withZ(z), sectorId, 563, 0, 0)
    fountain.setRepeats(40, 40)
    map.addSprite(fountain)
    val decal = new Sprite(((c2 + c3)/2).withZ(z - BuildConstants.ZStepHeight * 3), sectorId, 592, 0, 0)
    decal.setCstat((Sprite.CSTAT_FLAGS.PLACED_ON_WALL).toShort)
    decal.setAngle(AngleUtil.angleOf(p0.vectorTo(p1).toF.rotatedCW().toI))
    decal.setRepeats(24, 24)
    map.addSprite(decal)

    (Seq(sectorId), Seq(wall(p0, loungeWall), wall(c1, loungeWall.withAlignBottom().withBlockable()), wall(c4, loungeWall)), c5)
  }

  def tripBombPlacement(gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, floorZ: Int): (Seq[Int], Seq[Wall], PointXY) = {

    // its a post:
    // 256 wide
    // 32 deep
    // 11 pgup high

    // frontpic: 797
    // side & top: 355
    ???
  }
}
