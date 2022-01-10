package trn.prefab

import java.util

import trn.{HardcodedConfig, Sprite, Wall, WallView}
import trn.duke.{Lotags, PaletteList, TextureList}
import trn.render.Texture

import scala.collection.JavaConverters._
import scala.io.Source

/**
  * Contains information about all of the available textures.
  *
  * TODO Originally I thought a single `GameConfig` object could encapsulate all the differences between different Build
  * games, however I didn't realize how different they were (Shadow Warrior only has a single SE with many, many tags
  * on it...).  So instead I think Textures, at least, should be a separate concern.
  */
trait TexturePack {

  def tex(picnum: Int): Texture

  def textureWidth(texture: Int): Int

  def textureHeight(texture: Int): Int
}

/**
  * TODO there is some stuff about sprite angles in GameLogic
  *
  * This is intended to represent game-specific information, as an input, so avoid hard coding things.
  * For example, the fact that a sprite with texture 142 ends a level is specific to Duke3d and probably
  * not relevant to other build games.
  */
trait GameConfig extends TexturePack { // TODO: GameConfig should contain a TexturePack, not BE a TexturePack

  /**
    * Looks up the width of a texture.  This is necessary because setting the scaling/alignment of textures requires
    * knowing their width, which is information not present in the map file.
    *
    * @param texture the texture id (picnum) of a texture
    * @return the width of the texture, in pixels.
    */
  override def textureWidth(texture: Int): Int

  /**
    * Look up the height of a texture, which is useful when trying to align textures vertically.
    *
    * TODO: i added all this height shit when I wrote the code to align textures vertically (on the z axis) however
    *     i later realized that the max ypan is always 256 and texture height doesnt matter!  can probably remove this
    *
    * @param texture the texture id (picnum) of a texture
    * @return the height of the texture, in pixels
    */
  def textureHeight(texture: Int): Int

  override def tex(picnum: Int): Texture = Texture(picnum, textureWidth(picnum))

  /**
    * Method for reading all of the unique hi or lo tag values from a sprite, which is any tag used to link sprites
    * together, e.g. the value used to link activators and switches.
    *
    * This WILL change hitags of 0 for sprites that are expected to have a unique high tag.
    *
    * Note: this is also going to return 32767 for the nuke button
    *
    * @param sprite the sprite to examine
    * @return the "uniqe hitag values" read off this sprite
    */
  def uniqueTags(sprite: Sprite): Seq[Int]

  /**
    * Subset of  tags returned by uniqueTags(); only the ones that are related to each other and must maintain their
    * relationship with each other, for example the multi switch always involves 4 sequential values, which must
    * remain in sequence after mapping.
    *
    * NOTE: this will return ALL possible tags, not just the ones used.  For example if you use a MultiSwitch starting
    * at tag 1024, but only implement doors for 1024 and 1026, this function will still return (1024, 1025, 1026, 1027)
    * anyway.
    *
    */
  def groupedUniqueTags(sprite: Sprite): Seq[Int]


  /**
    * Unlike the overload for sprites, this WILL NOT alter walls (with door texture) if they have a hitag of zero.
    * Unlike SE and other sprites, door walls are not expected to have a hitag, so this code assumes a hitag of 0
    * means it shouldn't have one.
    *
    * @param wall
    * @return
    */
  def uniqueTags(wall: Wall): Seq[Int]

  /**
    * Modifies the sprite in place, changing its unique tag according to `idMap`.  The unique tag could be a hi or
    * lo tag, depending on the sprites texture and tag values.
    *
    * Throws an exception if the given id map does not contain a mapping for the tag value.
    *
    * @param sprite the sprite to modify
    * @param idMap a map of (current tag => new tag)
    */
  def updateUniqueTagInPlace(sprite: Sprite, idMap: Map[Int, Int]): Unit

  def updateUniqueTagInPlace(wall: Wall, idMap: Map[Int, Int]): Unit

  /** @returns true if the given texture/picnum represents a keycard */
  def isKeycard(tex: Int): Boolean

  /** @returns true if the given texture/picnum represents a lock ("accessswitch") */
  def isKeycardLock(tex: Int): Boolean

  def ST: SectorTags

  def visibleForceField: Texture
  def invisibleForceField: Texture
}

// I'm thinking about using this to replace trn.duke.Lotags
case class SectorTags(
  ceilingDoor: Int,
  liftDown: Int,
  liftUp: Int,
  elevatorDown: Int,
  elevatorUp: Int
) {
  def elevatorFor(travelDown: Boolean, withCeiling: Boolean): Int = (travelDown, withCeiling) match {
    case (true, true) => elevatorDown
    case (true, false) => liftDown
    case (false, true) => elevatorUp
    case (false, false) => liftUp
  }
}


object DukeConfig {

