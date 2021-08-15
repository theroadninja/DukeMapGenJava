package trn.bespoke.moonbase2

import trn.{AngleUtil, BuildConstants, PointXY, RandomX, Sprite, Wall, Map => DMap}
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

  /** return  (list of new sector ids, walls for main loop, "new p0" point) */
  def chairs(gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, floorZ: Int, ceilZ: Int, loungeWall: WallPrefab, loungeCeil: HorizontalBrush, chairCount: Int): (Seq[Int], Seq[Wall], PointXY) = {

    val SeatBackHeight = 7 * BuildConstants.ZStepHeight
    val SeatHeight = 4 * BuildConstants.ZStepHeight
    val ChairBackSide = WallPrefab(gameCfg.tex(786)).copy(xrepeat=Some(4), yrepeat=Some(8)).withShade(15)
    val ChairSeatSide = WallPrefab(gameCfg.tex(788)).copy(xrepeat=Some(4), yrepeat=Some(16)).withShade(15) // TODO need to adjust xrepeat based on wall length
    val ChairSeatFloor = HorizontalBrush(gameCfg.tex(786)).withRelative(true).withSmaller(true)
    val chairFrontXRepeat = ChairSeatSide.tex.get.xRepeatForNRepetitions(chairCount)     //c2.distanceTo(c3).toInt

    val _ :: c1 :: c2 :: c3 :: c4 :: c5 :: Nil = LoungeWallPrinter.chairControlPoints(p0, p1, chairCount)
    val seatBackTex = WallPrefab(gameCfg.tex(786)).withShade(15).copy(xrepeat = Some(24), yrepeat = Some(8))

    // TODO in addition to xRepeatForScale, also need ot calculate an offset.  Maybe its best to use do a mod on its global position (set the offset so the texture always begins at x=0)
    val seatBackId = MiscPrinter.createSector(map, Seq(wall(p0, loungeWall.withXRepeatForScale(1.0, p0.distanceTo(c5).toInt)), wall(c5, WallPrefab.Empty), wall(c4, WallPrefab.Empty), wall(c1, WallPrefab.Empty)), floorZ - SeatBackHeight, ceilZ)
    ChairSeatFloor.writeToFloor(map.getSector(seatBackId))
    loungeCeil.writeToCeil(map.getSector(seatBackId))
    val seatId = MiscPrinter.createSector(map, Seq(c1, c4, c3, c2).map(MiscPrinter.wall(_, seatBackTex.copy(xrepeat=Some(chairFrontXRepeat)))), floorZ - SeatHeight, ceilZ)
    ChairSeatFloor.writeToFloor(map.getSector(seatId))
    loungeCeil.writeToCeil(map.getSector(seatId))
    MiscPrinter.autoLinkRedWalls(map, seatBackId, seatId)

    val outerWalls = Seq(
      wall(p0, ChairBackSide), wall(c1, ChairSeatSide), wall(c2, ChairSeatSide.copy(xrepeat=Some(chairFrontXRepeat))), wall(c3, ChairSeatSide), wall(c4, ChairBackSide)
    )
    (Seq(seatBackId, seatId), outerWalls, c5)
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

  /**
    * Total Width:  512
    *
    * p0 --> c1 -------- c4 -- c5 ...> p1
    *         |          |
    *        c2 -------- c3
    */
  def tableCtrlPoints(p0: PointXY, p1: PointXY): Seq[PointXY] = {
    val across = p0.vectorTo(p1).toF.normalized
    val down = across.rotatedCW()
    val c1 = p0 + across * 64
    val c4 = c1 + across * 384
    val c5 = c4 + across * 64
    val c2 = c1 + down * 448
    val c3 = c4 + down * 448
    Seq(c1, c2, c3, c4, c5)
  }

  def table(r: RandomX, gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, floorZ: Int, ceilZ: Int, loungeWall: WallPrefab, loungeCeil: HorizontalBrush): (Seq[Int], Seq[Wall], PointXY) = {
    val c1 :: c2 :: c3 :: c4 :: c5 :: Nil = tableCtrlPoints(p0, p1)
    val TableFloor = HorizontalBrush(3387).withShade(15).withSmaller().withRelative()
    val tableZ = floorZ - BuildConstants.ZStepHeight * 4
    val tableWalls = Seq(wall(c1, loungeWall), wall(c4, E), wall(c3, E), wall(c2, E))
    val tableId = createAndPaintSector(map, tableWalls, tableZ, ceilZ, TableFloor, loungeCeil)

    val across = p0.vectorTo(p1).toF.normalized
    val plantP = c1 + across * 192 + across.rotatedCW() * 128
    val plant = new Sprite(plantP.withZ(tableZ), tableId, 1025, 0, 0)
    plant.setShade(10)
    plant.setRepeats(24, 24)
    map.addSprite(plant)

    r.flipCoin{
      val glassP = c1 + across * 320 + across.rotatedCW() * 384
      val glass = new Sprite(glassP.withZ(tableZ), tableId, 957, 0, 0)
      glass.setRepeats(24, 24)
      glass.setShade(7)
      map.addSprite(glass)
      ()
    }{}

    val TableWall = WallPrefab(gameCfg.tex(3387)).withRepeats(12, 16).withShade(15)
    val outerWalls = Seq(wall(p0, loungeWall), wall(c1, TableWall), wall(c2, TableWall), wall(c3, TableWall), wall(c4, loungeWall))
    (Seq(tableId), outerWalls, c5)
  }

  /**
    * p0 --> c1           c4 --> c5 ..> p1
    *         \          /
    *          \        /
    *           c2 --- c3
    *
    * @param p0
    * @param p1
    * @return
    */
  def bulkheadCtlPoints(p0: PointXY, p1: PointXY, length: Int): Seq[PointXY] = {
    require(length >= 2048)
    val middleLength = length - 256 * 2 - 448 * 2
    val across = p0.vectorTo(p1).toF.normalized
    val down = across.rotatedCW()
    val c1 = p0 + across * 256
    val c2 = c1 + across * 448 + down * 448
    val c3 = c2 + across * middleLength
    val c4 = c1 + across * (middleLength + 448 * 2)
    val c5 = c4 + across * 256
    Seq(c1, c2, c3, c4, c5)
  }

  def bulkhead(gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, loungeWall: WallPrefab, length: Int): (Seq[Int], Seq[Wall], PointXY) = {

    val Bulkhead = WallPrefab(gameCfg.tex(367)).withRepeats(4, 8).withShade(15)
    val c1 :: c2 :: c3 :: c4 :: c5 :: Nil = bulkheadCtlPoints(p0, p1, length)

    val middleWallLength = c2.manhattanDistanceTo(c3).toInt
    val outerWalls = Seq(wall(p0, loungeWall), wall(c1, Bulkhead), wall(c2, Bulkhead.withXRepeatForScale(1, middleWallLength)), wall(c3, Bulkhead), wall(c4, loungeWall))
    (Seq.empty, outerWalls, c5)
  }

  def window(gameCfg: GameConfig, map: DMap, p0: PointXY, p1: PointXY, floorZ: Int): (Seq[Int], Seq[Wall], PointXY) = {
    ???
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
