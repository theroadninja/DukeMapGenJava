package trn

object BuildConstants {
  /** Height in z units of a single PGUP / PGDOWN action in the build editor */
  val ZStepHeight = 1024

  /** Max number of PgUp / PgDown z-steps of height difference that player can walk over */
  val MaxStepsWalk = 4

  /**
    * Max number of PgUp / PgDown z-steps of height difference that player can jump over.
    * This is expressed in steps and not z-height because I measured with flat floors, and sloped
    * floors might lead to a different result (there might be a z diff value between 20 and 21 steps
    * that the player can still jump over).
    */
  val MaxStepsJump = 20
}
