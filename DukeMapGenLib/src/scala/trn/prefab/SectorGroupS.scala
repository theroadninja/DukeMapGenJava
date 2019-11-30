package trn.prefab

import trn.duke.{MapErrorException, TextureList}
import trn.{DukeConstants, ISpriteFilter, PointXY, PointXYZ, Sector, Sprite, Wall, Map => DMap}
import trn.MapImplicits._

import scala.collection.JavaConverters._ // this is the good one


class CopyPasteMapBuilder(val outMap: DMap) extends MapBuilder {

}

/**
  * Set of marker + connectors used to connect a "child" sector group to a "parent" sector group.
  * @param childMarker
  * @param childConnectors
  */
case class ChildPointer(
  val childMarker: Sprite,
  val connectorId: Int,
  val childConnectors: Seq[RedwallConnector],
)
{
  def sectorId: Int = childMarker.getSectorId
  def parentGroupId: Int = childMarker.getHiTag
  def connectorsJava: java.util.List[RedwallConnector] = childConnectors.asJava
}

object SectorGroupS {

  /**
    * This should only be called by SectorGroupBuilder
    */
  def newSG(map: DMap, sectorGroupId: Int, props: SectorGroupProperties): SectorGroup = {

    val connectors = try {
      Connector.findConnectors(map)
      //for(Connector c : Connector.findConnectors(map)){
      //  addConnector(c);
      //}
      //}catch(SpriteLogicException ex){
    }catch{
      case ex: Exception => throw new SpriteLogicException(
        "exception while scanning connectors in sector group.  id=" + sectorGroupId,
        ex
      )
    }

    new SectorGroup(map, sectorGroupId, props, connectors)
  }
}

