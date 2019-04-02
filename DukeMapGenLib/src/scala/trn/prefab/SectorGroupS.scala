package trn.prefab

import trn.duke.TextureList
import trn.{DukeConstants, Sprite, Wall, Map => DMap}

import trn.MapImplicits._

class SectorGroupS(val map: DMap, val sectorGroupId: Int) {

  def copy(): SectorGroup = {
    new SectorGroup(map.copy, this.sectorGroupId);
  }
  /**
    * @return a copy of this sector group, flipped about the X axis
    */
  def flippedX(x: Int): SectorGroupS = {
    val sg = this.copy.asInstanceOf[SectorGroupS]

    sg.wallSeq.foreach{ w =>
      //w.x =
    }
    if(1==1) throw new RuntimeException("not implemented yet")
    sg
  }

  //mapprotected def getMap(): DMap = map

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

  // TODO - should this be here?  it is specific to my sprite logic prefab stuff...
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

  // def sprites: Seq[Sprite] = {  // TODO replace this with the one in map implicits
  //   val list = new collection.mutable.ArrayBuffer[Sprite](map.getSpriteCount)
  //   for(i <- 0 until map.getSpriteCount){
  //     list += map.getSprite(i)
  //   }
  //   list
  // }

  def hasPlayerStart: Boolean = hasMarker(PrefabUtils.MarkerSpriteLoTags.PLAYER_START)

  def hasEndGame: Boolean = containsSprite{ s =>
    s.getTexture == DukeConstants.TEXTURES.NUKE_BUTTON && s.getLotag == DukeConstants.LOTAGS.NUKE_BUTTON_END_LEVEL
  }

}
