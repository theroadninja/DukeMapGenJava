package trn.render

import trn.prefab.SpritePrefab

/**
  * This is intended to be THE sprite brush/prefab.
  * See also SimpleSpritePrefab in trn.prefab.experiments.dijkdrop
  */
case class SimpleSpriteBrush(
  override val tex: Int,
  override val pal: Int = 0,
  override val lotag: Int = 0,
  override val hitag: Int = 0,
) extends SpritePrefab {
}
