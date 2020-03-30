package trn.prefab

import java.util

import trn.MapImplicits._
import trn.{ISpriteFilter, IdMap, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, SpriteFilter, Map => DMap}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer


object MapBuilder {
  def mapBounds = BoundingBox(DMap.MIN_X, DMap.MIN_Y, DMap.MAX_X, DMap.MAX_Y)

  //def MAX_SECTOR_GROUPS: Int = 1024

  def isMarkerSprite(s: Sprite, lotag: Int): Boolean = {
    s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == lotag
  }
  def isAnchorSprite(s: Sprite): Boolean = isMarkerSprite(s, PrefabUtils.MarkerSpriteLoTags.ANCHOR)

  def waterSortKey(p: PointXYZ): Long = {
    val x = (p.x + 65535L) << 4
    val y = (p.y + 65535L) << 2
    val z = p.z + 65535L
    x + y + z
  }

  def linkAllWater(singleGroup: SectorGroup, conns: Seq[TeleportConnector], tagGenerator: TagGenerator): Unit = {
    val map = singleGroup.getMap
    val aboveWater: Seq[TeleportConnector] = conns.filter(c => map.getSector(c.getSectorId).getLotag == 1).sortBy(t => MapBuilder.waterSortKey(t.getSELocation(singleGroup)))
    val belowWater: Seq[TeleportConnector] = conns.filter(c => map.getSector(c.getSectorId).getLotag == 2).sortBy(t => MapBuilder.waterSortKey(t.getSELocation(singleGroup)))
    if(aboveWater.size != belowWater.size){
      throw new SpriteLogicException(s"There are ${aboveWater.size} above water vs ${belowWater.size} below.")
    }
    //println(s"above water size=${aboveWater.size}")

    // TODO - verify the locations have the same relative positions
    aboveWater.zip(belowWater).foreach { case (above, below) => TeleportConnector.linkTeleporters(above, singleGroup, below, singleGroup, tagGenerator.nextUniqueHiTag()) }
  }

  // TODO - keep as a reference?
  // def linkTwoElevators(group: SectorGroup, elevators: Seq[ElevatorConnector], tagGenerator: TagGenerator, startLower: Boolean): Unit = {
  //   if(elevators.size != 2) throw new IllegalArgumentException
  //   // TODO - why does this work?  physically lower sectors should have a HIGHER z
  //   elevators.sortBy(c => group.map.getSector(c.getSectorId).getFloorZ) // lower z should be higher
  //   ElevatorConnector.linkElevators(elevators(0), group, elevators(1), group, tagGenerator.nextUniqueHiTag(), startLower)
  // }

}

/**
  * Providers extra functionality for placing sectors whose locations are not important (underwater sectors, etc).
  * Automatically separates them, so you don't have to bother hardcoding locations.
  */
trait AnywhereBuilder { // TODO rename to AnywhereWriter or something

  def sgPacker: SectorGroupPacker

  // this is a method on MapBuilder trait
  def pasteSectorGroup(sg: SectorGroup, translate: PointXYZ): PastedSectorGroup

  final def placeAnywhere(sg: SectorGroup): PastedSectorGroup = {
    val topLeft = sgPacker.reserveArea(sg)
    val tr = sg.boundingBox.getTranslateTo(topLeft).withZ(0)
    pasteSectorGroup(sg, tr)
  }
}

/**
  * TODO - put a comment here saying where I can find a basic, vanilla builder
  * TODO - this should go away in favor of SgMapBuilder + MapWriter
  */
trait MapBuilder extends ISectorGroup with TagGenerator {
  val outMap: DMap

  val sgBuilder = new SgMapBuilder(outMap)
  //def pastedSectorGroups: mutable.Buffer[PastedSectorGroup] = sgBuilder.pastedSectorGroups
  def pastedSectorGroups: Seq[PastedSectorGroup] = sgBuilder.pastedSectorGroups

  //val pastedSectorGroups: mutable.Buffer[PastedSectorGroup] = new ListBuffer()
  override def nextUniqueHiTag(): Int = sgBuilder.nextUniqueHiTag()

  override def getMap(): DMap = outMap

  override def findSprites(picnum: Int, lotag: Int, sectorId: Int): util.List[Sprite] = {
    getMap().findSprites(picnum, lotag, sectorId)
  }

  override def findSprites(filters: ISpriteFilter*): java.util.List[Sprite] = {
    getMap().findSprites4Scala(filters.asJava)
  }

  /**
    * TODO - candidate for moving to MapWriter
    * paste the sector group so that it's anchor is at the given location.  If no anchor,
    * its top left corner of the bounder box will be used.
    * @param sg
    * @param location
    * @return
    */
  def pasteSectorGroupAt(sg: SectorGroup, location: PointXYZ, anchorOnly: Boolean = false): PastedSectorGroup = {
    val anchor = sg.sprites.find(isAnchor).map(_.getLocation).getOrElse{
      if(anchorOnly){ throw new SpriteLogicException(("no anchor sprite"))}
      new PointXYZ(sg.boundingBox.xMin, sg.boundingBox.yMin, 0) // TODO - bounding box doesnt do z ...
    }
    //pasteSectorGroup(sg, anchor.getTransformTo(location))
    val (psg, _) = sgBuilder.pasteSectorGroup2(sg, anchor.getTransformTo(location))
    psg
  }

