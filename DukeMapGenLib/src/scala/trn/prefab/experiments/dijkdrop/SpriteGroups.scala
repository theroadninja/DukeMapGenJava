package trn.prefab.experiments.dijkdrop

import trn.prefab.{Enemy, Item}

object SpriteGroups {

  //
  // Items
  //

  // no handgun, pipe bombs, or trip mines.  To use:  marker w/ lotag 23, hitag 16
  val StandardAmmo = Seq(Item.ChaingunAmmo, Item.ShotgunAmmo, Item.FreezeAmmo, Item.DevastatorAmmo, Item.RpgAmmo, Item.ShrinkRayAmmo)
  val STANDARD_AMMO = 16

  // ammo for the more basic weapons
  val BasicAmmo = Seq(Item.ChaingunAmmo, Item.ShotgunAmmo, Item.HandgunAmmo)
  val BASIC_AMMO = 17

  val BasicGuns = Seq(Item.Chaingun, Item.Shotgun)
  val BASIC_GUNS = 18

  //
  // Enemies
  //

  // val FOOT_SOLDIERS = 16
  // val FootSoldiers = Seq(Enemy.LizTroop, Enemy.PigCop, Enemy.Enforcer)

  // val SPACE_FOOT_SOLDIERS = 17
  // val SpaceFootSoldiers = Seq(Enemy.LizTroop, Enemy.Enforcer)

  // val OCTABRAINS = 1820
  // val Octabrains = Seq(Enemy.OctaBrain, Enemy.OctaBrain)
}
