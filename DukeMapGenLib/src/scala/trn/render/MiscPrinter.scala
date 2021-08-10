package trn.render

import trn.{BuildConstants, Main, MapLoader, PlayerStart, PointXY, Wall, Map => DMap}
import scala.collection.JavaConverters._

object MiscPrinter {

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
