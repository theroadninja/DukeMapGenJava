package duchy.experiments.render.maze.stonetunnels

import org.apache.commons.lang3.tuple.ImmutablePair
import org.apache.commons.lang3.tuple.Pair
import duchy.experiments.render.Block
import duchy.experiments.render.maze.{LegacyConnector, AbstractBlock, WallPrefab}
import org.apache.commons.lang3.ArrayUtils
import trn.{PointXY, MapUtil, Sector, Wall, PlayerStart}
import trn.maze.Heading

import scala.collection.JavaConverters._


class StartBlock(gridCoordinate: Pair[Integer, Integer]) extends AbstractBlock(gridCoordinate) {

  val connector = LegacyConnector.northEdge(this)
  val connectorEdge = Heading.NORTH;
  val floorZ = StoneConstants.UPPER_FLOORZ;

  override def getConnector(heading: Heading): LegacyConnector = {
    if(heading == connectorEdge){ connector }else{ None.orNull }
  }

  def getPlayerStart: PlayerStart = {
    //return player start in the center, facing the room's exit
    val x = (getWestEdge + getEastEdge) / 2
    val y = (getSouthEdge + getNorthEdge) / 2
    new PlayerStart(x, y, floorZ, connectorEdge.getDukeAngle)
  }

  /**
    *
    * @param map
    * @return the index of the (a?) sector that was created.
    */
  override def draw(map: trn.Map): Int = {
    //this room is a square with a misshapen dodecagon (12-sided circle)
    //in the middle, down which light should shine.
    val south: Int = getSouthEdge
    val west: Int = getWestEdge
    val north: Int = getNorthEdge
    val east: Int = getEastEdge


    //and remember, positive y goes down/south//and remember, positive y goes down/south
    val mediumUnit: Int = getOuterWallLength / 8 //grid size #3, 256 in original drawing
    val smallUnit: Int = getOuterWallLength / 32 //grid size...64 in original drawing

    //outer bounding box for dodecagon//outer bounding box for dodecagon
    val dodec_south: Int = south - mediumUnit
    val dodec_north: Int = north + mediumUnit
    val dodec_east: Int = east - mediumUnit
    val dodec_west: Int = west + mediumUnit

    //distance from outer edge to the corner point//distance from outer edge to the corner point
    val cornerPointDist: Int = 2 * mediumUnit - smallUnit

    val box: Array[PointXY] = Array[PointXY](new PointXY(west, south), new PointXY(west, north), new PointXY(east, north), new PointXY(east, south))

    //the dodecagon has a flat edge (two points) as N,E,S,W, and two edges (one point)//the dodecagon has a flat edge (two points) as N,E,S,W, and two edges (one point)
    //between those.//between those.
    //so lets call the flat edges at N/E/S/W the ordinal edges.//so lets call the flat edges at N/E/S/W the ordinal edges.
    //we are starting with the left/west point of the south ordinal edge, and going around//we are starting with the left/west point of the south ordinal edge, and going around
    //counter clockwise because I believe walls have to go in a certain direction//counter clockwise because I believe walls have to go in a certain direction

    var circle: Array[PointXY] = Array[PointXY](
      //south edge
      new PointXY(west + 3 * mediumUnit, dodec_south), new PointXY(east - 3 * mediumUnit, dodec_south), //SE point
      new PointXY(east - cornerPointDist, south - cornerPointDist), //east edge
      new PointXY(dodec_east, south - 3 * mediumUnit), new PointXY(dodec_east, north + 3 * mediumUnit), //NE point
      new PointXY(east - cornerPointDist, north + cornerPointDist), //north edge
      new PointXY(east - 3 * mediumUnit, dodec_north), new PointXY(west + 3 * mediumUnit, dodec_north), //NW point
      new PointXY(west + cornerPointDist, north + cornerPointDist), //west edge
      new PointXY(dodec_west, north + 3 * mediumUnit), new PointXY(dodec_west, south - 3 * mediumUnit), //SW point
      new PointXY(west + cornerPointDist, south - cornerPointDist))
    ////
    //first, try to create the outer sector//first, try to create the outer sector
    //  NOTE:  the multiple loops must be next to each other, due to build's format//  NOTE:  the multiple loops must be next to each other, due to build's format

    val wall: WallPrefab = StoneConstants.UPPER_WALL

    val outerSectorIndex: Int = map.createSectorFromLoop2(Wall.createLoopAsList(box, wall))
    map.addLoopToSector(outerSectorIndex, Wall.createLoopAsList(circle, wall))
    val outerLoopCircle: Int = map.getAllWallLoops(outerSectorIndex).get(1).iterator.next

    val outerSector: Sector = map.getSector(outerSectorIndex)
    outerSector.setFloorTexture(StoneConstants.UPPER_FLOOR)
    outerSector.setCeilingTexture(StoneConstants.UPPER_CEILING)
    outerSector.setFloorShade(StoneConstants.SHADE)
    outerSector.setCeilingShade(StoneConstants.SHADE)

    ////
    // inner sector// inner sector
    ////

    val innerWallPrefab: WallPrefab = WallPrefab(wall).withTexture(StoneConstants.UPPER_CEILING)
    innerWallPrefab.setXRepeat(4).setShade(0.toShort)

    circle = circle.reverse

    val innerSectorIndex: Int = map.createSectorFromLoop2(Wall.createLoopAsList(circle, innerWallPrefab))
    val innerLoop: Int = map.getSector(innerSectorIndex).getFirstWall

    val innerSector: Sector = map.getSector(innerSectorIndex)
    innerSector.setFloorTexture(StoneConstants.UPPER_FLOOR)
    innerSector.setCeilingTexture(StoneConstants.UPPER_CEILING)
    innerSector.setCeilingZ(floorZ - 3 * 4 * 4096)

    MapUtil.linkInnerSectorWallLoops(map, outerSectorIndex, outerLoopCircle, innerSectorIndex, innerLoop)



    this.connector.setSectorIndex(outerSectorIndex)


    return -(1)

  }
}
