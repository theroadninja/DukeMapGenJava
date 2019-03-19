package trn

import trn.{Map => DMap}

/** extending the Map class with scala stuff */
object MapImplicits {
  class MapExtended(map: DMap) {

    def allSprites: Seq[Sprite] = {
      val list = new collection.mutable.ArrayBuffer[Sprite](map.getSpriteCount)
      for(i <- 0 until map.getSpriteCount){
        list += map.getSprite(i)
      }
      list
    }

  }
  implicit def mapExtended(map: DMap) = new MapExtended(map)
}

