package trn.prefab.experiments.hyperloop

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

}
