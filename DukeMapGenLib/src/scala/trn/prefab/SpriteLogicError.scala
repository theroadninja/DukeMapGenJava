package trn.prefab

import trn.{PointXY, Sprite}
import scala.collection.JavaConverters._

/**
  * Sort of a scala counterpart to the SpriteLogicException.
  *
  * See @MarkerScala
  */
case class SpriteLogicError(
  message: String, locations: Seq[PointXY]
) {

  def throwSelf(): Unit = {
    // XXX see MarkerScala for the validateion rules
    throw new SpriteLogicException(message, locations.asJava)
  }
}

object SpriteLogicError {
  def forSprites(msg: String, sprites: Seq[Sprite]): SpriteLogicError = {
    SpriteLogicError(msg, sprites.map(_.getLocationXY))
  }
}
