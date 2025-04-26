package trn.prefab.gatekeytreewalk

/**
  * Algo that uses random walks to draw tile-based paths on a gride.
  *
  * This is the evolution of: SquareTileMain, MoonBase2, MoonBase3
  *
  * The algorithm works by creating a logical plan and pasting tiles to match:
  *
  *  K1 - 1 - S
  *  |    |
  *  G2   G1 - K2
  *  |
  *  2  - E
  *
  *
  * Plans:
  * - tiles will have 1 to 4 conns and fixed positions, always the same size & locations (no stretching)
  * - spaces left between tiles to insert hallways/doors
  * - z-heights are not part of the plan:  the algo will adjust z-heigh as it pastes the tiles
  *      - might not do loops in this version
  *      - if we do loops, the 2>1 or background tiles are responsible for figuring out z-height problems
  * - all redwall conns in tiles AND hallways/doors can use Marker16 to specify a "tileset" which will be
  *   used for best-effort tileset matching (this is how you get a hallway floor text to match the room's)
  * - rooms have zones 0,1,2,3
  *
  * Tile Types:
  *  S - start
  *  E - end
  *  # - normal room of zone # in 0,1,2,3
  *  K - key
  *  G - gate
  *  1<2 - one-way
  *  X   - background/view-only
  *
  *
  */
object GateKeyTreeWalk1 {

  def main(args: Array[String]): Unit = {
    println("hi")
  }

}
