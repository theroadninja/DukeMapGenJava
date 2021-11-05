package trn.bespoke.moonbase2

import trn.AngleUtil
import trn.logic.Tile2d
import trn.math.{RotatesCW, SnapAngle}
import trn.prefab.{GameConfig, Heading, PrefabUtils, SectorGroup}

// SectorGroup decorated with extra info for this algorithm
case class TileSectorGroup(
  id: String, // used to identify which logical room, to prevent unique rooms being used more than once
  tile: Tile2d,
  sg: SectorGroup,
  tags: Set[String],
  oneWayHigherSideHeading: Option[Int]
) extends RotatesCW[TileSectorGroup] {


  // TODO rotate the heading
  override def rotatedCW: TileSectorGroup = TileSectorGroup(id, tile.rotatedCW, sg.rotatedCW, tags, oneWayHigherSideHeading.map(Heading.rotateCW))

  def withKeyLockColor(gameCfg: GameConfig, color: Int): TileSectorGroup = copy(sg=sg.withKeyLockColor(gameCfg, color))

  // TODO plotzone: Int  (0, 1, 2, K, G, ...) ??  (more like PlacedTileSectorGroup)

  /**
    * special tile that is only used for one-way groups.  it has a '2' in the spot where the connection to the higher
    * zoon needs to go
    */
  val oneWayTile: Option[Tile2d] = if(tags.contains(RoomTags.OneWay)){
    // val higherSide = TileSectorGroup.OneWayTagToSide(tags.find(s => s.startsWith("ONEWAY_")).get)
    Some(tile.withSide(oneWayHigherSideHeading.get, TileSpec.SpecialOneWayVal))
  }else{
    None
  }


  def rotateAngleToOneWayTarget(target: Tile2d): SnapAngle = {
    val sgTile = oneWayTile.get
    val angle = sgTile.rotationTo(target)
    require(angle.isDefined, s"sgTile=${sgTile},id=${id} target=${target}")
    angle.get

  }
  def rotatedToOneWayTarget(target: Tile2d): TileSectorGroup = {
    rotateAngleToOneWayTarget(target) * this
  }
}

object TileSectorGroup {

  val OneWayTagToSide: Map[String, Int] = Map(
    "ONEWAY_E" -> Heading.E,
    "ONEWAY_S" -> Heading.S,
    "ONEWAY_W" -> Heading.W,
    "ONEWAY_N" -> Heading.N,
  )

  // /**
  //   *
  //   * @param ang the angle of the one-way hint sprite, in weird duke angle units
  //   * @return
  //   */
  // def oneWayTagForAngle(ang: Int): String = ang match {
  //   case AngleUtil.ANGLE_RIGHT => "ONEWAY_E"
  //   case AngleUtil.ANGLE_DOWN => "ONEWAY_S"
  //   case AngleUtil.ANGLE_LEFT => "ONEWAY_W"
  //   case AngleUtil.ANGLE_UP => "ONEWAY_N"
  //   case _ => throw new Exception(s"invalid angle for Algo Hint Oneway: ${ang}")
  // }
  def oneWayHeadingForAngle(ang: Int): Int = ang match {
    case AngleUtil.ANGLE_RIGHT => Heading.E
    case AngleUtil.ANGLE_DOWN => Heading.S
    case AngleUtil.ANGLE_LEFT => Heading.W
    case AngleUtil.ANGLE_UP => Heading.N
    case _ => throw new Exception(s"invalid angle for Algo Hint Oneway: ${ang}")
  }


  /**
    * Returns true if the tsg is marked as Unique AND a tsg with the same id has already been placed
    * @param alreadyPlaced set of ids of tsgs that have already been placed
    * @param tsg the tsg to check
    * @return
    */
  def uniqueViolation(alreadyPlaced: collection.Set[String], tsg: TileSectorGroup): Boolean = {
    tsg.tags.contains(RoomTags.Unique) && alreadyPlaced.contains(tsg.id)
  }

  def apply(
    id: String, // used to identify which logical room, to prevent unique rooms being used more than once
    tile: Tile2d,
    sg: SectorGroup,
    tags: Set[String]
  ): TileSectorGroup = {

    // val oneway = sg.allSprites.find(s => PrefabUtils.isMarker(s, AlgoHint.OneWay, PrefabUtils.MarkerSpriteLoTags.ALGO_HINT))
    // val h = oneway.map(s => oneWayHeadingForAngle(s.getAngle))

    TileSectorGroup(id, tile, sg, tags, None)

  }

  def oneWay(
    id: String, // used to identify which logical room, to prevent unique rooms being used more than once
    tile: Tile2d,
    sg: SectorGroup,
    tags: Set[String],
    oneWaySpriteAngle: Int
  ): TileSectorGroup = {
    TileSectorGroup(id, tile, sg, tags, Some(oneWayHeadingForAngle(oneWaySpriteAngle)))

  }
}