  // TODO get rid of hardcoding
  def load(texWidthsFilePath: String, texHeightsFilePath: String = HardcodedConfig.getAtomicHeightsFile): GameConfig = {
    val texWidths = Source.fromFile(texWidthsFilePath).getLines.map(_.trim).filterNot(_.startsWith("#")).map{ line =>
      val fields = line.split("=")
      fields(0).toInt -> fields(1).toInt
    }.toMap
    val texHeights = Source.fromFile(texHeightsFilePath).getLines.map(_.trim).filterNot(_.startsWith("#")).map{ line =>
      val fields = line.split("=")
      fields(0).toInt -> fields(1).toInt
    }.toMap
    new DukeConfig(texWidths, texHeights)
  }

  /** @deprecated - the GameConfig should be loaded propertly; only making this so I dont have to rewrite so much */
  def loadHardCodedVersion(): GameConfig = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile, HardcodedConfig.getAtomicHeightsFile)

  /** this is for unit tests only */
  lazy val empty: GameConfig = new DukeConfig(Map.empty, Map.empty) // TODO - make package private?

  /** Lotags of SE sprites with unique hitags */
  private[prefab] val UniqueHiSE = Set(0, 1, 3, 6, 7, 8, 9, 12, 13, 14, 15, 17, 19, 21, 22, 24, 30) // TODO consider using trn.duke.Lotags

  // TODO - for SE6 (subway engine) - is the sector hitag of car sectors also unique?
  // TODO - does SE 11 (rotate sector door) use unique hitags to link doors?
  // TODO - SE 22 hitag matches hitag of sector with lotag 29...

  // TODO - for SE 30 we also need to reserve hitag+1 and hitag+2 ( done - check using unit test )

  // TODO - double check that SE36 (spawn shot) really dont use a unique tag (is the masterswitch in the same sector?)

  /** Texture/picnums of sprites that can have unique lotags */
  private[prefab] lazy val TexWithLotags = Set(TextureList.ACTIVATOR, TextureList.TOUCHPLATE,
    TextureList.ACTIVATOR_LOCKED, TextureList.MASTERSWITCH, TextureList.RESPAWN, TextureList.CAMERA1)

  /** sprites with these textures have unique tags (if non zero) */
  private[prefab] lazy val TexWithHitags = Set(TextureList.VIEWSCREEN, TextureList.VIEWSCREEN_SPACE)

  private[prefab] lazy val Switches: Seq[Int] = TextureList.Switches.ALL.asScala.map(_.intValue)

  /** tiles that can trigger activators.  NOT the same as "tiles that look like doors" */
  private[prefab] lazy val DoorTiles: Seq[Int] = TextureList.DOORS.ALL.asScala.map(_.intValue)

  private[prefab] lazy val ForceFields: Seq[Int] = TextureList.ForceFields.ALL.asScala.map(_.intValue)

  private[prefab] lazy val Fems: Seq[Int] = TextureList.FEM.ALL.asScala.map(_.intValue)

  val KeyColors: Seq[Int] = Seq(PaletteList.KEYCARD_BLUE, PaletteList.KEYCARD_RED, PaletteList.KEYCARD_YELLOW)

  def ST: SectorTags = SectorTags(
    ceilingDoor = Lotags.ST.CEILING_DOOR,
    liftDown = Lotags.ST.LIFT_DOWN,
    liftUp = Lotags.ST.LIFT_UP,

    /** starts high, and travels DOWN, with ceiling */
    elevatorDown = Lotags.ST.ELEVATOR_DOWN,

    /** starts low, and travels UP, with ceiling */
    elevatorUp = Lotags.ST.ELEVATOR_UP
  )
}

/** TODO - move this somewhere else? */
class DukeConfig(textureWidths: Map[Int, Int], textureHeights: Map[Int, Int]) extends GameConfig {

  override def textureWidth(texture: Int): Int = textureWidths.get(texture).getOrElse(0)

  override def textureHeight(texture: Int): Int = textureHeights.get(texture).getOrElse(0)

  /**
    * @return true if the texture (picnum) is a switch
    */
  def isSwitch(tex: Int): Boolean = DukeConfig.Switches.contains(tex)

  /** @returns true if the given texture/picnum represents a keycard */
  def isKeycard(tex: Int): Boolean = tex == 60

  /** @returns true if the given texture/picnum represents a lock ("accessswitch") */
  def isKeycardLock(tex: Int): Boolean = {
    Seq(TextureList.Switches.ACCESS_SWITCH, TextureList.Switches.ACCESS_SWITCH_2).contains(tex)
  }

  def isCrack(tex: Int): Boolean = {
    Seq(TextureList.CRACK1, TextureList.CRACK2, TextureList.CRACK3, TextureList.CRACK4).contains(tex)
  }

  def isDoor(tex: Int): Boolean = DukeConfig.DoorTiles.contains(tex)

  def isForceField(tex: Int): Boolean = DukeConfig.ForceFields.contains(tex)

  def isFem(tex: Int): Boolean = DukeConfig.Fems.contains(tex)

  override def groupedUniqueTags(sprite: Sprite): Seq[Int] = {
    val tags = uniqueTags(sprite)
    if(tags.size > 1){
      tags
    }else{
      Seq.empty
    }
  }

