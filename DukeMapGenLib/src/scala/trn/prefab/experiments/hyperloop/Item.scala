package trn.prefab.experiments.hyperloop

import trn.duke.{TextureList, PaletteList}

/**
  * Abstract Representation of an item for pickup like key, ammo, weapon, powerup.
  *
  * TODO consider making this more generic to be used among any algorithm
  */
case class Item(tex: Int, pal: Int = 0)

object Item {
  val BlueKey = Item(TextureList.Items.KEY, PaletteList.KEYCARD_BLUE)
  val RedKey = Item(TextureList.Items.KEY, PaletteList.KEYCARD_RED)
  val YellowKey = Item(TextureList.Items.KEY, PaletteList.KEYCARD_YELLOW)
}