  // TODO - if the other experiments have bugs with pasting sector groups, look at this commented out version
  // I think we want to deprecate this one in favor of pasteSectorGroup2
  // def pasteSectorGroup(sg: SectorGroup, translate: PointXYZ): PastedSectorGroup = {
  //   val psg = new PastedSectorGroup(outMap, MapUtil.copySectorGroup(sg.map, outMap, 0, translate));
  //   pastedSectorGroups.append(psg)
  //   psg
  // }
  def pasteSectorGroup(sg: SectorGroup, translate: PointXYZ): PastedSectorGroup = {
    val (psg, _) = pasteSectorGroup2(sg, translate)
    psg
  }

  def pasteSectorGroup2(sg: SectorGroup, translate: PointXYZ): (PastedSectorGroup, IdMap) = {
    sgBuilder.pasteSectorGroup2(sg, translate)
  }

  // def pasteSectorGroup2(sg: SectorGroup, translate: PointXYZ): (PastedSectorGroup, IdMap)  = {
  //   val copyState = MapUtil.copySectorGroup(sg.map, outMap, 0, translate);
  //   val tp = (new PastedSectorGroup(outMap, copyState), copyState.idmap)
  //   pastedSectorGroups.append(tp._1)
  //   tp
  // }

  /**
    * If the map does not have a player start set yet,
    * sets the player start of the map to the location of the first player start marker sprite it finds
    */
  def setAnyPlayerStart(): Unit = {
    if(!outMap.hasPlayerStart){
      val playerStarts = outMap.allSprites.filter(s =>
        s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == PrefabUtils.MarkerSpriteLoTags.PLAYER_START
      )
      if(playerStarts.size < 1) {
        throw new SpriteLogicException("cannot set player start - there are no player start markers")
      }
      outMap.setPlayerStart(new PlayerStart(playerStarts(0)))
    }
  }

  /**
    * Sets the player start to one of the player start markers in the group.
    * @param psg
    */
  def setPlayerStart(psg: PastedSectorGroup): Unit = {
    //
    val sectorIds = psg.getCopyState.destSectorIds().asScala

    val playerStarts = psg.getMap.allSprites.filter(s =>
      s.getTexture == PrefabUtils.MARKER_SPRITE_TEX
        && s.getLotag == PrefabUtils.MarkerSpriteLoTags.PLAYER_START
        && sectorIds.contains(s.getSectorId)
    )
    if(playerStarts.size < 1) {
      throw new SpriteLogicException(s"no player start markers in sector group")
    }
    outMap.setPlayerStart(new PlayerStart(playerStarts(0)))
  }

  /** sets the player start but bitches at you if there is more than one */
  def setPlayerStart(): Unit = {
    val playerStarts = outMap.allSprites.filter(s =>
      s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == PrefabUtils.MarkerSpriteLoTags.PLAYER_START
    )
    if(playerStarts.size < 1) {
      throw new SpriteLogicException("cannot set player start - there are no player start markers")
    } else if(playerStarts.size > 1){
      throw new SpriteLogicException("cannot set player start - there is more than one player start marker")
    }
    outMap.setPlayerStart(new PlayerStart(playerStarts(0)))
  }

  def clearMarkers(): Unit = sgBuilder.clearMarkers()
  // def clearMarkers(): Unit = {
  //   if(!outMap.hasPlayerStart){
  //     throw new IllegalStateException("Cannot delete marker sprites - there is no player start set")
  //   }
  //   outMap.deleteSprites(SpriteFilter.texture(PrefabUtils.MARKER_SPRITE_TEX))
  // }

  /**
    * @param s
    * @param lotag
    * @returns true if the given sprite is a marker sprite that also has a matching lotag
    */
  def isMarker(s: Sprite, lotag: Int): Boolean = {
    //s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == lotag
    MapBuilder.isMarkerSprite(s, lotag)
  }

  def isAnchor(s: Sprite): Boolean = MapBuilder.isAnchorSprite(s)


  // gets all of the water connections from the pasted sector group, in sorted order
  def getWaterConns(psg: PastedSectorGroup): Seq[TeleportConnector] = {
    val conns = psg.findConnectorsByType(ConnectorType.TELEPORTER).asScala.map(_.asInstanceOf[TeleportConnector])
    val waterConns = conns.filter(_.isWater).map(w => (w, w.getSELocation(psg))).sortBy(t => MapBuilder.waterSortKey(t._2))
    waterConns.unzip._1.toSeq
  }