class SectorGroupS(val map: DMap, val sectorGroupId: Int, val props: SectorGroupProperties, val connectors: java.util.List[Connector])
  extends SectorGroupBase
    with ConnectorCollection
    with ISectorGroup
{
  // val connectors: java.util.List[Connector] = new java.util.ArrayList[Connector]();
  val autoTexts: java.util.List[AutoText] = new java.util.ArrayList[AutoText]

  protected def wallSeq: Seq[Wall] = {
    val walls = Seq.newBuilder[Wall]
    for(i <- 0 until map.getWallCount){
      walls += map.getWall(i)
    }
    walls.result
  }

  // TODO - get rid of this
  override def getMap: DMap = map

  override def findSprites(picnum: Int, lotag: Int, sectorId: Int): java.util.List[Sprite] = {
    getMap().findSprites(picnum, lotag, sectorId)
  }

  override def findSprites(filters: ISpriteFilter*): java.util.List[Sprite] = getMap().findSprites4Scala(filters.asJava)

  protected def addConnector(c: Connector): Unit = {
    require(c.getSectorId >= 0)
    require(c.getSectorId < map.getSectorCount)
    connectors.add(c)
  }

  def getGroupId: Int = sectorGroupId

  def allSprites: Seq[Sprite] = map.allSprites


  def allWalls: Seq[Wall] = wallSeq

  val allSectorIds = (0 until map.getSectorCount).toSet

  def sectorCount: Int = map.getSectorCount

  def copy(): SectorGroup = {
    SectorGroupBuilder.createSectorGroup(map.copy, this.sectorGroupId, this.props)
    //new SectorGroup(map.copy, this.sectorGroupId);
  }

  def withModifiedSectors(f: Sector => Unit): SectorGroup = {
    val cp = copy()
    cp.getMap.sectors.asScala.foreach(f)
    cp
  }

  /**
    * @return a copy of this sector group, flipped about the X axis
    */
  def flippedX(x: Int): SectorGroup = {
    //new SectorGroup(map.flippedX(x), this.sectorGroupId)
    SectorGroupBuilder.createSectorGroup(map.flippedX(x), this.sectorGroupId, this.props)
  }

  def flippedX(): SectorGroup = flippedX(getAnchor.x)

  def flippedY(y: Int): SectorGroup = {
    //new SectorGroup(map.flippedY(y), this.sectorGroupId)
    SectorGroupBuilder.createSectorGroup(map.flippedY(y), this.sectorGroupId, this.props)
  }

  def flippedY(): SectorGroup = flippedY(getAnchor.y)

  def rotateAroundCW(anchor: PointXY): SectorGroup = {
    //new SectorGroup(map.rotatedCW(anchor), this.sectorGroupId)
    SectorGroupBuilder.createSectorGroup(map.rotatedCW(anchor), this.sectorGroupId, this.props)
  }

  def rotateCW: SectorGroup = rotateAroundCW(this.rotationAnchor)

  def rotateCCW: SectorGroup = rotateCW.rotateCW.rotateCW

  def rotate180: SectorGroup = rotateCW.rotateCW

  def rotateAroundCW(anchor: PointXYZ): SectorGroup = rotateAroundCW(anchor.asXY)

  // TODO - mutable; get rid of this is we move all java stuff to scala
  def addAutoText(at: AutoText): Unit ={
    this.autoTexts.add(at)
  }

  def getAutoTextById(autoTextId: Int): AutoText = {
    val results: Seq[AutoText] = this.autoTexts.asScala.filter(_.autoTextId == autoTextId)
    results.size match {
      case 0 => throw new SpriteLogicException(s"No auto text with id ${autoTextId}")
      case 1 => results.head
      case 2 => throw new SpriteLogicException(s"More than one auto text with id ${autoTextId} - shouldnt be possible")
    }
  }

  // @throws(classOf[MapErrorException])
  // protected def updateConnectors(): Unit = ???
  protected def updateConnectors(): Unit = {
    connectors.clear()
    Connector.findConnectors(map).forEach(c => addConnector(c))
  }


  def getRedwallConnector(connectorId: Int): RedwallConnector = {
    getConnector(connectorId).asInstanceOf[RedwallConnector]
  }

  // TODO - move to trait
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

  // TODO - move to trait
  def getRedwallConnectors(connectorType: Int): Seq[RedwallConnector] = {
    if(! ConnectorType.isRedwallType(connectorType)){
      throw new IllegalArgumentException(s"not a redwall connector type: ${connectorType}")
    }
    connectors.asScala.filter(c => c.getConnectorType == connectorType).map(_.asInstanceOf[RedwallConnector])
  }

  // TODO - move to base class or interface something
  def findFirstConnector(cf: ConnectorFilter): Connector = {
    val it: java.util.Iterator[Connector] = Connector.findConnectors(connectors, cf).iterator();
    //Iterator<Connector> it = Connector.findConnectors(this.connectors_(), cf).iterator();
    //return it.hasNext() ? it.next() : null;
    if(it.hasNext) {
      it.next
    }else{
      None.orNull // TODO!
    }
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


    if(result.sectorCount != this.map.getSectorCount + group2.sectorCount){
      throw new SpriteLogicException()
    }

    result.updateConnectors()
    result
  }

  def connectedToChild(childPtr: ChildPointer, child: SectorGroup, tagGenerator: TagGenerator): SectorGroup = {
    // parent and child must have the same number of connectors
    if(this.getRedwallConnectorsById(childPtr.connectorId).size != childPtr.connectorsJava.size){
      throw new SpriteLogicException("parent and child sector groups have different count of connectors with ID " + childPtr.connectorId);
    }
    // TODO - allow child sector groups to connect to other child sector groups with same parent (need to
    //     sort them first, and dedupe all connector IDs, to ensure deterministic behavior)
    // TODO - sort children by connector id first! (so that at least results are deterministic)
    require(childPtr.connectorsJava.size == 1, "not implemented yet")
    val c1 = getRedwallConnectorsById(childPtr.connectorId)
    val c2 = child.getRedwallConnectorsById(childPtr.connectorId)
    if(this.sectorGroupId != childPtr.parentGroupId || c1.size != c2.size) throw new IllegalArgumentException

    if(c1.size > 1) throw new RuntimeException("not implemented yet")

    val result = connectedTo(c1(0), child, c2(0))

    // TODO - but now all the connectors are fucked up...

    val conns = result.getTeleportConnectors().filter(c => c.isWater && !c.isLinked(result.map) && c.getConnectorId == childPtr.connectorId)
    MapBuilder.linkAllWater(result, conns, tagGenerator)

    // find elevators with matching connector Ids
    val elevatorConns = result.getElevatorConnectorsById(childPtr.connectorId)
    if(elevatorConns.size == 2){
      val sorted = elevatorConns.sortBy(ElevatorConnector.sortKey(_, result.map))
      MapWriter.withElevatorsLinked(result, sorted(0), sorted(1), tagGenerator.nextUniqueHiTag(), false)
    }else if(elevatorConns.size != 0){
      throw new RuntimeException("not implemented yet") // need to auto detect which elevators match ...
    }else{
      result
    }
  }

  /**
    * Called by the palette loading code to attach sector group children to their parent.
    * THIS object is the parent.
    *
    * 1. the child sector's marker must match THIS parent's sector group ID.
    * 2. more than one child cannot use the same connector id
    * 3. the connectorId the child uses to connect must match the parent's in quantity
    * 		(e.g. if a child has two redwall connectors with ID 123, the parent must have exactly two)
    */
  def connectedToChildren(children: java.util.List[SectorGroup], tagGenerator: TagGenerator): SectorGroup = {
    var result = this.copy
    if(children.size < 1){
      return result
    }
    if(sectorGroupId == -1){
      throw new SpriteLogicException("cannot connect children to parent with no group id")
    }

    val seenConnectorIds = new java.util.HashSet[Int](children.size())
    children.forEach { child =>
      val childPtr = child.getChildPointer()
      require(sectorGroupId == childPtr.parentGroupId)

      if(seenConnectorIds.contains(childPtr.connectorId)){
        throw new SpriteLogicException("more than one child sector group is trying to use connector " + childPtr.connectorId)
      }else{
        seenConnectorIds.add(childPtr.connectorId)
        seenConnectorIds.addAll(child.allConnectorIds.asScala.map(_.toInt).asJava)
      }

      result = result.connectedToChild(childPtr, child, tagGenerator);
    }
    result
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

  // def boundingBox: BoundingBox = {
  //   val walls = wallSeq
  //   val w = walls(0)
  //   walls.foldLeft(BoundingBox(w.getX, w.getY, w.getX, w.getY)){ (acc, wall) =>
  //     acc.add(wall.getX, wall.getY)
  //   }
  // }

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

  // well this is a terriable name - maybe getAnchor should do this
  private def rotationAnchor: PointXYZ = getAnchorSprite.map(_.getLocation).getOrElse {
    val bb = this.boundingBox
    new PointXY(bb.xMin, bb.yMin).withZ(0)
  }

  def getAnchorSprite: Option[Sprite] = sprites.find(MapBuilder.isAnchorSprite)

}
