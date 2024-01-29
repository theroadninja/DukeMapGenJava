package duchy.experiments.render.maze.stonetunnels

import org.apache.commons.lang3.tuple.Pair
import trn.duke.TextureList
import trn.{BuildConstants, PointXY, Wall, Sprite, AngleUtil}
import trn.duke.experiments.gridblock.{LegacyConnector, AbstractBlock}
import trn.maze.Heading

class ExitBlock(gridCoordinate: Pair[Integer, Integer]) extends AbstractBlock(gridCoordinate) {

  val connector = LegacyConnector.northEdge(this)

  val connectorEdge = Heading.NORTH;  // rotation didn't exist when I originally wrote this
  val floorZ = StoneConstants.UPPER_FLOORZ;

  override def getConnector(heading: Heading): LegacyConnector = if (heading == connectorEdge) { connector } else { None.orNull }

  /**
    *
    * @param map
    * @return the index of the (a?) sector that was created.
    */
  override def draw(map: trn.Map): Int = {
    val south = getSouthEdge
    val west = getWestEdge
    val north = getNorthEdge
    val east = getEastEdge
    val box = Array[PointXY](new PointXY(west, south), new PointXY(west, north), new PointXY(east, north), new PointXY(east, south))
    val sectorIndex = map.createSectorFromLoop2(Wall.createLoopAsList(box, StoneConstants.UPPER_WALL))
    StoneConstants.UPPER_SECTOR.writeTo(map.getSector(sectorIndex))

    val exitSprite = new Sprite((east + west) / 2, south, 256 << 4, //z
      sectorIndex.toShort)

    exitSprite.setTexture(TextureList.Switches.NUKE_BUTTON)
    exitSprite.setCstat(Sprite.CSTAT_FLAGS.PLACED_ON_WALL.toShort)
    exitSprite.setXRepeat(21)
    exitSprite.setYRepeat(26)
    exitSprite.setAngle(AngleUtil.ANGLE_UP)
    exitSprite.setLotag(TextureList.Switches.NUKE_BUTTON_LOTAG) // how can 65535 be correct if its a short?

    map.addSprite(exitSprite)

    connector.setSectorIndex(sectorIndex)

    return -1
  }
}
