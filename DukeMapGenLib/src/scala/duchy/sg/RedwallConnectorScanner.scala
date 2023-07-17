package duchy.sg

import duchy.vector.{Line2D, VectorMath}
import trn.{PointXY, MapUtil, WallView, MapView, Sprite}

/**
  * Meant to replace ALL of the redwall connector scanning I wrote before.
  *
  * Idea:
  * - start w/ the primary marker-20s and follow contiguous walls in the same sector (dont cross between white
  * and red walls)
  * - detect sprite pointed at the same wall group; sprites with no wall group
  * - add an isLoop()?   (loops will need some kind of arbitrary start and end, or need to fix all of the code to
  *   work without those two things)
  * - parse multisector children
  * - ensure no wall overlap
  * - match children to parents
  */
object RedwallConnectorScanner {

  def intersectSpriteWall(sprite: Sprite, wallView: WallView): Option[PointXY] = {
    // TODO add implicits for asSprite(), etc
    VectorMath.intersection(Line2D.spriteRay(sprite), Line2D.wallSegment(wallView)).filterNot { point =>
      // ignore if it intersects the end of the segment, because it should intersect the beginning of the next segment
      point == wallView.p2
    }
  }

  /**
    * Find the wall of the sector that intersects the sprites ray.
    *
    * @param map
    * @param sprite
    * @returns the id of the nearest wall that the sprite is pointing at
    */
  def findSpriteTargetWall(map: MapView, sprite: Sprite): Int = {
    val intersections = map.getAllSectorWallIdsBySectorId(sprite.getSectorId).flatMap { wallId =>
      intersectSpriteWall(sprite, map.getWallView(wallId)).map { point => (wallId, point)}
    }
    if(intersections.isEmpty){
      throw new RuntimeException("sprite ray does not intersect any walls in the sector")
    }
    val (wallId, _) = intersections.minBy{
      case (_, intersection) => sprite.getLocation.asXY.manhattanDistanceTo(intersection)
    }
    wallId
  }

}
