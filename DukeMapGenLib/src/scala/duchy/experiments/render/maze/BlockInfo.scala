package duchy.experiments.render.maze

import trn.Sector


/**
  * Created to replace trn.duke.experiments.LegacyGrid.SimpleTileset
  */
case class BlockTileset(wallTex: Int, floorTex: Int, ceilTex: Int) {

  def applyToCeilAndFloor(s: Sector): Unit = {
    s.setCeilingTexture(ceilTex)
    s.setFloorTexture(floorTex)
  }
}

/**
  * Created to replace trn.duke.experiments.LegacyGrid.BlockInfo
  */
case class BlockInfo(floorz: Int, tileset: BlockTileset) {
  def applyToSector(s: Sector): Unit = {
    tileset.applyToCeilAndFloor(s)
    s.setFloorZ(floorz)
  }
}
