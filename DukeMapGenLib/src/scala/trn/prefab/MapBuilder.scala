package trn.prefab

import trn.MapImplicits._
import trn.{PlayerStart, SpriteFilter, Map => DMap}

trait MapBuilder {
  val outMap: DMap

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

}
