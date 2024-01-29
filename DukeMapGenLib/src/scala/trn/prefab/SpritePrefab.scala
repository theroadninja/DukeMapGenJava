package trn.prefab

import trn.Sprite


/**
  * TODO rename to SpriteBrush
  */
trait SpritePrefab {
  def tex: Int
  def pal: Int
  def lotag: Int = 0
  def hitag: Int = 0

  final def writeToSprite(sprite: Sprite): Unit = {
    sprite.setTexture(tex)
    sprite.setPal(pal)
    sprite.setLotag(lotag)
    sprite.setHiTag(hitag)
  }
}
