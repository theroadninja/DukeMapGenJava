package duchy.experiments.render.maze

import trn.MapUtil

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

  def genderToHeading(connectorType: Int, gender: Boolean): OldHeadingScala = {
    if (connectorType == NORTH_SOUTH) {
      if (gender) OldHeadingScala.NORTH else OldHeadingScala.SOUTH
    } else if (connectorType == EAST_WEST) {
      if (gender) OldHeadingScala.EAST else OldHeadingScala.WEST
    } else {
      throw new IllegalArgumentException
    }
  }

  def apply(heading: OldHeadingScala, block: Block): LegacyConnector = heading match {
    case OldHeadingScala.NORTH => new LegacyConnector(LegacyConnector.NORTH_SOUTH, block, LegacyConnector.MALE)
    case OldHeadingScala.EAST => new LegacyConnector(LegacyConnector.EAST_WEST, block, LegacyConnector.MALE)
    case OldHeadingScala.SOUTH => new LegacyConnector(LegacyConnector.NORTH_SOUTH, block, LegacyConnector.FEMALE)
    case OldHeadingScala.WEST => new LegacyConnector(LegacyConnector.EAST_WEST, block, LegacyConnector.FEMALE)
  }

  def eastEdge(block: Block) = LegacyConnector(OldHeadingScala.EAST, block)

  def westEdge(block: Block) = LegacyConnector(OldHeadingScala.WEST, block)

  def northEdge(block: Block) = LegacyConnector(OldHeadingScala.NORTH, block)

  def southEdge(block: Block) = LegacyConnector(OldHeadingScala.SOUTH, block)

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