  private def hasUniqueLotag(tex: Int): Boolean = {
    DukeConfig.TexWithLotags.contains(tex) || isSwitch(tex)
  }

  private def hasUniqueHitag(tex: Int): Boolean = {
    val Explosives = Seq(TextureList.EXPLOSIVE_TRASH.OOZFILTER, TextureList.EXPLOSIVE_TRASH.SEENINE)
    DukeConfig.TexWithHitags.contains(tex) || isCrack(tex) || Explosives.contains(tex) || isFem(tex)
  }

  override def uniqueTags(sprite: Sprite): Seq[Int] = {
    // Decided not to exclude zeros, because I wanted to keep them for multiswitches, and that would create
    // unpredictable, non-local behavior:  simply adding a multi_switch in the same sector group would cause
    // unrelated SE sprites to have their hitags changed.  Instead, this code with _always_ map hitags, even
    // if they are zero (though probably dont want to do this with walls!)

    // TODO - verify we can use locator lotags without interference...

    if(sprite.getTex == TextureList.SE) {
      if (Lotags.SE.TWO_WAY_TRAIN == sprite.getLotag) {
        (sprite.getHiTag to sprite.getHiTag + 2) // must come before normal SE logic
      } else if (DukeConfig.UniqueHiSE.contains(sprite.getLotag) && sprite.getHiTag != 0) {
        Seq(sprite.getHiTag)
      } else {
        Seq.empty
      }
    }else if(sprite.getTex == TextureList.Switches.MULTI_SWITCH){
      (sprite.getLotag to sprite.getLotag + 3) // must come before hasUniqueLotag()
    }else if(hasUniqueLotag(sprite.getTex)){
      Seq(sprite.getLotag)
    }else if(hasUniqueHitag(sprite.getTex)){
      Seq(sprite.getHiTag)
    }else{
      Seq.empty
    }
  }

  def uniqueTags(wall: Wall): Seq[Int] = {
    Seq(wall.getTex, wall.getMaskTex).flatMap { tex =>
      if(isDoor(tex) || isForceField(tex)){
        Seq(wall.getLotag).filterNot(_ == 0)
      }else{
        Seq.empty
      }
    }
  }

  override def updateUniqueTagInPlace(sprite: Sprite, idMap: Map[Int, Int]): Unit = {
    if(sprite.getTex == TextureList.SE && DukeConfig.UniqueHiSE.contains(sprite.getLotag)){
      // There is a bug where where an SE sprite we expect to have a hitag doesnt have one
      // and then there is no hitag entry in the idMap for it
      // NOTE:  SE21 has an OPTIONAL hitag...both zero and nonzero are valid
      if(sprite.getHiTag != 0){ // make sure the old hitag is nonzero
        sprite.setHiTag(idMap(sprite.getHiTag))
      }else{
        println(s"GameConfig.updateUniqueTagInPlace() WARNING:  not setting unique hitag on sprite at ${sprite.getPoint}")
      }
    }else if(hasUniqueLotag(sprite.getTex)){
      // TODO do these need the same check at the hitag stuff above?
      sprite.setLotag(idMap(sprite.getLotag))
    }else if(hasUniqueHitag(sprite.getTex)){
      sprite.setHiTag(idMap(sprite.getHiTag))
    }
  }

  override def updateUniqueTagInPlace(wall: Wall, idMap: Map[Int, Int]): Unit = {
    if((isDoor(wall.getTex) || isForceField(wall.getTex)) && wall.getLotag > 0){
      wall.setLotag(idMap(wall.getLotag))
    }
    if((isDoor(wall.getMaskTex) || isForceField(wall.getMaskTex)) && wall.getLotag > 0){
      wall.setLotag(idMap(wall.getLotag))
    }
  }

  override def ST: SectorTags = DukeConfig.ST
  // override def ST: SectorTags = SectorTags(
  //   elevatorDown = Lotags.ST.ELEVATOR_DOWN,
  //   elevatorUp = Lotags.ST.ELEVATOR_UP
  // )

  override def visibleForceField: Texture = tex(663)
  override def invisibleForceField: Texture = tex(230) // BigForce
}

/**
  * Hardcoded stuff to allow code that depends on GameConfig to run in unit tests.
  *
  * TODO get rid of this if it is unused
  */
object TestGameConfig extends GameConfig {

  override def textureWidth(texture: Int): Int = 128

  override def textureHeight(texture: Int): Int = 128

  override def uniqueTags(sprite: Sprite): Seq[Int] = ???

  override def groupedUniqueTags(sprite: Sprite): Seq[Int] = ???

  override def uniqueTags(wall: Wall): Seq[Int] = ???

  override def updateUniqueTagInPlace(sprite: Sprite, idMap: Map[Int, Int]): Unit = ???

  override def updateUniqueTagInPlace(wall: Wall, idMap: Map[Int, Int]): Unit = ???

  override def isKeycard(tex: Int): Boolean = ???

  override def isKeycardLock(tex: Int): Boolean = ???

  override def ST: SectorTags = DukeConfig.ST

  def visibleForceField: Texture = ???
  def invisibleForceField: Texture = ???
}
