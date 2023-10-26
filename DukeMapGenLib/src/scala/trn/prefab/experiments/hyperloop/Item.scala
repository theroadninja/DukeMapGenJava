package trn.prefab.experiments.hyperloop

import trn.duke.{TextureList, PaletteList}
import trn.duke.TextureList.Items._

/**
  * Abstract Representation of an item for pickup like key, ammo, weapon, powerup.
  *
  * TODO - this was a good idea.  I put the new version in prefab/Item.scala.  Switch to that one
  */
case class Item(tex: Int, pal: Int = 0)

object Item {
  val BlueKey = Item(TextureList.Items.KEY, PaletteList.KEYCARD_BLUE)
  val RedKey = Item(TextureList.Items.KEY, PaletteList.KEYCARD_RED)
  val YellowKey = Item(TextureList.Items.KEY, PaletteList.KEYCARD_YELLOW)

  val Handgun = Item(HANDGUN)
  val Shotgun = Item(SHOTGUN)
  val Chaingun = Item(CHAINGUN)
  val Rpg = Item(RPG)
  val FreezeRay = Item(FREEZE_RAY)
  val ShrinkRay = Item(SHRINK_RAY)
  val PipeBombSingle = Item(PIPE_BOMB_SINGLE)
  val PipeBomb = Item(PIPE_BOMB_BOX)

  /** WARNING:  this is ONLY the ammo pickup.  To place them in the level use tex 2566 */
  val TripBomb = Item(TRIP_BOMB)
  val Devastator = Item(DEVASTATOR)

  // val FREEZE_AMMO = 37
  // val HANDGUN_AMMO = 40
  // val CHAINGUN_AMMO = 41
  // val DEVSTATOR_AMMO = 42
  // val RPG_AMMO = 44
  // val SHRINK_RAY_AMMO = 46
  // val SHOTGUN_AMMO = 49

  val Medkit = Item(TextureList.Items.HEALTH_MEDKIT)
  val Armor = Item(TextureList.Items.ARMOR)
  val Steroids = Item(TextureList.Items.STEROIDS)
  val Scuba = Item(TextureList.Items.SCUBA)
  val Jetpack = Item(TextureList.Items.JETPACK)
  val Nightvision = Item(TextureList.Items.NIGHT_VISION)
  val Boots = Item(TextureList.Items.BOOTS)
  val HoloDuke = Item(TextureList.Items.HOLODUKE)
  val AtomicHealth = Item(TextureList.Items.HEALTH_ATOMIC)
}
