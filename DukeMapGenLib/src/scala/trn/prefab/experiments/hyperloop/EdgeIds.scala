package trn.prefab.experiments.hyperloop


/**
  * Most Sections:
  *    ---------\
  *  /     2     |
  * | <-1     4->|
  *  \     3     |
  *    ---------/
  *
  * Column Set Piece:
  *
  *   __________-----------------
  *  /    2    |    5    |    7 |
  *  | 1     4 | 1     4 | 1    |
  *  \    3    |    6    |    8 |
  *   ----------_________|_______
  */
object EdgeIds {
  /**
    * Connector that faces the center of the circle.
    */
  val InnerEdgeConn = 1

  /**
    * Connector id of the anticlockwise edge.
    * If you are inside the group facing anitclockwise, you are looking at this edge
    */
  val AntiClockwiseEdge = 2

  /**
    * Connector id of the clockwise edge.
    * If you are inside the group facing anitclockwise, you are looking at this edge
    */
  val ClockwiseEdge = 3

  val OuterEdgeConn = 4

  val SetPieceMidAnticlockwise = 5
  val SetPieceMidClockwise = 6
  val SetPieceOuterAnticlockwise = 7
  val SetPieceOuterClockwise = 8

}
