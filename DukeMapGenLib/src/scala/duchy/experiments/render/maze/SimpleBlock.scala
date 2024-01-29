package duchy.experiments.render.maze

import org.apache.commons.lang3.tuple.Pair
import trn.{Wall, Sector}
import trn.maze.Heading

object SimpleBlock {

  val WALL_LENGTH = 2048 //2 x largest grid size


}

/**
  * This was hastily ported from java.
  */
class SimpleBlock(gridCoordinate: Pair[Integer, Integer]) extends AbstractBlock(gridCoordinate) {

  override def getConnector(heading: Heading): LegacyConnector = connectors(heading.arrayIndex)

  val connectors: Seq[LegacyConnector] = Seq(
    new LegacyConnector(LegacyConnector.NORTH_SOUTH, this, LegacyConnector.MALE), // NORTH
    new LegacyConnector(LegacyConnector.EAST_WEST, this, LegacyConnector.MALE), // EAST
    new LegacyConnector(LegacyConnector.NORTH_SOUTH, this, LegacyConnector.FEMALE), // SOUTH
    new LegacyConnector(LegacyConnector.EAST_WEST, this, LegacyConnector.FEMALE), // WEST
  )
  private var wallTex = 0
  private var ceilTex = 0
  private var floorTex = 0

  private var floorZ: Integer = null

  private var wallPrefab: WallPrefab = null

  private var sectorPrefab: SectorPrefab = null

  /**
    * index of the sector that was created by this block.
    * Note: in the future multiple sectors will be created by this block ...
    */
  private var createdSectorIndex = -1

  def setWallPrefab(w: WallPrefab): Unit = {
    this.wallPrefab = w
  }

  def setSectorPrefab(s: SectorPrefab): Unit = {
    this.sectorPrefab = s
  }

  def getFloorZ: Integer = this.floorZ

  def setFloorZ(z: Integer): Unit = {
    this.floorZ = z
    for (c <- connectors) {
      c.setFloorZ(floorZ)
    }
  }


  /**
    *
    * @param map
    * @return the index of the (a?) sector that was created.
    */
  override def draw(map: trn.Map): Int = {

    val west = gridCoordinate.getLeft * SimpleBlock.WALL_LENGTH
    val east = (gridCoordinate.getLeft + 1) * SimpleBlock.WALL_LENGTH
    val north = gridCoordinate.getRight * SimpleBlock.WALL_LENGTH
    val south = (gridCoordinate.getRight + 1) * SimpleBlock.WALL_LENGTH

    val nw = new Wall(west, north, wallTex, 16, 8) //first wall; also matches the grid coordinate

    val ne = new Wall(east, north, wallTex, 16, 8)
    val se = new Wall(east, south, wallTex, 16, 8)
    val sw = new Wall(west, south, wallTex, 16, 8)

    if (this.wallPrefab != null) {
      wallPrefab.writeToWall(nw)
      wallPrefab.writeToWall(ne)
      wallPrefab.writeToWall(se)
      wallPrefab.writeToWall(sw)
    }

    val sectorIndex = map.createSectorFromLoop(nw, ne, se, sw)
    val s = map.getSector(sectorIndex)
    s.setFloorTexture(this.floorTex)
    s.setCeilingTexture(this.ceilTex)

    if (this.floorZ != null) s.setFloorZ(this.floorZ)

    if (this.sectorPrefab != null) this.sectorPrefab.writeTo(s)


    createdSectorIndex = sectorIndex

    for (c <- connectors) {
      c.setSectorIndex(createdSectorIndex)
    }

    sectorIndex
  }
}
