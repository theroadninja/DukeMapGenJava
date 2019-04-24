package trn.prefab

import trn.{IdMap, PointXYZ, Sprite, Map => DMap}
import trn.MapImplicits._
import trn.duke.TextureList

object AutoText {

  def isFontSprite(s: Sprite): Boolean = {
    TextureList.isFontTex(s.getTexture) && s.getLotag > 0
  }

  def getTextSpriteIds(sectorId: Int, map: DMap): Seq[Int] = {
    (0 until map.getSpriteCount).filter { spriteId =>
      val sprite = map.getSprite(spriteId)
      sprite.getSectorId == sectorId && isFontSprite(sprite)
    }
  }

  def apply(marker: Sprite, map: DMap): AutoText = {
    // TODO - currently the IdMap doesnt track sprites, so we cant load them here
    if(getTextSpriteIds(marker.getSectorId, map).size < 1){
      throw new SpriteLogicException("AutoText marker (5) without any texture sprites with nonzero lotags")
    }
    new AutoText(PrefabUtils.hitagToId(marker), Set(marker.getSectorId.toInt))
  }
}

/**
  * This is a logical object make of a group of text sprites.
  */
case class AutoText(autoTextId: Int, sectorIds: Set[Int]) {

  // TODO - cant do this yet
  // def translateIds(idmap: IdMap, delta: PointXYZ): AutoText = {
  //   textSpriteIds.map(spriteId => idmap.)
  //   AutoText(connectorId, )
  // }

  def translateIds(idMap: IdMap, delta: PointXYZ): AutoText = {
    new AutoText(autoTextId, sectorIds.map(i => idMap.sector(i).toInt))
  }

  def mergedWith(other: AutoText): AutoText = {
    if(this.autoTextId != other.autoTextId) throw new IllegalArgumentException
    return AutoText(this.autoTextId, this.sectorIds ++ other.sectorIds) // TODO - and then this has the sprite ids
  }

  /**
    * WARNING: side effects.
    *
    * Finds sprites designated as text sprites (a font texture, and lotag > 0) and sets their textures such that they
    * spell out the provided string.
    *
    * The sprites are assumed to be ordered by ascending lotag.
    *
    * The lotags are reset to 0, allowing this method to be called multiple times.
    *
    * @param text
    * @param map
    */
  def appendText(text: String, map: DMap): Unit = {
    if(text == "") return

    val sprites = sectorIds.flatMap(sectorId => AutoText.getTextSpriteIds(sectorId, map)).map(map.getSprite)
    val sorted = sprites.toSeq.sortBy(s => s.getLotag)
    if(sorted.size < text.size){
      val msg = s"Not enough text sprites: string length is ${text.size} but only ${sorted.size} sprites"
      throw new SpriteLogicException(msg)
    }
    text.chars().toArray
    sorted.take(text.size).zip(text.toList).foreach { case(sprite, char) =>
        val font = TextureList.getFont(sprite.getTexture)
        sprite.setTexture(font.textureFor(char.toString))
        sprite.setLotag(0)
    }
  }

  def appendText(text: String, map: ISectorGroup): Unit = appendText(text, map.getMap)


}