  //
  // Connector Linking Features
  //
  def linkAllWater(psg1: PastedSectorGroup, psg2: PastedSectorGroup): Unit = {
    //
    // TODO - for now this assumes that all water connectors in both sector groups are part of the same
    // connection (and dont go to some third sector group)

    val waterConns1 = getWaterConns(psg1)
    val waterConns2 = getWaterConns(psg2)
    if(waterConns1.size != waterConns2.size) throw new SpriteLogicException()
    // TODO - we could also check the relative distances bewteen all of them

    waterConns1.zip(waterConns2).foreach { case (c1: TeleportConnector, c2: TeleportConnector) =>
        //TeleportConnector.linkTeleporters(c1, psg1, c2, psg2, nextUniqueHiTag())
        TeleportConnector.linkTeleporters(c1, this, c2, this, nextUniqueHiTag())
    }
  }


  def getWaterConns2(groups: Seq[PastedSectorGroup]): Seq[TeleportConnector] = {
    val conns = groups.flatMap { psg => psg.findConnectorsByType(ConnectorType.TELEPORTER).asScala.map(_.asInstanceOf[TeleportConnector]) }
    val waterConns = conns.filter(_.isWater).map(w => (w, w.getSELocation(this))).sortBy(t => MapBuilder.waterSortKey(t._2))
    waterConns.unzip._1
  }

  def linkAllWater2(aboveWater: Seq[PastedSectorGroup], belowWater: Seq[PastedSectorGroup]): Unit = {

    val aboveWaterConns = getWaterConns2(aboveWater)
    val belowWaterConns = getWaterConns2(belowWater)

    aboveWaterConns.zip(belowWaterConns).foreach { case (aboveC: TeleportConnector, belowC: TeleportConnector) =>
        TeleportConnector.linkTeleporters(aboveC, this, belowC, this, nextUniqueHiTag())
    }

  }

  /**
    * NOTE:  the reason this is a method on the Builder object is because of the need to call nextUniqueHitag
    *
    * TODO - 1) verity locations of SE sprites have the same relative positions  2) make nextUniqueHitag scan the map...
    *
    * search through a single sector group, and assume that all of the above water connectors should match all of the
    * below water connectors
    *
    * @param singleGroup
    */
  def linkAllWater(singleGroup: SectorGroup): Unit = {
    val conns = singleGroup.getTeleportConnectors().filter(c => c.isWater && !c.isLinked(singleGroup.map))
    MapBuilder.linkAllWater(singleGroup, conns, this)
    // val map = singleGroup.getMap
    // val conns = singleGroup.getTeleportConnectors().filter(c => c.isWater && !c.isLinked(map))
    // val aboveWater: Seq[TeleportConnector] = conns.filter(c => map.getSector(c.getSectorId).getLotag == 1).sortBy(t => MapBuilder.waterSortKey(t.getSELocation(singleGroup)))
    // val belowWater: Seq[TeleportConnector] = conns.filter(c => map.getSector(c.getSectorId).getLotag == 2).sortBy(t => MapBuilder.waterSortKey(t.getSELocation(singleGroup)))
    // if(aboveWater.size != belowWater.size){
    //   throw new SpriteLogicException(s"There are ${aboveWater.size} above water vs ${belowWater.size} below.")
    // }
    // println(s"above water size=${aboveWater.size}")

    // // TODO - verify the locations have the same relative positions
    // aboveWater.zip(belowWater).foreach { case (above, below) => TeleportConnector.linkTeleporters(above, singleGroup, below, singleGroup, nextUniqueHiTag()) }

  }

  // TODO - maybe keep this as a reference?
  // def linkTwoElevators(group: SectorGroup, connectorId: Int): Unit = {
  //   if(connectorId < 1) throw new IllegalArgumentException
  //   val elevators: Seq[ElevatorConnector] = group.getElevatorConnectors.filter(c => c.getConnectorId == connectorId && !c.isLinked(group.getMap))
  //   val startLower = false // TODO - pass in
  //   MapBuilder.linkTwoElevators(group, elevators, this, startLower)
  //   // if(elevators.size != 2) throw new SpriteLogicException(s"Number of teleporters with connectorId ${connectorId}")
  //   // // TODO - why does this work?  physically lower sectors should have a HIGHER z
  //   // elevators.sortBy(c => group.map.getSector(c.getSectorId).getFloorZ) // lower z should be higher
  //   // ElevatorConnector.linkElevators(elevators(0), group, elevators(1), group, nextUniqueHiTag(), false)
  // }

  def applyPaletteToAll(textureId: Int, palette: Int): Unit = {
    if(textureId < 0 || palette < 0) throw new IllegalArgumentException
    this.outMap.allWalls.foreach { w =>
      if(w.getTexture == textureId){
        w.setPal(palette)
      }
    }
    this.outMap.sectors.asScala.foreach { s =>
      if(s.getFloorTexture == textureId){
        s.setFloorPalette(palette)
      }
      if(s.getCeilingTexture == textureId){
        s.setCeilingPalette(palette)
      }
    }
  }

}
