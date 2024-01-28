package duchy.experiments.render.maze.stonetunnels

import trn.duke.experiments.{WallPrefab, SectorPrefab}
import trn.Sector

object StoneConstants {

  // //dark shade for most textures
  val SHADE = (14).toShort

  //
  // UPPER LEVEL
  //
  val UPPER_WALL_TEX = 781
  val UPPER_CEILING = 742
  val UPPER_FLOOR = 782

  /** z coord, not texture */
  val UPPER_FLOORZ: Int = Sector.DEFAULT_FLOOR_Z

  val UPPER_WALL: WallPrefab = new WallPrefab(StoneConstants.UPPER_WALL_TEX).setXRepeat(16).setYRepeat(8).setShade(StoneConstants.SHADE.toShort)

  val UPPER_SECTOR: SectorPrefab = new SectorPrefab(UPPER_FLOOR, UPPER_CEILING).setFloorShade(SHADE.toShort).setCeilingShade(SHADE.toShort)

  ////
  // LOWER LEVEL// LOWER LEVEL
  ////

  val LOWER_FLOOR = 801

}
