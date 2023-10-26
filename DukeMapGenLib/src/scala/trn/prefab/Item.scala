package trn.prefab

import trn.duke.{TextureList, PaletteList}
import trn.prefab.experiments.hyperloop.Item

/**
  * TODO copied from hyperloop/Item.scala -- replace that with this
  */
case class Item(tex: Int, pal: Int = 0)

object Item {
  val BlueKey = Item(TextureList.Items.KEY, PaletteList.KEYCARD_BLUE)
  val RedKey = Item(TextureList.Items.KEY, PaletteList.KEYCARD_RED)
  val YellowKey = Item(TextureList.Items.KEY, PaletteList.KEYCARD_YELLOW)
}
