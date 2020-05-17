package trn.prefab

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
}

object DukeConfig {
  def load(texWidthsFilePath: String): GameConfig = {
    val texWidths = Source.fromFile(texWidthsFilePath).getLines.map(_.trim).filterNot(_.startsWith("#")).map{ line =>
      val fields = line.split("=")
      fields(0).toInt -> fields(1).toInt
    }.toMap
    new DukeConfig(texWidths)
  }
}

/** TODO - move this somewhere else? */
class DukeConfig(textureWidths: Map[Int, Int]) extends GameConfig {

  override def textureWidth(texture: Int): Int = textureWidths.get(texture).getOrElse(0)
}
