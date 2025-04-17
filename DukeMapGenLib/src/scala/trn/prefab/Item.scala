package trn.prefab

import trn.duke.{TextureList, PaletteList}
import trn.prefab.experiments.hyperloop.Item
import trn.duke.TextureList.Items._

/**
  * TODO copied from hyperloop/Item.scala -- replace that with this
  */
case class Item(tex: Int, pal: Int = 0) extends SpritePrefab

object Item {
  val KeyColors = Seq(PaletteList.KEYCARD_BLUE, PaletteList.KEYCARD_RED, PaletteList.KEYCARD_YELLOW)

  val BlueKey = Item(TextureList.Items.KEY, PaletteList.KEYCARD_BLUE)
  val RedKey = Item(TextureList.Items.KEY, PaletteList.KEYCARD_RED)
  val YellowKey = Item(TextureList.Items.KEY, PaletteList.KEYCARD_YELLOW)

  // val Handgun = Item(HANDGUN)
  val Shotgun = Item(TextureList.Items.SHOTGUN)
  val Chaingun = Item(TextureList.Items.CHAINGUN)
  val Rpg = Item(RPG)
  val FreezeRay = Item(FREEZE_RAY)
  val ShrinkRay = Item(SHRINK_RAY)
  // val PipeBombSingle = Item(PIPE_BOMB_SINGLE)
  val PipeBomb = Item(TextureList.Items.PIPE_BOMB_BOX)

  // /** WARNING:  this is ONLY the ammo pickup.  To place them in the level use tex 2566 */
  // val TripBomb = Item(TRIP_BOMB)
  val Devastator = Item(DEVASTATOR)

  val FreezeAmmo = Item(TextureList.Items.FREEZE_AMMO)
  val HandgunAmmo = Item(TextureList.Items.HANDGUN_AMMO)
  val ChaingunAmmo = Item(TextureList.Items.CHAINGUN_AMMO)
  val DevastatorAmmo = Item(TextureList.Items.DEVSTATOR_AMMO)
  val RpgAmmo = Item(TextureList.Items.RPG_AMMO)
  val ShrinkRayAmmo = Item(TextureList.Items.SHRINK_RAY_AMMO)
  val ShotgunAmmo = Item(TextureList.Items.SHOTGUN_AMMO)

  val SmallHealth = Item(TextureList.Items.HEALTH_SMALL)
  val MediumHealth = Item(TextureList.Items.HEALTH_MEDIUM)
  val Medkit = Item(TextureList.Items.HEALTH_MEDKIT)
  val Armor = Item(TextureList.Items.ARMOR)
  val Steroids = Item(TextureList.Items.STEROIDS)
  val Scuba = Item(TextureList.Items.SCUBA)
  val Jetpack = Item(TextureList.Items.JETPACK)
  val Nightvision = Item(TextureList.Items.NIGHT_VISION)
  val Boots = Item(TextureList.Items.BOOTS)
  val HoloDuke = Item(TextureList.Items.HOLODUKE)
  val AtomicHealth = Item(TextureList.Items.HEALTH_ATOMIC)


  val Blank = Enemy(355, lotag=29) // 355 is the marker sprite, 29 is the BLANk lotag
}
