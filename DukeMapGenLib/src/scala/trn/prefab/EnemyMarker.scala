package trn.prefab

import trn.Sprite
import trn.duke.{TextureList, PaletteList}

/**
  * See also the Enemy class in MoonBase2 which also has fields related to jump, crouch, etc
  * @param picnum
  * @param palette
  */
case class Enemy(
  tex: Int,
  palette: Int = 0,
) {
  def writeTo(sprite: Sprite): Unit = {
    sprite.setTexture(tex)
    sprite.setPal(palette)
    sprite.setLotag(0)
    sprite.setHiTag(0)
  }
}

/**
  * 264 = lizard, pigcop
  */
case class EnemyMarker(
  turnIntoRespawn: Boolean,
  locationFuzzing: Boolean,
  enemyList: Set[Enemy],
) {
}

object EnemyMarker {

  /** Bit that controls whether the marker turns into a direct enemy, or just turns
    * into a respawn sprite.
    * off = direct
    * on = respawn
    */
  val DirectVsRespawn = 1

  /**
    * If set, the sprites position will be randomy changed within the sector to appear
    * more random.
    */
  val LocationFuzzing = 2

  /** Might want another flag in the future */
  val Reserved = 4

  val LizardTrooper = 8
  val LizardTrooperCmdr = 16
  val Octabrain = 32
  val Drone = 64
  val AssaultCmdr = 128
  val PigCop = 256
  val Enforcer = 512
  val MiniBattleLord = 1024
  val Reserved2 = 2048 // reserved for tripmine ...
  val Egg = 4096
  val Slime = 8192

  val EnemyOptions: Seq[Int] = Seq(
    LizardTrooper, LizardTrooperCmdr, Octabrain, Drone, AssaultCmdr, PigCop, Enforcer, MiniBattleLord, Egg, Slime
  )

  val FlagToEnemy: Map[Int, Enemy] = Map(
    LizardTrooper -> Enemy(TextureList.Enemies.LIZTROOP),
    LizardTrooperCmdr -> Enemy(TextureList.Enemies.LIZTROOP, PaletteList.BLUE_TO_RED),
    Octabrain -> Enemy(TextureList.Enemies.OCTABRAIN),
    Drone -> Enemy(TextureList.Enemies.DRONE),
    AssaultCmdr -> Enemy(TextureList.Enemies.COMMANDER),
    PigCop -> Enemy(TextureList.Enemies.PIGCOP),
    Enforcer -> Enemy(TextureList.Enemies.LIZMAN),
    MiniBattleLord -> Enemy(TextureList.Enemies.BOSS1, PaletteList.BLUE_TO_RED),
    Egg -> Enemy(TextureList.Enemies.EGG),
    Slime -> Enemy(TextureList.Enemies.GREENSLIME),
  )
  def isSet(value: Int, whichBit: Int): Boolean = (value & whichBit) == whichBit

  def apply(marker: Sprite): EnemyMarker = {
    if (!Marker.isMarker(marker, Marker.Lotags.ENEMY)) {
      throw new IllegalArgumentException("the given sprite is not an enemy marker sprite")
    }

    val hitag = marker.getHiTag
    EnemyMarker(hitag)
  }

  def apply(hitag: Int): EnemyMarker = {
    val enemies = EnemyOptions.filter(e => isSet(hitag, e))
    val allowedEnemies = if(enemies.isEmpty){ EnemyOptions }else{ enemies }
    EnemyMarker(
      turnIntoRespawn = isSet(hitag, DirectVsRespawn),
      locationFuzzing = isSet(hitag, LocationFuzzing),
      enemyList = allowedEnemies.toSet.map(FlagToEnemy),
    )
  }
}

