package trn.prefab

import trn.Sprite

/**
  * Like a Marker, except:
  * 1. it is algorithm-specific ?
  * 2. it is best-effort (doesnt cause input to be rejected like a Marker does) ?
  */
case class Decorator(hitag: Int, lotag: Int) {

}

object Decorator {

  val Texture = 310

  object Lotags {

    /**
      * the sector groups with this Decorator are meant to be randomly connected to the "parent" sector group, as
      * a way of generating a random sector group.  Multiple "child" groups should be associate with the same parent,
      * so that they can be randomly chosen.
      *
      * hitag: the sector group id (marker 1) of the parent sector group.
      * other:   there must be a redwall connector in this child group that has an ID that matches one or more connectors
      *          in the "parent" group
      *
      * constraints?
      * - can't put more than one of these in the same sector with a connector (maybe ok in same sector group?)
      */
    val RANDOM_CHILD = 4
  }

  def isDecorator(sprite: Sprite): Boolean = sprite.getTex == 310 && sprite.getPal == 0 && sprite.getStat.isFaceAligned

  def apply(sprite: Sprite): Decorator = Decorator(sprite.getHiTag, sprite.getLotag)

  def fromSprite(sprite: Sprite): Option[Decorator] = if(isDecorator(sprite)) {
    Some(Decorator(sprite))
  } else {
    None
  }

}
