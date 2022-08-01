package trn.prefab.experiments.tiling

import trn.RandomX
import trn.prefab.{GameConfig, MapWriter, PastedSectorGroup, SectorGroup}

trait TileFactory {

  def chooseTile(random: RandomX, coord: (Int, Int), tileType: Int, planNode: PlanNode, edges: Seq[Int]): String

  // TODO probaly dont need a separate TileMaker?   Just make the factory create the tile, and it can delegate to other
  //  classes if it wants
  def getTileMaker(gameCfg: GameConfig, name: String, tileType: Int): TileMaker
  // def palette: PrefabPalette

  def getTileMaker(gameCfg: GameConfig, tile: TileNode): TileMaker = getTileMaker(gameCfg, tile.name, tile.shape)

  def makeTile(gameCfg: GameConfig, tile: TileNode): SectorGroup = getTileMaker(gameCfg, tile).makeTile(gameCfg, tile)

  // dont need GameConfig because the writer has it
  def makeEdge(writer: MapWriter, tileA: RenderedTile, edgeA: Int, tileB: RenderedTile, edgeB: Int): Option[PastedSectorGroup] = None

  /**
    * For assigning "special edges", where sector groups get special modifications to fit together
    *
    * These edges are directed, edgeInfo(n1, edge, n2) needs to return the same thing as edgeInfo(n2, edge, n1) if
    * you want the special edges to match (of course, they don't actually have to return the same thing).
    *
    * @param tile the tile to create a special edge for
    * @param edge the edge to make special
    * @param neighboor  the tile on the other side of the edge
    * @return a string, to be used as special edge info, or None to indicate the edge isn't special.
    */
  def edgeInfo(tile: TileNode, edge: TileEdge, neighboor: TileNode): Option[String] = None

}

object TileFactory {

  /**
    * Attaches sg2 onto sg by using the connectors with id `connId`
    *
    * Compare to MapWriter.pasteAndLink(), which works for PastedSectorGroups
    *
    * Both connectors must have the same id.
    * @param sg
    * @param sg2
    * @param connId
    * @param secondConnId optional connId for `sg2` -- if not specified, will use `connId` for both groups
    * @return
    */
  def attachByConnId(gameCfg: GameConfig, sg: SectorGroup, sg2: SectorGroup, connId: Int, secondConnId: Option[Int] = None): SectorGroup = {
    val connId2 = secondConnId.getOrElse(connId)
    sg.withGroupAttached(gameCfg, sg.getRedwallConnectorsById(connId).head, sg2, sg2.getRedwallConnectorsById(connId2).head)
  }

  // TODO get rid of this
  def attachHallway(gameConfig: GameConfig, sg: SectorGroup, attachments: Map[Int, SectorGroup], attachId: Int): SectorGroup = {
    val hallwaySg = attachments(attachId)
    val connId = attachId
    sg.withGroupAttached(gameConfig, sg.getRedwallConnectorsById(connId).head, hallwaySg, hallwaySg.getRedwallConnectorsById(connId).head)
  }
}
