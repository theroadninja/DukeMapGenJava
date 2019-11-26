package trn.prefab

import trn.{IdMap, PointXYZ, Sprite, Map => DMap}
import trn.MapImplicits._
import trn.duke.TextureList

object AutoText {

  def isFontSprite(s: Sprite): Boolean = {
    TextureList.isFontTex(s.getTexture) && s.getLotag > 0
  }

  def getTextSprites(sectorId: Int, sprites: Iterable[Sprite]): Seq[Sprite] = {
    sprites.filter(s => s.getSectorId == sectorId && isFontSprite(s)).toSeq
  }

  private[prefab] def write(text: String, sprites: Set[Sprite]): Unit = {
    if(text == "") return
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
}

/**
  * This is a logical object make of a group of text sprites.
  */
case class AutoText(autoTextId: Int, sectorIds: Set[Int]) {

  def translateIds(idMap: IdMap, delta: PointXYZ): AutoText = {
    new AutoText(autoTextId, sectorIds.map(i => idMap.sector(i).toInt))
  }

  def mergedWith(other: AutoText): AutoText = {
    if(this.autoTextId != other.autoTextId) throw new IllegalArgumentException
    return AutoText(this.autoTextId, this.sectorIds ++ other.sectorIds) // TODO - and then this has the sprite ids
  }

  /**
    *
    * Finds sprites designated as text sprites (a font texture, and lotag > 0) and sets their textures such that they
    * spell out the provided string.
    *
    * The sprites are assumed to be ordered by ascending lotag.
    *
    * The lotags are reset to 0, allowing this method to be called multiple times.
    */
  def appendText(text: String, sg: SectorGroup): Unit = {
    if(text == "") return
    val sprites = sectorIds.flatMap(sectorId => AutoText.getTextSprites(sectorId, sg.allSprites))
    AutoText.write(text, sprites)
  }
}
