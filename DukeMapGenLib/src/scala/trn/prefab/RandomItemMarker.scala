package trn.prefab

import trn.{Sprite, RandomX}
import trn.duke.TextureList

object RandomItemMarker {

  // TODO start using this older java syntax for consts that I'm using for enums
  /** hitag to indicate item should be chosen according to the "ammo" algo for subway */
  val SUBWAY_AMMO = 1


  def writeTo(random: RandomX, sprite: Sprite): Unit = {
    require(Marker.isMarker(sprite, Marker.Lotags.RANDOM_ITEM))
    val hitag = sprite.getHiTag
    hitag match {
      case SUBWAY_AMMO => writeSubwayAmmo(random, sprite)
    }
  }

  def writeSubwayAmmo(random: RandomX, sprite: Sprite): Unit = {
    require(Marker.isMarker(sprite, Marker.Lotags.RANDOM_ITEM)) // 23

    // TODO - should these items be done in a late pass after we know what weapons?
    val Ammo = Seq(
      TextureList.Items.RPG_AMMO,
      TextureList.Items.FREEZE_AMMO,
      TextureList.Items.HANDGUN_AMMO,
      TextureList.Items.CHAINGUN_AMMO,
      TextureList.Items.DEVSTATOR_AMMO,
      TextureList.Items.PIPE_BOMB_SINGLE, // TextureList.Items.PIPE_BOMB_BOX,
      TextureList.Items.SHOTGUN_AMMO,
      TextureList.Items.SHRINK_RAY_AMMO,
      // TextureList.Items.TRIP_BOMB,
    )
    println(s"setting ammo sprite")
    sprite.setTexture(random.randomElement(Ammo))
    sprite.setLotag(0)
    sprite.setHiTag(0)
  }
}
