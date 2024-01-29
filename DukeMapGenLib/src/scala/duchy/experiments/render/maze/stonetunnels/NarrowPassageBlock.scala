package duchy.experiments.render.maze.stonetunnels

import duchy.experiments.render.maze.{SimpleBlock, AbstractBlock, LegacyConnector}
import org.apache.commons.lang3.tuple.Pair
import trn.{Wall, Sector}
import trn.maze.Heading

object NarrowPassageBlock {
  val VERTICAL = 0
}
class NarrowPassageBlock(gridCoordinate: Pair[Integer, Integer], rotation: Int) extends AbstractBlock(gridCoordinate) {

  val connectors: Map[Int, LegacyConnector] = if(rotation == NarrowPassageBlock.VERTICAL){
    Map(
      Heading.NORTH.arrayIndex -> LegacyConnector.northEdge(this),
      Heading.SOUTH.arrayIndex -> LegacyConnector.southEdge(this),
    )
  }else{
    Map(
      Heading.EAST.arrayIndex -> LegacyConnector.eastEdge(this),
      Heading.WEST.arrayIndex -> LegacyConnector.westEdge(this),

    )
  }

  override def getConnector(heading: Heading): LegacyConnector = {
    connectors(heading.arrayIndex)
  }

  /**
    *
    * @param map
    * @return the index of the (a?) sector that was created.
    */
  override def draw(map: trn.Map): Int = {

    val WALL_LENGTH = SimpleBlock.WALL_LENGTH

    val west = gridCoordinate.getLeft * WALL_LENGTH
    val east = (gridCoordinate.getLeft + 1) * WALL_LENGTH
    val north = gridCoordinate.getRight * WALL_LENGTH
    val south = (gridCoordinate.getRight + 1) * WALL_LENGTH

    val wallTex = StoneConstants.UPPER_WALL_TEX

    val vMiddle = north + (south - north) / 2
    val bowedDist = 512 //amount middle points are drawn in

    val nw = new Wall(west, north, wallTex, 16, 8) //first wall; also matches the grid coordinate

    val ne = new Wall(east, north, wallTex, 16, 8)
    val ne2 = new Wall(east - bowedDist, vMiddle - 512, wallTex, 8, 8)
    val ne3 = new Wall(east - bowedDist, vMiddle + 512, wallTex, 16, 8)
    val se = new Wall(east, south, wallTex, 16, 8)
    val sw = new Wall(west, south, wallTex, 16, 8)
    val sw2 = new Wall(west + bowedDist, vMiddle + 512, wallTex, 8, 8)
    val sw3 = new Wall(west + bowedDist, vMiddle - 512, wallTex, 16, 8)


    val wallPrefab = StoneConstants.UPPER_WALL
    wallPrefab.writeToWall(nw)
    wallPrefab.writeToWall(ne)
    wallPrefab.writeToWalls(ne2, ne3)
    wallPrefab.writeToWall(se)
    wallPrefab.writeToWall(sw)
    wallPrefab.writeToWalls(sw2, sw3)

    //int sectorIndex =  map.createSectorFromLoop(nw, ne, se, sw);//int sectorIndex =  map.createSectorFromLoop(nw, ne, se, sw);
    val sectorIndex = map.createSectorFromLoop(nw, ne, ne2, ne3, se, sw, sw2, sw3)

    val s = map.getSector(sectorIndex)
    StoneConstants.UPPER_SECTOR.writeTo(s)

    val createdSectorIndex = sectorIndex

    for (c: LegacyConnector <- connectors.values) {
      if (c != null) c.setSectorIndex(createdSectorIndex)
    }

    return sectorIndex
  }
}
