package trn.prefab

import trn.duke.{MapErrorException, TextureList}
import trn.{DukeConstants, ISpriteFilter, PointXY, PointXYZ, Sprite, Wall, Map => DMap}
import trn.MapImplicits._

import scala.collection.JavaConverters._ // this is the good one


class CopyPasteMapBuilder(val outMap: DMap) extends MapBuilder {

}

class SectorGroupS(val map: DMap, val sectorGroupId: Int) {
  val connectors: java.util.List[Connector] = new java.util.ArrayList[Connector]();

  def copy(): SectorGroup = {
    new SectorGroup(map.copy, this.sectorGroupId);
  }
  /**
    * @return a copy of this sector group, flipped about the X axis
    */
  def flippedX(x: Int): SectorGroup = {
    new SectorGroup(map.flippedX(x), this.sectorGroupId)
  }

  def flippedX(): SectorGroup = flippedX(getAnchor.x)



  def flippedY(y: Int): SectorGroup = {
    new SectorGroup(map.flippedY(y), this.sectorGroupId)
  }

  def flippedY(): SectorGroup = flippedY(getAnchor.y)

  def rotateAroundCW(anchor: PointXY): SectorGroupS = {
    new SectorGroup(map.rotatedCW(anchor), this.sectorGroupId)

  }

  @throws(classOf[MapErrorException])
  protected def updateConnectors(): Unit = ???



  def getRedwallConnector(connectorType: Int, allowMoreThanOne: Boolean = false): RedwallConnector = {
    if(! ConnectorType.isRedwallType(connectorType)){
      throw new IllegalArgumentException(s"not a redwall connector type: ${connectorType}")
    }
    val matching = connectors.asScala.filter(c => c.getConnectorType == connectorType)
    matching.size match {
      case i: Int if i < 1 => throw new NoSuchElementException(s"no connector with type ${connectorType} exists")
      case i: Int if i == 1 || allowMoreThanOne => matching.head.asInstanceOf[RedwallConnector]
      case _ => throw new SpriteLogicException(s"more than one connector with type ${connectorType} found")
    }
  }

  def getRedwallConnectors(connectorType: Int): Seq[RedwallConnector] = {
    if(! ConnectorType.isRedwallType(connectorType)){
      throw new IllegalArgumentException(s"not a redwall connector type: ${connectorType}")
    }
    connectors.asScala.filter(c => c.getConnectorType == connectorType).map(_.asInstanceOf[RedwallConnector])
  }

  def getTeleportConnectors(): Seq[TeleportConnector] = {
    connectors.asScala.filter(c => c.getConnectorType == ConnectorType.TELEPORTER).map(_.asInstanceOf[TeleportConnector])
  }

  def getElevatorConnectors(): Seq[ElevatorConnector] = {
    connectors.asScala.filter(c => c.getConnectorType == ConnectorType.ELEVATOR).map(_.asInstanceOf[ElevatorConnector])
  }

  def connectedTo(joinType: RedwallJoinType, group2: SectorGroup): SectorGroup = {
    val conn1 = getRedwallConnector(joinType.connectorType1)
    val conn2 = group2.getRedwallConnector(joinType.connectorType2, false)
    connectedTo(conn1, group2, conn2)
  }



