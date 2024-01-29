package duchy.experiments.render.maze

import trn.MapUtil
import trn.maze.Heading

object LegacyConnector {

  var MALE = true
  var FEMALE = false

  /**
    * connector that glues the north edge of a grid-based block to the south edge of another
    * north is male, south is female
    */
  val NORTH_SOUTH = 1

  /**
    * connector that glues the east edge of a grid-based block to the west edge of another
    * east is male, west is female
    */
  val EAST_WEST = 2

  def genderToHeading(connectorType: Int, gender: Boolean): Heading = {
    if (connectorType == NORTH_SOUTH) {
      if (gender) Heading.NORTH else Heading.SOUTH
    } else if (connectorType == EAST_WEST) {
      if (gender) Heading.EAST else Heading.WEST
    } else {
      throw new IllegalArgumentException
    }
  }

  def apply(heading: Heading, block: Block): LegacyConnector = heading match {
    case Heading.NORTH => new LegacyConnector(LegacyConnector.NORTH_SOUTH, block, LegacyConnector.MALE)
    case Heading.EAST => new LegacyConnector(LegacyConnector.EAST_WEST, block, LegacyConnector.MALE)
    case Heading.SOUTH => new LegacyConnector(LegacyConnector.NORTH_SOUTH, block, LegacyConnector.FEMALE)
    case Heading.WEST => new LegacyConnector(LegacyConnector.EAST_WEST, block, LegacyConnector.FEMALE)
  }

  def eastEdge(block: Block) = LegacyConnector(Heading.EAST, block)

  def westEdge(block: Block) = LegacyConnector(Heading.WEST, block)

  def northEdge(block: Block) = LegacyConnector(Heading.NORTH, block)

  def southEdge(block: Block) = LegacyConnector(Heading.SOUTH, block)

}
class LegacyConnector(val connectorType: Int, parentBlock: Block, gender: Boolean) {

  val heading = LegacyConnector.genderToHeading(connectorType, gender)
  var sectorIndex = -1
  var floorZ: Integer = null

  def setSectorIndex(index: Int): Unit = {
    this.sectorIndex = index
  }

  def setFloorZ(floorZ: Integer): Unit = {
    this.floorZ = floorZ
  }

  def getFloorZ: Integer = this.floorZ

  def getCreatedSectorIndex: Int = {
    if (sectorIndex == -1) throw new RuntimeException("sector index not set on connector")
    sectorIndex
  }

  def draw(map: trn.Map, otherBlock: Block): Unit = {
    if ((this.parentBlock eq otherBlock) || otherBlock == null) throw new IllegalArgumentException
    val other = if (this.gender == LegacyConnector.MALE) {
      otherBlock.getConnector(LegacyConnector.genderToHeading(connectorType, LegacyConnector.FEMALE))
    } else {
      otherBlock.getConnector(LegacyConnector.genderToHeading(connectorType, LegacyConnector.MALE))
    }
    this.draw2(map, other)
  }

  protected def draw2(map: trn.Map, other: LegacyConnector): Unit = {
    MapUtil.autoLinkWalls(map, this.getCreatedSectorIndex, other.getCreatedSectorIndex)
  }



}
