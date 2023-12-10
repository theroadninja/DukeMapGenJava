package trn.prefab.experiments.dijkdrop

import trn.RandomX
import trn.prefab.{SectorGroup, PrefabPalette, RedwallConnector, GameConfig}

object CustomRooms {

  def stoneVaults(gameCfg: GameConfig, random: RandomX, stonePalette: PrefabPalette): NodeTile2 = {
    val sg: SectorGroup = stonePalette.getSG(2)
    val itemChunk = stonePalette.getSG(3)
    val tunnelChunk = stonePalette.getSG(4)
    val flameWall = stonePalette.getSG(5)
    val torchWall = stonePalette.getSG(6)

    // all the subgroups use redwall conn 100 to connet to the main group
    def get100conn(sg: SectorGroup): RedwallConnector = sg.getRedwallConnector(100)

    val chunks = random.shuffle(
      Seq(itemChunk, tunnelChunk, tunnelChunk, tunnelChunk, tunnelChunk, flameWall, flameWall, torchWall)
    ).toSeq

    var sg2: SectorGroup = sg
    Seq(100, 101, 102, 103, 104, 105, 106, 107).zipWithIndex.foreach { case (connId, i) =>
      val conn = sg2.getRedwallConnector(connId)
      sg2 = sg2.withGroupAttachedAutoRotate(gameCfg, conn, chunks(i))(get100conn)
    }
    val sg3 = PipeRoom.fixTunnelRedwallConnIds(sg2)
    NodeTile2(sg3)
  }
}
