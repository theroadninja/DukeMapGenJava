package duchy.experiments.render.maze


/**
  * Created to replace trn.duke.experiments.LegacyGrid.SimpleTileset
  */
case class BlockTileset(wallTex: Int, floorTex: Int, ceilTex: Int) {

}

/**
  * Created to replace trn.duke.experiments.LegacyGrid.BlockInfo
  */
case class BlockInfo(floorz: Int, tileset: BlockTileset)
