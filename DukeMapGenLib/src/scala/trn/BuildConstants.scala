package trn

import trn.prefab.BoundingBox

// this info was found by experiment
// TODO - incorporate this info also http://infosuite.duke4.net/index.php?page=references_dimensions
// TODO - should this be merged with GameConfig ?
object BuildConstants {

  // These are duplicated in Map.java, but I don't want to created a reference to it
  val MAX_X = 65536
  val MIN_X: Int = -65536
  val MAX_Y = 65536
  val MIN_Y: Int = -65536

  // if set a hi/lotag to 32768 it displays correctly in the 2D view but as soon as you edit
  // the hi tag is says "-32768"...
  // TODO - see if this is UI thing and build actually supports up to 65535 for this...
  val MaxTagValue = 32767

  val MapBounds = BoundingBox(MIN_X, MIN_Y, MAX_X, MAX_Y)
  val MaxSectors = 1024 // more than this and Build will crash

  /** Height in z units of a single PGUP / PGDOWN action in the build editor */
  val ZStepHeight = 1024

  /** minimum sector height, in PgUp/PgDown steps, that duke can enter without ducking */
  val MinStandingSectorHeightSteps = 11
  val MinStandSectHeight = MinStandingSectorHeightSteps * ZStepHeight

  /** Max number of PgUp / PgDown z-steps of height difference that player can walk over */
  val MaxStepsWalk = 4

  /**
    * Max number of PgUp / PgDown z-steps of height difference that player can jump over.
    * This is expressed in steps and not z-height because I measured with flat floors, and sloped
    * floors might lead to a different result (there might be a z diff value between 20 and 21 steps
    * that the player can still jump over).
    */
  val MaxStepsJump = 20

  /** See XRepeat.md for an explanation.  multiplied by tex coordinates to get tex world coordinates */
  val TexScalingFactorX = 16;



  /** default sector height, in PgUp/PgDown steps */
  val DefaultSectorHeightStep = 16
  val DefaultSectorHeight = DefaultSectorHeightStep * ZStepHeight

  // remember, positize Z points down, for some reason
  val DefaultFloorZ = 8192
  val DefaultCeilZ = -8192


  /**
    * transforms z coordinates into the same scale as xy coordinates.
    *
    * @param z
    * @return
    */
  def ztoxy(z: Int): Int = z >> 4
}
