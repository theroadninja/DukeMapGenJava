package trn.prefab

import trn.MapImplicits._
import trn.{MapUtil, PlayerStart, PointXYZ, Sprite, SpriteFilter, Map => DMap}

trait MapBuilder {
  val outMap: DMap

  def pasteSectorGroup(sg: SectorGroup, translate: PointXYZ): PastedSectorGroup = {
    new PastedSectorGroup(outMap, MapUtil.copySectorGroup(sg.map, outMap, 0, translate));
  }

  /** sets the player start of the map to the location of the first player start marker sprite it finds */
  def setAnyPlayerStart(): Unit = {
    val playerStarts = outMap.allSprites.filter(s =>
      s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == PrefabUtils.SpriteLoTags.PLAYER_START
    )
    if(playerStarts.size < 1) {
      throw new SpriteLogicException("cannot set player start - there are no player start markers")
    }
    outMap.setPlayerStart(new PlayerStart(playerStarts(0)))
  }

  /** sets the player start but bitches at you if there is more than one */
  def setPlayerStart(): Unit = {
    val playerStarts = outMap.allSprites.filter(s =>
      s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == PrefabUtils.SpriteLoTags.PLAYER_START
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
    s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == lotag
  }

  // TODO - add optional hitag param
  def isAnchor(s: Sprite): Boolean = isMarker(s, PrefabUtils.SpriteLoTags.ANCHOR)

}
