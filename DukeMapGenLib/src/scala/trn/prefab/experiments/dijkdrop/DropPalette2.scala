package trn.prefab.experiments.dijkdrop

import trn.{Sprite, RandomX}
import trn.prefab.{SectorGroup, PrefabPalette, Item, Marker, Enemy, GameConfig}

import scala.collection.JavaConverters._
import scala.collection.mutable

case class DropTileSet (
  start: NodeTile2,
  exit: NodeTile2,
  gate: NodeTile2,
  key: NodeTile2,
  normalRooms: Seq[NodeTile2],
)


object DropPalette2 {
  // THEMES (i.e. tunnel color)
  val Blue = 1
  val Red = 2
  val Green = 3
  val White = 4 // moon
  val Gray = 5 // like the gray brick texture
  val Dirt = 6 // classic dirt/canyon walls
  val Wood = 7 // wood
  val Stone = 8

  def isTunnelTile(sg: SectorGroup): Boolean = {
    val bb = sg.boundingBox
    val c1: Int = sg.allRedwallConnectors.size
    val c2 = sg.allUnlinkedRedwallConns.filter { conn =>
      // conn.getWallCount == 4 && conn.totalManhattanLength == 2048 * 4
      conn.getWallCount == 3 || conn.getWallCount == 1
    }
    val totalConnLength: Long = c2.map(_.totalManhattanLength()).sum
    val themeMarker = sg.sprites.find(s => Marker.isMarker(s, Marker.Lotags.ALGO_GENERIC)).size
    // bb.width == 2048 && bb.height == 2048 && c1 == 1 && c2 == 1 && themeMarker == 1
    bb.width == 2048 && bb.height == 2048 && c1 == 2 && c2.size == 2 && totalConnLength == 2048 * 4 && themeMarker == 1
  }
}

