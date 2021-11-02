package trn.render

import trn.prefab.GameConfig
import trn.{AngleUtil, BuildConstants, Main, MapLoader, PlayerStart, PointXY, PointXYZ, Sprite, Wall, Map => DMap}

import scala.collection.JavaConverters._

/**
  * Collection of methods for rendering/printing sectors directly into maps.
  */
object MiscPrinter {

  /**
    * Given a sequence of points that form a wall loop, return a sequence of tuples where the second point is the "next"
    * point in the wall loop.  It wraps around, so the last element in the seq will have the point at index 0 for its second member.
    *
    * This is for convenience; e.g. it makes it easier for low level functions to know how long a wall is
    *
    * So in general,  returnSeq(i)._2 == returnSeq(i+1)._1
    *
    * @param wallLoop
    * @return  Seq(point, nextPointInLoop)
    */
  def withP2(wallLoop: Seq[PointXY]): Seq[(PointXY, PointXY)] = {
    val nextPoints: Seq[PointXY] = wallLoop.drop(1) ++ wallLoop.take(1)
    wallLoop.zip(nextPoints)
  }

  def autoLinkRedWalls(map: DMap, sectorId0: Int, sectorId1: Int): Unit ={
    map.getAllWallLoopsAsViews(sectorId0).asScala.flatMap(_.asScala).foreach { w0 =>
      map.getAllWallLoopsAsViews(sectorId1).asScala.flatMap(_.asScala).foreach { w1 =>
        if(w0.getLineSegment == w1.getLineSegment.reversed() && !(w0.isRedwall || w1.isRedwall)){
          if(w0.isRedwall || w1.isRedwall){
            require(w0.otherWallId == w1.getWallId && w1.otherWallId == w0.getWallId)
          }else{
            map.linkRedWallsStrict(sectorId0, w0.getWallId, sectorId1, w1.getWallId)
          }
        }else{
          //println(s"${w0.getLineSegment()} vs ${w1.getLineSegment}")
        }
      }
    }
  }

  def autoLinkRedWalls(map: DMap, sectors: Seq[Int]): Unit = sectors.foreach { s0 =>
    sectors.foreach { s1 =>
      autoLinkRedWalls(map, s0, s1)
    }
  }

  /** runs a N^2 algo to link all redwalls that it can.  for testing */
  def lazyAutoLinkEverything(map: DMap): Unit = {
    for(id0 <- 0 until map.getSectorCount){
      for(id1 <- 0 until map.getSectorCount){
        if(id0 != id1){
          autoLinkRedWalls(map, id0, id1)
        }
      }
    }
  }

  def wall(p: PointXY, tex: Texture): Wall = {
    val wall = new Wall(p.x, p.y)
    wall.setXRepeat(8)
    wall.setYRepeat(8)
    wall.setTexture(tex.picnum)
    wall
  }

  def wall(p: PointXY, wallPrefab: WallPrefab): Wall = wallPrefab.create(p)

  def sprite(
    p: PointXYZ,
    sectorId: Int,
    picnum: Int,
    shade: Int = 0,
    pal: Int = 0,
    xrepeat: Int = 64, // 64 seems to be the default
    yrepeat: Int = 64,
    hitag: Int = 0,
    lotag: Int = 0,
    angle: Int = AngleUtil.ANGLE_UP,
    cstat: Int = 0,
  ): Sprite = {
    val s = new Sprite(p, sectorId, picnum, hitag, lotag)
    s.setShade(shade)
    s.setPal(pal)
    s.setRepeats(xrepeat, yrepeat)
    s.setAng(angle)
    s.setCstat(cstat.toShort)
    s
  }

  /**
    * THIS IS THE ONE FOR A VISIBLE FORCE FIELD, not the invisible kind
    *
    *
    *
    * @return a wall prefab to paint a force field wall - TODO it doesnt handle scaling for you
    */
  def forceField(gameCfg: GameConfig): WallPrefab = WallPrefab(gameCfg.visibleForceField)
    .withOverpic(gameCfg.visibleForceField).withBlockable().withMask().withHitscan().copy(yrepeat = Some(16))   // 87 = 64(Hitscan) + 16(mask) + 4 + 2 + 1(blockable)

  /**
    * For force fields that are invisible until hit.
    *
    * @param gameCfg
    * @return
    */
  def invisibleForceField(gameCfg: GameConfig, wallLength: Int): WallPrefab = WallPrefab(gameCfg.invisibleForceField)
    .withOverpic(gameCfg.invisibleForceField).withHitscan().copy(yrepeat = Some(8)).withXRepeatForScale(1.0, wallLength)

  // From working on StairPrinter:
  // // This code makes the north wall a curved dome
  // val nw = new PointXY(0, 0)
  // val ne = new PointXY(2048, 0)
  // val sw = new PointXY(0, 1024)
  // val se = new PointXY(2048, 1024)
  // // i want the curve to be a half circle if my "control arms" are facing straight out
  // // multiplying the "arm" length by 2/3 seems to do the trick
  // val handleWidth = 2048 * 2 / 3
  // val handle = new PointXY(0, -handleWidth)
  // val topRow = Interpolate.cubic(nw, nw.add(handle), ne.add(handle), ne, 8)
  // val loop = topRow.map(p => w(p)) ++ Seq(w(se), w(sw))
  // val sectorId = map.createSectorFromLoop(loop: _*)

  def createSector(map: DMap, walls: Seq[Wall], floorZ: Int, ceilZ: Int): Int = {
    val sectorId = map.createSectorFromLoop(walls: _*)
    val sector = map.getSector(sectorId)
    sector.setFloorZ(floorZ)
    sector.setCeilingZ(ceilZ)
    sectorId
  }

  def createAndPaintSector(map: DMap, walls: Seq[Wall], floorZ: Int, ceilZ: Int, floorBrush: HorizontalBrush, ceilBrush: HorizontalBrush): Int = {
    val sectorId = createSector(map, walls, floorZ, ceilZ)
    val sector = map.getSector(sectorId)
    floorBrush.writeToFloor(sector)
    ceilBrush.writeToCeil(sector)
    sectorId
  }

  def createMultiLoopSector(map: DMap, loops: Seq[Iterable[Wall]], floorZ: Int, ceilZ: Int): Int = {
    val loops2 = loops.map(_.toArray)
    val sectorId = map.createSectorFromMultipleLoops(loops2: _*)
    val sector = map.getSector(sectorId)
    sector.setFloorZ(floorZ)
    sector.setCeilingZ(ceilZ)
    sectorId
  }

  def box(
    map: DMap,
    topLeft: PointXY,  // NW corner
    bottomRight: PointXY, // SE corner
    floorZ: Int,
    ceilZ: Int,
  ): Int = {
    require(ceilZ <= floorZ)
    val WallTex = Texture(0, 64)
    val topRight = new PointXY(bottomRight.x, topLeft.y)
    val bottomLeft = new PointXY(topLeft.x , bottomRight.y)
    val loop = Seq(topLeft, topRight, bottomRight, bottomLeft).map(p => wall(p, WallTex))
    val sectorId = map.createSectorFromLoop(loop: _*)
    val sector = map.getSector(sectorId)
    sector.setFloorZ(floorZ)
    sector.setCeilingZ(ceilZ)
    sectorId
  }
}