  // Merging
  //  - what do we do about two anchors?
  //  - can we remove the redwall connectors?
  // TODO - right now, the sector copy code ONLY copies sectors linked with red walls.  Two unconnected
  // sectors will not both be copied (the bug isnt here, but in the main copy code ...
  def connectedTo(conn1: RedwallConnector, group2: SectorGroup, conn2: RedwallConnector): SectorGroup = {
    if(conn1 == conn2) throw new IllegalArgumentException("same connection object passed") // sanity check

    val result = this.copy()

    val anchorSprite = result.getAnchorSprite
    val removeSecondAnchor = anchorSprite.nonEmpty && group2.getAnchorSprite.nonEmpty

    val tmpBuilder = new CopyPasteMapBuilder(result.map)
    val cdelta = conn2.getTransformTo(conn1)
    val (_, idmap) = tmpBuilder.pasteSectorGroup2(group2, cdelta)
    val pastedConn2 = conn2.translateIds(idmap, cdelta)

    // TODO - link redwalls  ( TODO - make this a member of the builder? )
    //PrefabUtils.joinWalls(result.map, conn1, pastedConn2)
    conn1.linkConnectors(result.map, pastedConn2)

    // CLEANUP:  remove any other anchor sprite
    if(removeSecondAnchor){
      tmpBuilder.outMap.deleteSprites(new ISpriteFilter {
        override def matches(s: Sprite): Boolean = {
          tmpBuilder.isAnchor(s) && s.getLocation != anchorSprite.get.getLocation
        }
      })
      // for(i <- 0 until tmpBuilder.outMap.getSpriteCount){
      //   val s: Sprite = tmpBuilder.outMap.getSprite(i)
      //   if(tmpBuilder.isAnchor(s) && s.getLocation != anchorSprite.get){
      //     tmpBuilder.outMap.deleteSprite(i) // it is the other anchor, delete it
      //   }
      // }
    }

    // DELETE USED CONNECTORS
    conn1.removeConnector(tmpBuilder.outMap)
    pastedConn2.removeConnector(tmpBuilder.outMap)


    if(result.getSectorCount != this.map.getSectorCount + group2.getSectorCount){
      throw new SpriteLogicException()
    }

    result.updateConnectors()
    result
  }


  private def wallSeq(): Seq[Wall] = {
    val walls = Seq.newBuilder[Wall]
    for(i <- 0 until map.getWallCount){
      walls += map.getWall(i)
    }
    walls.result
  }

  private def bbDimension(values: Seq[Int]): Int = values.max - values.min
  def bbHeight: Int = bbDimension(wallSeq.map(_.getY))
  def bbWidth: Int = bbDimension(wallSeq.map(_.getX))

  /**
    * Returns the "top left" corner of the bounding box, to aid in placing the group on a map.
    * In duke build files, the x and y axis are like this:
    *
    *   -  <-- X --> +
    *   |
    *   Y
    *   |
    *   +
    *
    * @return
    */
  def bbTopLeft: (Int, Int) = {
    val x = wallSeq.map(_.getX)
    val y = wallSeq.map(_.getY)
    (x.min, y.min)
  }

  def boundingBox: BoundingBox = {
    val walls = wallSeq
    val w = walls(0)
    walls.foldLeft(BoundingBox(w.getX, w.getY, w.getX, w.getY)){ (acc, wall) =>
      acc.add(wall.getX, wall.getY)
    }
  }

  def hasMarker(lotag: Int): Boolean = {
    for(i <- 0 until map.getSpriteCount){
      val sprite = map.getSprite(i)
      if(sprite.getTexture == PrefabUtils.MARKER_SPRITE_TEX && sprite.getLotag == lotag){
        return true
      }
    }
    return false
  }

  def containsSprite(f: (Sprite) => Boolean): Boolean = {
    for(i <- 0 until map.getSpriteCount){
      if(f(map.getSprite(i))){
        return true
      }
    }
    return false
  }

  def sprites: Seq[Sprite] = map.allSprites

  def hasPlayerStart: Boolean = hasMarker(PrefabUtils.MarkerSpriteLoTags.PLAYER_START)

  def hasEndGame: Boolean = containsSprite{ s =>
    s.getTexture == DukeConstants.TEXTURES.NUKE_BUTTON && s.getLotag == DukeConstants.LOTAGS.NUKE_BUTTON_END_LEVEL
  }

  def getAnchor: PointXYZ = getAnchorSprite.getOrElse(
    throw new SpriteLogicException("no anchor sprite")
  ).getLocation

  def getAnchorSprite: Option[Sprite] = sprites.find(MapBuilder.isAnchorSprite)

}
