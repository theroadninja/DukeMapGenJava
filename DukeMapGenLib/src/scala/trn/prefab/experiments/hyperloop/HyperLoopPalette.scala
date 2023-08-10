package trn.prefab.experiments.hyperloop

import trn.prefab.SectorGroup

trait HyperLoopPalette {

  /** a special sector group used only to measure the size of the ring */
  def coreSizeGroup: SectorGroup

  /** a special sector group used only to measure the size of the ring */
  def innerSizeGroup: SectorGroup

  /** a special sector group used only to measure the size of the ring */
  def midSizeGroup: SectorGroup // must have anchor, and "inner connector" (id 1) defined

}
