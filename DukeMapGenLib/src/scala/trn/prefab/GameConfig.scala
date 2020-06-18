package trn.prefab

import java.util

import trn.{DukeConstants, HardcodedConfig, Sprite, Wall, WallView}
import trn.duke.TextureList

import scala.collection.JavaConverters._
import scala.io.Source

/**
  * This is intended to represent game-specific information, as an input, so avoid hard coding things.
  * For example, the fact that a sprite with texture 142 ends a level is specific to Duke3d and probably
  * not relevant to other build games.
  */
trait GameConfig {

  /**
    * Looks up the width of a texture.  This is necessary because setting the scaling/alignment of textures requires
    * knowing their width, which is information not present in the map file.
    *
    * @param texture the texture id (picnum) of a texture
    * @return the width of the texture, in pixels.
    */
  def textureWidth(texture: Int): Int

  /**
    * Method for reading all of the unique hi or lo tag values from a sprite, which is any tag used to link sprites
    * together, e.g. the value used to link activators and switches.
    *
    * Note: this is also going to return 32767 for the nuke button
    *
    * @param sprite the sprite to examine
    * @return the "uniqe hitag values" read off this sprite
    */
  def uniqueTags(sprite: Sprite): Seq[Int]


  def uniqueTags(wall: Wall): Seq[Int]
}

object DukeConfig {
  def load(texWidthsFilePath: String): GameConfig = {
    val texWidths = Source.fromFile(texWidthsFilePath).getLines.map(_.trim).filterNot(_.startsWith("#")).map{ line =>
      val fields = line.split("=")
      fields(0).toInt -> fields(1).toInt
    }.toMap
    new DukeConfig(texWidths)
  }

  /** @deprecated - the GameConfig should be loaded propertly; only making this so I dont have to rewrite so much */
  def loadHardCodedVersion(): GameConfig = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile())

  /** this is for unit tests only */
  private[prefab] def empty: GameConfig = new DukeConfig(Map.empty)

  /** SE sprites with unique hitags */
  private[prefab] val UniqueHiSE = Set(0, 1, 3, 6, 7, 8, 9, 12, 13, 14, 15, 17, 19, 21, 22, 24, 30)

  // TODO - for SE6 (subway engine) - is the sector hitag of car sectors also unique?
  // TODO - does SE 11 (rotate sector door) use unique hitags to link doors?
  // TODO - SE 22 hitag matches hitag of sector with lotag 29...

  // TODO - for SE 30 we also need to reserve hitag+1 and hitag+2 ( done - check using unit test )

  // TODO - double check that SE36 (spawn shot) really dont use a unique tag (is the masterswitch in the same sector?)

  private[prefab] lazy val Switches: Seq[Int] = TextureList.Switches.ALL.asScala.map(_.intValue)

  /** tiles that can trigger activators.  NOT the same as "tiles that look like doors" */
  private[prefab] lazy val DoorTiles: Seq[Int] = TextureList.DOORS.ALL.asScala.map(_.intValue)

  private[prefab] lazy val Fems: Seq[Int] = TextureList.FEM.ALL.asScala.map(_.intValue)
}

/** TODO - move this somewhere else? */
class DukeConfig(textureWidths: Map[Int, Int]) extends GameConfig {

  override def textureWidth(texture: Int): Int = textureWidths.get(texture).getOrElse(0)

  /**
    * @return true if the texture (picnum) is a switch
    */
  def isSwitch(tex: Int): Boolean = DukeConfig.Switches.contains(tex)

  def isCrack(tex: Int): Boolean = {
    Seq(TextureList.CRACK1, TextureList.CRACK2, TextureList.CRACK3, TextureList.CRACK4).contains(tex)
  }

  def isDoor(tex: Int): Boolean = DukeConfig.DoorTiles.contains(tex)

  def isFem(tex: Int): Boolean = DukeConfig.Fems.contains(tex)

  override def uniqueTags(sprite: Sprite): Seq[Int] = {
    if(sprite.getTex == TextureList.SE){
      if(DukeConstants.LOTAGS.TWO_WAY_TRAIN == sprite.getLotag){
        (sprite.getHiTag to sprite.getHiTag + 2)
      }else if(DukeConfig.UniqueHiSE.contains(sprite.getLotag) && sprite.getHiTag != 0){
        Seq(sprite.getHiTag)
      }else{
        Seq.empty
      }
    }else if(sprite.getTex == TextureList.ACTIVATOR){
      Seq(sprite.getLotag) // TODO - all of these need to filter out zeros (maybe do it at the end)
    }else if(sprite.getTex == TextureList.TOUCHPLATE){
      Seq(sprite.getLotag)
    }else if(sprite.getTex == TextureList.ACTIVATOR_LOCKED){
      Seq(sprite.getLotag)
    }else if(sprite.getTex == TextureList.MASTERSWITCH){
      Seq(sprite.getLotag)
    }else if(sprite.getTex == TextureList.RESPAWN){
      Seq(sprite.getLotag)
    }else if(sprite.getTex == TextureList.Switches.ACCESS_SWITCH){
      Seq(sprite.getLotag)
    }else if(sprite.getTex == TextureList.Switches.ACCESS_SWITCH_2) {
      Seq(sprite.getLotag)
    }else if(sprite.getTex == TextureList.Switches.MULTI_SWITCH){
      (sprite.getLotag to sprite.getLotag + 3)
    }else if(isSwitch(sprite.getTex)){ // this needs to be tested AFTER multi switch
      Seq(sprite.getLotag)
    }else if(sprite.getTex == TextureList.VIEWSCREEN || sprite.getTex == TextureList.VIEWSCREEN_SPACE){
      Seq(sprite.getHiTag) // TODO - filter out zero
    }else if(isCrack(sprite.getTex)){
      Seq(sprite.getHiTag)
    }else if(sprite.getTex == TextureList.CAMERA1){
      Seq(sprite.getLotag)
    }else if(isDoor(sprite.getTex)){
      Seq(sprite.getLotag) // NOTE: this is silly because this code looks at sprites, not walls
    }else if(Seq(TextureList.EXPLOSIVE_TRASH.OOZFILTER, TextureList.EXPLOSIVE_TRASH.SEENINE).contains(sprite.getTex)){
      // TODO - figure out if other explosive textures work for that effect
      Seq(sprite.getHiTag)
    }else if(isFem(sprite.getTex)){
      Seq(sprite.getHiTag)
    }else{
      Seq.empty
    }

    // TODO - clean this function up, and get rid of the zeros!

    // TODO - verify we can use locator lotags without interference...

  }

  def uniqueTags(wall: Wall): Seq[Int] = {
    if(isDoor(wall.getTex) && wall.getLotag != 0){
      Seq(wall.getLotag)
    }else{
      Seq.empty
    }
  }
}
