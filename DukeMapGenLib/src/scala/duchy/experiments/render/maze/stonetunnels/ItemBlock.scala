package duchy.experiments.render.maze.stonetunnels

import org.apache.commons.lang3.ArrayUtils
import trn.duke.experiments.SpritePrefab
import org.apache.commons.lang3.tuple.Pair
import trn.{PointXY, MapUtil, Sector, Wall, Sprite}
import trn.duke.experiments.gridblock.SimpleBlock
import trn.duke.experiments.WallPrefab

import java.util

class ItemBlock(gridCoordinate: Pair[Integer, Integer], itemPrefab: SpritePrefab) extends SimpleBlock(gridCoordinate) {

  override def draw(map: trn.Map): Int = {
    val south: Int = getSouthEdge
    val west: Int = getWestEdge
    val north: Int = getNorthEdge
    val east: Int = getEastEdge
    /** width between dais and wall */
    val edgeWidth: Int = 256
    val inner_south: Int = south - edgeWidth
    val inner_north: Int = north + edgeWidth
    val inner_west: Int = west + edgeWidth
    val inner_east: Int = east - edgeWidth
    val daisZ: Int = StoneConstants.UPPER_FLOORZ - (64 << 4)
    val outer_box: Array[PointXY] = Array[PointXY](new PointXY(west, south), new PointXY(west, north), new PointXY(east, north), new PointXY(east, south))
    val inner_box_cw: Array[PointXY] = Array[PointXY](new PointXY(inner_west, inner_south), new PointXY(inner_west, inner_north), new PointXY(inner_east, inner_north), new PointXY(inner_east, inner_south))
    var inner_box_ccw: Array[PointXY] = util.Arrays.copyOf(inner_box_cw, inner_box_cw.length)
    inner_box_ccw = inner_box_ccw.reverse
    val daisWall: WallPrefab = new WallPrefab(StoneConstants.UPPER_WALL)
    daisWall.setTexture(StoneConstants.UPPER_CEILING)
    //outer sector
    val outerSectorIndex: Int = map.createSectorFromMultipleLoops(Wall.createLoop(inner_box_ccw, daisWall), //this needs to be first b/c of linkAllWalls
      Wall.createLoop(outer_box, StoneConstants.UPPER_WALL))
    StoneConstants.UPPER_SECTOR.writeTo(map.getSector(outerSectorIndex))
    val innerSectorIndex: Int = map.createSectorFromLoop2(Wall.createLoopAsList(inner_box_cw, StoneConstants.UPPER_WALL))
    val innerSector: Sector = map.getSector(innerSectorIndex)
    StoneConstants.UPPER_SECTOR.writeTo(innerSector)
    innerSector.setFloorTexture(StoneConstants.UPPER_CEILING)
    innerSector.setFloorZ(daisZ)
    MapUtil.linkAllWalls(map, outerSectorIndex, map.getSector(outerSectorIndex).getFirstWall, innerSectorIndex, innerSector.getFirstWall)
    if (this.itemPrefab != null) {
      val item: Sprite = new Sprite(getCenter, daisZ, innerSectorIndex)
      itemPrefab.writeTo(item)
      map.addSprite(item)
    }
    for (c <- connectors) {
      c.setSectorIndex(outerSectorIndex)
    }
    return -(1)
  }
}
