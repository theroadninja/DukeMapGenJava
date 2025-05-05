package trn.prefab.gatekeytreewalk

import duchy.sg.SgPaletteScala
import trn.prefab.{SpriteLogicException, PrefabPalette, GameConfig, Marker}
import trn.{ScalaMapLoader, Map => DMap}

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

    // TODO after generating the plan, run BFS/DFS to make sure all nodes are connected.

    // TODO this same algo should be able to do both grids AND hex grids
    // because a logical grid maps onto both
    // TODO also do bricks!

    // NOTE: the tiling experiments in TilingMain do Doorways by adding adapters to the Room groups to create
    // connectors that happend to line up (e.g. Room1 and Room2 are connected by adding Hallway1 to Room1, and
    // Hallway2 to Room2, such that Hallway1 and 2 have redwall conns that line up with each other.

    // TODO use special sector groups just to establish the sizes?


    // DESIGN NOTES:
    // - edge conn ids in rooms numbered powers of two: E=1, S=2, W=4, N=8
    //      - hex: E=1, SE=2,
    // - edge conn ids in doorway groups bitwise OR of all edges they can match
    //      - e.g. 5 means E+W
    // - for hex, the two sets of diagonals (2-5 vs 3-6) require different shapes; cant match by rotating
    //
    // doorways:
    //  - conn ids: which edges they can match
    //  - sector group hint sprite: which tileset (see SectorGroupHints class)
    //
    // gaps for doorways:
    // - tiles with 90 degree edges can just create gaps by "shrinking" from boundary boxes
    // - the hex tiles cannot - will have 2 different gap sizes.  So the gap logic needs to be programmed
    //    into the "tiling" layout math.
    // - these "tiling" systems are just grid-based layouts?
    //
    // anchors:
    // - will still require all room groups to use anchors?
    //    - it is simple and allows for verification of all the shapes:
    //         - distance from anchor to redwall conn (though need to do each tileset differently)

    // TODO dont forget to randomly rotate rooms whenver the files make it possible!
    // (will need to reassign the edge conn ids)

    // TODO if we could parse ASCII art into maps, could use that to make maps for unit tests!

    println("hi")
  }


}
