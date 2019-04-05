package trn.prefab

import trn.MapImplicits._
import trn.{MapUtil, PlayerStart, PointXYZ, Sprite, SpriteFilter, Map => DMap}
import scala.collection.JavaConverters._


object MapBuilder {
  def isMarkerSprite(s: Sprite, lotag: Int): Boolean = {
    s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == lotag
  }
  def isAnchorSprite(s: Sprite): Boolean = isMarkerSprite(s, PrefabUtils.MarkerSpriteLoTags.ANCHOR)

}

trait MapBuilder {
  val outMap: DMap

  //var hiTagCounter = 1 + Math.max(0, outMap.allSprites.map(_.getHiTag).max)
  var hiTagCounter = 1

  def nextUniqueHiTag(): Int = {
    val i = hiTagCounter
    hiTagCounter += 1
    i
  }

  /**
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
    pasteSectorGroup(sg, anchor.getTransformTo(location))
  }

  def pasteSectorGroup(sg: SectorGroup, translate: PointXYZ): PastedSectorGroup = {
    new PastedSectorGroup(outMap, MapUtil.copySectorGroup(sg.map, outMap, 0, translate));
  }

  /** sets the player start of the map to the location of the first player start marker sprite it finds */
  def setAnyPlayerStart(): Unit = {
    val playerStarts = outMap.allSprites.filter(s =>
      s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == PrefabUtils.MarkerSpriteLoTags.PLAYER_START
    )
    if(playerStarts.size < 1) {
      throw new SpriteLogicException("cannot set player start - there are no player start markers")
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

  def clearMarkers(): Unit = {
    outMap.deleteSprites(SpriteFilter.texture(PrefabUtils.MARKER_SPRITE_TEX))
  }

  /**
    * @param s
    * @param lotag
    * @returns true if the given sprite is a marker sprite that also has a matching lotag
    */
  def isMarker(s: Sprite, lotag: Int): Boolean = {
    //s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == lotag
    MapBuilder.isMarkerSprite(s, lotag)
  }

  // TODO - add optional hitag param
  def isAnchor(s: Sprite): Boolean = MapBuilder.isMarkerSprite(s, PrefabUtils.MarkerSpriteLoTags.ANCHOR)


  //
  // Connector Linking Features
  //
  def linkAllWater(psg1: PastedSectorGroup, psg2: PastedSectorGroup): Unit = {
    //
    // TODO - for now this assumes that all water connectors in both sector groups are part of the same
    // connection (and dont go to some third sector group)

    def sortKey(p: PointXYZ): Long = {
      val x = (p.x + 65535L) << 4
      //val y = (p.y + 65535L) << 2  // removing y: concerned that has collisions will put them out of order
      val z = p.z + 65535L
      x + z
    }

    def getWaterConns(psg: PastedSectorGroup): Seq[TeleportConnector] = {
      val conns = psg.findConnectorsByType(ConnectorType.TELEPORTER).asScala.map(_.asInstanceOf[TeleportConnector])
      val waterConns = conns.filter(_.isWater).map(w => (w, w.getSELocation(psg))).sortBy(t => sortKey(t._2))
      waterConns.unzip._1
    }

    val waterConns1 = getWaterConns(psg1)
    val waterConns2 = getWaterConns(psg2)
    if(waterConns1.size != waterConns2.size) throw new SpriteLogicException()
    // TODO - we could also check the relative distances bewteen all of them

    waterConns1.zip(waterConns2).foreach { case (c1: TeleportConnector, c2: TeleportConnector) =>
        TeleportConnector.linkTeleporters(c1, psg1, c2, psg2, nextUniqueHiTag())
    }
  }

}
