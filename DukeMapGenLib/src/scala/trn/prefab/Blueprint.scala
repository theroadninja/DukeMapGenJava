package trn.prefab

import trn.PointXY

case class BlueprintWall(p1: PointXY, p2: PointXY) // TODO: add floorZ and  topZ  ?


object BlueprintConnector {
  def apply(wall: BlueprintWall): BlueprintConnector = BlueprintConnector(Seq(wall))
}
case class BlueprintConnector(walls: Seq[BlueprintWall])

/**
  * Blueprint for a sector group that can be used for planning.
  */
case class BlueprintGroup(
  bb: BoundingBox,
  fineBBs: Seq[BoundingBox],
  connectors: Seq[BlueprintConnector]
)

class Blueprint {

}
