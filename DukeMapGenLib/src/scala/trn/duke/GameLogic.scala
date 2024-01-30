package trn.duke;

import trn.Sprite
import trn.prefab.{Marker};

// TODO this stuff probably needs to move to GameConfig
// TODO this is duke specific
object GameLogic {



  /** sprites whose angles have meaning */
  val SE_SPECIAL_ANGLE: Set[Int] = Set(0, 1, 11, 13, 21, 31, 32)

  /** lotags of marker sprites whose angles have special meaning */
  val MARKER_SPECIAL_ANGLE: Set[Int] = Set(17)

  /**
   * When a sector is rotated or flipped, most of the sprites should be rotated or flipped with it, which really just
   * means that their angle should undergo the same transformation as the sector.  However, the angles of some sprites
   * have a special meaning and do NOT represent a vector in 3D space (e.g. SE sprites where you set the angle "UP"
   * or "DOWN") and those angles should not be altered.
   *
   * See also https://wiki.eduke32.com/wiki/Sector_Effector_Reference_Guide
   *
   * @param s
   * @return true if the sprite should be rotated, false if the sprite should have a special meaning.
   */
  def shouldRotate(s: Sprite): Boolean = if(s.getTex == TextureList.SE && SE_SPECIAL_ANGLE.contains(s.getLotag)) {
    false
  } else if(s.getTex == Marker.MARKER_SPRITE_TEX && MARKER_SPECIAL_ANGLE.contains(s.getLotag)){
    false
  } else if(s.getTex == TextureList.CYCLER) {
    false
  } else {
    true
  }

//      public static boolean shouldRotate(Sprite s){
//        if(s.getTexture() == TextureList.SE){
//            if(SE_SPECIAL_ANGLE.contains(s.getLotag())){
//                return false;
//            }
//        }else if(s.getTexture() == TextureList.CYCLER){
//            // cyclers seems to break when angle is changed
//            return false;
//        }
//
//        return true;
//
//    }

}
