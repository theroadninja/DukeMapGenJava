package trn.prefab

import trn.{Sprite, PointXYZ}
import trn.prefab.Marker.Lotags

import scala.collection.mutable

object MarkerScala {

  def isMarker(sprite: Sprite): Boolean = Marker.isMarker(sprite)

  def isMarker(sprite: Sprite, lotag: Int): Boolean = Marker.isMarker(sprite, lotag)

  /**
    * Scans for logic errors involving marker sprites.
    * Should only be called when we are scanning sector group in from he input
    * map, and not on merged sector groups.
    *
    * @param sprites
    */
  def validateSprites(sprites: Seq[Sprite]): Seq[SpriteLogicError] = {
    val errors = mutable.ArrayBuffer[SpriteLogicError]()

    val anchors = sprites.filter(s => isMarker(s, Lotags.ANCHOR) && s.getHiTag == 0)
    if(anchors.size > 1) {
      val message = "too many anchors with hitag=0"
      errors.append(SpriteLogicError.forSprites(message, anchors))
    }
    val pairedAnchors = sprites.filter(s => isMarker(s, Lotags.ANCHOR) && s.getHiTag == 1)
    if(pairedAnchors.size == 2){
      val a :: b :: _ = pairedAnchors
      if(a.getLocationXY.equals(b.getLocationXY)){
        val message = s"paired anchors share the same XY location"
        errors.append(SpriteLogicError.forSprites(message, pairedAnchors))
      }

    }else if(pairedAnchors.size != 0){
      val message = s"wrong number of paired anchors (hitag=2): ${pairedAnchors.size}"
      errors.append(SpriteLogicError.forSprites(message, pairedAnchors))
    }
    val invalidAnchors = sprites.filter(s => isMarker(s, Lotags.ANCHOR) && s.getHiTag > 1)
    if(invalidAnchors.nonEmpty){
      val message = "detected anchor(s) with invalid hitag"
      errors.append(SpriteLogicError.forSprites(message, invalidAnchors))
    }

    errors
  }

  /**
    * Returns all the anchors it found.  Multiple anchors are possible because sector groups can be merged.
    */
  def scanAnchors(allSprites: Seq[Sprite]): Seq[PointXYZ] = {

    val anchorSprites = allSprites.filter(s => Marker.isMarker(s, Marker.Lotags.ANCHOR))
    val rawAnchors = anchorSprites.filter(_.getHiTag == 0).map(_.getLocation)
    val paired = anchorSprites.filter(_.getHiTag == 2)
    val anchors2: Seq[PointXYZ] = if (paired.size == 2) {
      val a :: b :: _ = paired
      Seq(BoundingBox.centerXYZ(a.getLocation, b.getLocation))
    } else {
      if (paired.size != 0) {
        println(s"WARNING: ignoring paired anchors count=${paired.size}")
      }
      Seq.empty
    }

    rawAnchors ++ anchors2
  }

}