class DropPalette2(
  gameCfg: GameConfig,
  random: RandomX,
  palette: PrefabPalette, // the main palette for this map
  stonePalette: PrefabPalette, // <- a source map dedicated to the stone room
  sewerPalette: PrefabPalette, // <- a source map dedicated to the sewer tunnel
) {

  // The Blank tunnel connector, for when there is no edge
  val blank = palette.getSG(99)

  // val tunnelsOld = Seq(100, 101, 102, 103, 104, 105, 106, 107, 108, 109, 110, 111, 112, 113, 114, 115).map(palette.getSG).map(sg => TunnelTile(sg))
  val tunnels: Seq[TunnelTile] = palette.numberedSectorGroupIds.asScala.toSeq.map(i => palette.getSG(i)).filter(DropPalette2.isTunnelTile).map(sg => TunnelTile(sg))

  val floorTunnels = tunnels.filter(_.isFloor).map(tile => tile.tunnelTheme -> tile).toMap
  val ceilingTunnels = tunnels.filter(!_.isFloor).map(tile => tile.tunnelTheme -> tile).toMap

  def isFallConn(sprite: Sprite): Boolean = Marker.isMarker(sprite, Marker.Lotags.FALL_CONNECTOR)

  def getFloorTunnel(connectorId: Int, tunnelTheme: Int): SectorGroup = {
    val tSG = floorTunnels(tunnelTheme).sg
    tSG.withModifiedSprites { sprite =>
      if (isFallConn(sprite)) {
        sprite.setHiTag(connectorId)
      }
    }
  }

  def getCeilingTunnel(connectorId: Int, tunnelTheme: Int): SectorGroup = {
    val tSG = ceilingTunnels(tunnelTheme).sg
    tSG.withModifiedSprites { sprite =>
      if (isFallConn(sprite)) {
        sprite.setHiTag(connectorId)
      }
    }
  }

  /**
    * connects a "blank" tunnel sg to every unlinked "tunnel" redwall conn
    */
  def withEmptyTunnelsBlanked(sg: SectorGroup): SectorGroup = {
    var sg2 = sg
    var break = false
    while (!break) {
      val connOpt = NodePalette.getUnlinkedTunnelConns(sg2).headOption
      if (connOpt.isDefined) {
        sg2 = sg2.withGroupAttachedAutoRotate(gameCfg, connOpt.get, blank) { otherSg =>
          otherSg.allRedwallConnectors.find(_.getWallCount == 1).get
        }
      } else {
        break = true
      }
    }
    sg2
  }

  // ================================ Rooms ================================

  val blueRoom = NodeTile2(palette.getSG(1), DropPalette2.Blue)
  val redRoom = NodeTile2(palette.getSG(2), DropPalette2.Red)
  val greenRoom = NodeTile2(palette.getSG(3), DropPalette2.Green)
  val whiteRoom = NodeTile2(palette.getSG(4), DropPalette2.White)
  val grayRoom = NodeTile2(palette.getSG(5), DropPalette2.Gray)
  val dirtRoom = NodeTile2(palette.getSG(6), DropPalette2.Dirt)
  val woodRoom = NodeTile2(palette.getSG(7), DropPalette2.Wood)

  val bluePentagon = NodeTile2(palette.getSG(8))
  val blueItemRoom = NodeTile2(palette.getSG(9))
    .modified(NodePalette.standardRoomSetup)
    .withEnemies(random, Seq(Enemy.LizTroop, Enemy.LizTroop, Enemy.LizTroopCmdr, Enemy.PigCop, Enemy.Blank))
    .withEnemies(random, Seq(Enemy.OctaBrain, Enemy.OctaBrain, Enemy.OctaBrain, Enemy.Blank), hitag = 1)


  val redGate = NodeTile2(palette.getSG(10))
    .withEnemies(random, Seq(Enemy.LizTroop, Enemy.LizTroop, Enemy.PigCop, Enemy.PigCop, Enemy.Enforcer, Enemy.Enforcer, Enemy.OctaBrain, Enemy.AssaultCmdr))
    .modified(NodePalette.standardRoomSetup)


  val exitRoom = NodeTile2(palette.getSG(11))

  val startRoom = NodeTile2(palette.getSG(12)).modified { sg =>
    val startItems: Seq[Item] = random.shuffle(Seq(Item.Armor, Item.Medkit, Item.Shotgun, Item.Chaingun, Item.PipeBomb, Item.HandgunAmmo, Item.ChaingunAmmo)).toSeq
    Utils.withRandomSprites(sg, 0, Marker.Lotags.RANDOM_ITEM, startItems)
  }

  val castleStairs = NodeTile2(palette.getSG(13)).modified { sg =>
    val enemies = Seq(
      Enemy.LizTroop, Enemy.LizTroop, Enemy.LizTroop,
      Enemy.LizTroopCmdr,
      Enemy.Enforcer,
      Enemy.OctaBrain,
      Enemy.Blank, Enemy.Blank, Enemy.Blank, Enemy.Blank,
    )
    val sg2 = Utils.withRandomSprites(sg, 0, Marker.Lotags.RANDOM_ITEM, Seq(Item.SmallHealth, Item.MediumHealth, Item.MediumHealth, Item.ShotgunAmmo))
    Utils.withRandomSprites(sg2, 0, Marker.Lotags.ENEMY, enemies)
  }.modified(NodePalette.standardRoomSetup)

  val moon3way = NodeTile2(palette.getSG(14)).modified { sg =>
    val enemies = random.shuffle(Seq(Enemy.LizTroop, Enemy.Enforcer, Enemy.Enforcer, Enemy.OctaBrain, Enemy.Blank, Enemy.AssaultCmdr)).toSeq
    Utils.withRandomEnemies(sg, enemies)
  }.modified(NodePalette.standardRoomSetup)

  val bathrooms = NodeTile2(palette.getSG(15)).modified { sg =>
    val sg2 = Utils.withRandomSprites(sg, 1, Marker.Lotags.ENEMY, random.shuffle(Seq(Enemy.LizTroopOnToilet, Enemy.Blank, Enemy.Blank)).toSeq)
    val sg3 = Utils.withRandomSprites(sg2, 0, Marker.Lotags.ENEMY, random.shuffle(Seq(Enemy.LizTroop, Enemy.LizTroopCrouch, Enemy.PigCop, Enemy.Blank)).toSeq)
    Utils.withRandomSprites(sg3, 1, Marker.Lotags.RANDOM_ITEM, random.shuffle(Seq(Item.RpgAmmo, Item.Devastator)).toSeq)
  }.modified(NodePalette.standardRoomSetup)

  val greenCastle = NodeTile2(palette.getSG(16)).modified { sg =>
    val heavies = random.shuffle(Seq(Enemy.AssaultCmdr, Enemy.MiniBattlelord, Enemy.OctaBrain, Enemy.Blank, Enemy.Blank)).toSeq
    val enemies = random.shuffle(Seq(Enemy.LizTroop, Enemy.OctaBrain, Enemy.OctaBrain, Enemy.Enforcer, Enemy.Blank)).toSeq
    val powerups = random.shuffle(Seq(Item.AtomicHealth, Item.Rpg, Item.Devastator, Item.ShrinkRay, Item.FreezeRay, Item.Medkit)).toSeq
    val sg2 = Utils.withRandomSprites(sg, 0, Marker.Lotags.ENEMY, heavies)
    val sg3 = Utils.withRandomSprites(sg2, 1, Marker.Lotags.ENEMY, enemies)
    Utils.withRandomSprites(sg3, 0, Marker.Lotags.RANDOM_ITEM, powerups)
  }.modified(NodePalette.standardRoomSetup)

  val buildingEdge = NodeTile2(palette.getSG(17)).modified(NodePalette.standardRoomSetup)
    .withEnemies(random, Seq(Enemy.LizTroop, Enemy.PigCop))

  val cavern = NodeTile2(palette.getSG(18)).modified(NodePalette.standardRoomSetup)
    .withEnemies(random, Seq(Enemy.LizTroop, Enemy.LizTroop, Enemy.OctaBrain, Enemy.Blank))

  val nukeSymbolCarpet = NodeTile2(palette.getSG(19)).modified(NodePalette.standardRoomSetup)
    .withEnemies(random, Seq(Enemy.LizTroop, Enemy.Enforcer, Enemy.LizTroop, Enemy.Blank))
    .withEnemies(random, Seq(Enemy.OctaBrain, Enemy.AssaultCmdr, Enemy.Blank))

  val parkingGarage = NodeTile2(palette.getSG(20)).modified(NodePalette.standardRoomSetup)
    .withEnemies(random, Seq(Enemy.LizTroop, Enemy.PigCop, Enemy.Enforcer, Enemy.Blank))

  // can have key OR heavy weapon (but only want it in the level once)
  val stoneVaults = NodeTile2(stonePalette.getSG(1)).modified(NodePalette.standardRoomSetup)

  val fountain = NodeTile2(palette.getSG(21)).modified(NodePalette.standardRoomSetup)
    .withRandomItems(random, Seq(Item.Blank, Item.SmallHealth, Item.SmallHealth, Item.SmallHealth, Item.MediumHealth, Item.MediumHealth, Item.MediumHealth, Item.MediumHealth, Item.Rpg))

  val sushi = NodeTile2(palette.getSG(22)).modified(NodePalette.standardRoomSetup)

  val sewer = NodeTile2(PipeRoom.makePipeRoom(gameCfg, random, sewerPalette)).modified(NodePalette.standardRoomSetup)
  require(sewer.tunnelConnIds.size == 4)

  def chooseTiles(): DropTileSet = {

    // convert a room that could hold a key, to one that holds a heavy weapon
    def toPowerUp(random: RandomX, tile: NodeTile2): NodeTile2 = tile.modified { sg =>
      val item = random.randomElement(Seq(Item.Rpg, Item.Devastator, Item.AtomicHealth))
      sg.withItem2(item)
    }

    // these rooms MUST have an Item (lotag=9) Marker
    val keyOrNormal = Seq(
      blueItemRoom,
      stoneVaults
    )
    val keyRoom::others = random.shuffle(keyOrNormal)

    val normalRooms = Seq(
      // nodepal.blueRoom, nodepal.redRoom, nodepal.greenRoom, nodepal.whiteRoom, nodepal.dirtRoom, nodepal.woodRoom, nodepal.grayRoom
      // nodepal.bluePentagon,
      buildingEdge, cavern, nukeSymbolCarpet,
      castleStairs, greenCastle, moon3way,
      bathrooms, parkingGarage, fountain, sushi, sewer
    ) ++ others.map(t => toPowerUp(random, t))


    // val keyRoom = if(random.nextBool()){
    //   normalRooms.append(stoneVaults.modified(sg => sg.withItem2(Item.Rpg)))
    //   blueItemRoom
    // }

    DropTileSet(
      startRoom,
      exitRoom,
      redGate,
      keyRoom,
      random.shuffle(normalRooms).toSeq
    )
  }
}
