package trn.prefab.gatekeytreewalk

import duchy.sg.SgPaletteScala
import trn.{Map => DMap}
import trn.ScalaMapLoader
import trn.prefab.{BoundingBox, GameConfig, SectorGroup, RedwallConnector, Heading, SpriteLogicException, Marker}


case class RectangleTilingDimensions(width: Int, height: Int, gapWidth: Int)

case class GateKeyMapInput(
  palette: SgPaletteScala,
) {

}


object GakeKeyInput {
  /**
    * Reads the map into sector groups and also enforces the higher level / second pass
    * of validation which is specific to this algorithm.
    */
  def readInputMap(cfg: GameConfig, map: DMap): GateKeyMapInput = {

    val palette = ScalaMapLoader.paletteFromMap(cfg, map).scalaObj

    // SG 1 must exist (we read all of the sizes from it)
    val sg1 = palette.numberedGroups.get(1).getOrElse {
      throw new SpriteLogicException("There is not SectorGroup with id=1.  This must exist, to establishing tiling and sizes")
    }

    // identify which tiling
    val edgeConns = sg1.allRedwallConnectors.filter(c => c.getConnectorId > 0 && !c.isLinked(map))
    edgeConns.size match {
      case 4 => {
        // TODO how to we identify brick?  pythagorean?
        readRectangleTilingParams(palette, sg1, edgeConns)

      }
      // TODO case 6 => _
      case count: Int => {
        throw new SpriteLogicException(s"Tile shape with ${count} edge conns not supported yet.")
      }
    }


    palette.numberedGroups.foreach { case (i: Int, sg) =>

      // Every sg must have an anchor
      val anchorCount = sg.scanAnchors().size
      if (anchorCount != 1) {
        val idMarker = sg.findFirstMarker(Marker.Lotags.GROUP_ID).get
        throw new SpriteLogicException(s"SectorGroup ${i} must have exactly 1 anchor, has ${anchorCount}", idMarker)
      }
    }


    GateKeyMapInput(palette)
  }

  /**
    * the params for rectangle tiling come from the dimensions of SG1.
    */
  def readRectangleTilingParams(
    palette: SgPaletteScala,
    sg1: SectorGroup,
    edgeConns: Seq[RedwallConnector],
  ): RectangleTilingDimensions = {
    edgeConns.find(!_.isAxisAligned).foreach { conn =>
      throw new SpriteLogicException(s"Edge conn ${conn.getConnectorId} in SG ${sg1.getGroupId} is not axis-aligned.", conn)
    }
    if (edgeConns.map(_.getConnectorId).toSet != Set(1, 2, 4, 8)) {
      throw new SpriteLogicException(s"Edge conns in SG ${sg1.getGroupId} have the wrong ids.  Must be 1,2,4,8")
    }

    if (!edgeConns(1).isEast) {
      throw new SpriteLogicException(s"Edge conn 1 must be facing east", edgeConns(1))
    }
    if (!edgeConns(2).isSouth) {
      throw new SpriteLogicException(s"Edge conn 2 must be facing south", edgeConns(2))
    }
    if (!edgeConns(4).isEast) {
      throw new SpriteLogicException(s"Edge conn 4 must be facing west", edgeConns(4))
    }
    if (!edgeConns(8).isEast) {
      throw new SpriteLogicException(s"Edge conn 8 must be facing north", edgeConns(8))
    }

    // the edge conns must have exactly one wall (this is "tileset 0")
    // and they must all be the same size
    val edgeConnLength = edgeConns(1).totalManhattanLength()
    Seq(1, 2, 4, 8).foreach { connId =>
      val ec = edgeConns(connId)
      if (ec.getWallCount != 1) {
        throw new SpriteLogicException(s"The edge conns in Sector Group 1 must have exactly one wall (this is only a requirement for SG 1)")
      }
      if(ec.totalManhattanLength() != edgeConnLength){
        throw new SpriteLogicException(s"Edge conn ${connId} has wrong length: ${ec.totalManhattanLength()} != ${edgeConnLength}")
      }
    }

    val width = edgeConns(1).getLocationXY.x - edgeConns(4).getLocationXY.x
    require(width > 0)
    val height = edgeConns(2).getLocationXY.y - edgeConns(8).getLocationXY.y
    require(height > 0)

    // make sure conns aren't inside a concave area
    val bb = BoundingBox(edgeConns.map(_.getLocationXY))
    if (bb.width != width || bb.height != height) {
      throw new SpriteLogicException("The edge conns in sector group 1 cannot be concave (bounding box does not match calculated widths")
    }

    // SG 2 must be a door group.
    // must have 2 axis-aligned conns on opposite sides, hitag 15
    val sg2: SectorGroup = palette.getSG(2)
    val doorEdgeConns = sg2.allRedwallConnectors.filter(c => c.isAxisAligned && c.getConnectorId == 15)
    if(sg2.allRedwallConnectors.size != doorEdgeConns.size){
      throw new SpriteLogicException("SG2 is a 'door' group and must have exactly 2 conns with id=15")
    }

    val dc1 :: dc2 :: _ = doorEdgeConns
    if(dc1.getHeading != Heading.opposite(dc2.getHeading)){
      throw new SpriteLogicException(("SG2 must have connectors facing opposite directions"))
    }

    val gapWidth = if(dc1.isEast || dc1.isWest){
      math.abs(dc1.getLocationXY.x - dc2.getLocationXY.x)
    }else{
      math.abs(dc1.getLocationXY.y - dc2.getLocationXY.y)
    }


    // so we have width, height, gapWidth
    RectangleTilingDimensions(width, height, gapWidth)
  }







}
