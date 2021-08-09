package trn.render

import trn.WallView

/**
  * Used for code that creates sectors directly into maps, and represents some wall (and its sector id) that you
  * probably want to join to something.
  *
  * @param wall
  * @param sectorId
  */
case class ResultAnchor(wall: WallView, sectorId: Int)
