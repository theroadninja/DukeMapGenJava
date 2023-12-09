package trn.prefab.experiments.dijkdrop

import trn.duke.TextureList
import trn.prefab.experiments.dijkdrop.SpriteGroups.{STANDARD_AMMO, StandardAmmo, BASIC_AMMO, BasicAmmo}
import trn.{Sprite, RandomX}
import trn.prefab.{SectorGroup, RedwallConnector, PrefabPalette, Item, Enemy, GameConfig, SpriteLogicException, Marker}

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
  val Lava = 9
  val BathroomTile = 10
  val RedBrick = 11 // Tile 3387
  val DirtyGrayLedge = 12
  val LightWood = 13
  val BlueBrick = 14 // Tile 3386

  def isTunnelTile(sg: SectorGroup): Boolean = {
    val bb = sg.boundingBox
    val c1: Int = sg.allRedwallConnectors.size
    val c2 = sg.allUnlinkedRedwallConns.filter { conn =>
      conn.getWallCount == 3 || conn.getWallCount == 1
    }
    val totalConnLength: Long = c2.map(_.totalManhattanLength()).sum
    val themeMarker = sg.sprites.find(s => Marker.isMarker(s, Marker.Lotags.ALGO_GENERIC)).size
    bb.width == 2048 && bb.height == 2048 && c1 == 2 && c2.size == 2 && totalConnLength == 2048 * 4 && themeMarker == 1
  }
}

class DropPalette2(
  gameCfg: GameConfig,
  random: RandomX,
  palette: PrefabPalette, // the main palette for this map
  stonePalette: PrefabPalette, // <- a source map dedicated to the stone room
  sewerPalette: PrefabPalette, // <- a source map dedicated to the sewer tunnel
  randomMoonRoomPalette: PrefabPalette,
) {
  val spriteGroups: Map[Int, Seq[Sprite]] = palette.scalaObj.spriteGroups
  val spriteGroups2: Map[Int, Seq[SimpleSpritePrefab]] = spriteGroups.mapValues { sprites =>
    sprites.map(s => SimpleSpritePrefab(s))
  }

  // The Blank tunnel connector, for when there is no edge
  val blank = palette.getSG(99)

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

  def standardRoomSetup(sg: SectorGroup): SectorGroup = {
    // TODO get rid of all this -- but what to do about items?
    val sg2 = Utils.withRandomSprites(sg, STANDARD_AMMO, Marker.Lotags.RANDOM_ITEM, StandardAmmo)
    val sg3 = Utils.withRandomSprites(sg2, BASIC_AMMO, Marker.Lotags.RANDOM_ITEM, BasicAmmo)
    // val sg4 = Utils.withRandomSprites(sg3, SpriteGroups.FOOT_SOLDIERS, Marker.Lotags.ENEMY, SpriteGroups.FootSoldiers)
    // val sg5 = Utils.withRandomSprites(sg4, SpriteGroups.OCTABRAINS, Marker.Lotags.ENEMY, SpriteGroups.Octabrains)
    // val sg6 = Utils.withRandomSprites(sg5, SpriteGroups.SPACE_FOOT_SOLDIERS, Marker.Lotags.ENEMY, SpriteGroups.SpaceFootSoldiers)
    val sg7 = Utils.withRandomSprites(sg3, SpriteGroups.BASIC_GUNS, Marker.Lotags.RANDOM_ITEM, SpriteGroups.BasicGuns)
    sg7.withInlineSpriteGroupsResolved(random)
  }

  def withEnemySpriteGroups(sg: SectorGroup): SectorGroup = {
    // distinct hitags of all "enemy" markers -- so we know which sprite groups to pull
    val enemyHiTags: Set[Int] = sg.allSprites.filter(s => Marker.isMarker(s, Marker.Lotags.ENEMY))
      .map(_.getHiTag)
      .filter(_ > 0)
      .toSet

    enemyHiTags.foldLeft(sg){ (sg_, spriteGroupId) =>
      val group = spriteGroups2.get(spriteGroupId).getOrElse {
        val msg = s"There is an enemy marker requesting sprite group ${spriteGroupId} but there is no Sprite Group Sector with that id (marker lotag ${Marker.Lotags.SPRITE_GROUP_ID}"
        throw new SpriteLogicException(msg)
      }
      Utils.withRandomSprites(sg_, spriteGroupId, Marker.Lotags.ENEMY, random.shuffle(group).toSeq)
    }
  }

  def withItemSpriteGroups(sg: SectorGroup): SectorGroup = {
    val itemHiTags: Set[Int] = sg.allSprites.filter(s => Marker.isMarker(s, Marker.Lotags.RANDOM_ITEM))
      .map(_.getHiTag).filter(_ > 0).toSet

    itemHiTags.foldLeft(sg){ (sg_, spriteGroupId) =>
      val group = spriteGroups2.get(spriteGroupId).getOrElse {
        val msg = s"There is an item marker requesting sprite group ${spriteGroupId} but there is no Sprite Group Sector with that id (marker lotag ${Marker.Lotags.SPRITE_GROUP_ID}"
        throw new SpriteLogicException(msg)
      }
      Utils.withRandomSprites(sg_, spriteGroupId, Marker.Lotags.RANDOM_ITEM, random.shuffle(group).toSeq)
    }
  }

  def withSpriteGroups(sg: SectorGroup): SectorGroup = withEnemySpriteGroups(withItemSpriteGroups(sg))

  val blueRoom = NodeTile2(palette.getSG(1), DropPalette2.Blue)
  val redRoom = NodeTile2(palette.getSG(2), DropPalette2.Red)
  val greenRoom = NodeTile2(palette.getSG(3), DropPalette2.Green)
  val whiteRoom = NodeTile2(palette.getSG(4), DropPalette2.White)
  val grayRoom = NodeTile2(palette.getSG(5), DropPalette2.Gray)
  val dirtRoom = NodeTile2(palette.getSG(6), DropPalette2.Dirt)
  val woodRoom = NodeTile2(palette.getSG(7), DropPalette2.Wood)

  val bluePentagon = NodeTile2(palette.getSG(8))
  val blueItemRoom = NodeTile2(palette.getSG(9))
    .withEnemies(random, spriteGroups2(2), hitag = 2)
    .withEnemies(random, spriteGroups2(3), hitag = 3)
    .modified(standardRoomSetup)


  val redGate = NodeTile2(palette.getSG(10))
    // .withEnemies(random, Seq(Enemy.LizTroop, Enemy.LizTroop, Enemy.PigCop, Enemy.PigCop, Enemy.Enforcer, Enemy.Enforcer, Enemy.OctaBrain, Enemy.AssaultCmdr))
    .withEnemies(random, spriteGroups2(10), hitag = 10)
    .modified(standardRoomSetup)

  val exitRoom = NodeTile2(palette.getSG(11))

  val startRoom = NodeTile2(palette.getSG(12)).modified(withItemSpriteGroups)

  val castleStairs = NodeTile2(palette.getSG(13)).modified(standardRoomSetup).modified(withEnemySpriteGroups).modified(withItemSpriteGroups)

  val moon3way = NodeTile2(palette.getSG(14)).modified(standardRoomSetup).modified(withEnemySpriteGroups)

  val bathrooms = NodeTile2(palette.getSG(15)).modified { sg =>
    // val sg2 = Utils.withRandomSprites(sg, 1, Marker.Lotags.ENEMY, random.shuffle(Seq(Enemy.LizTroopOnToilet, Enemy.Blank, Enemy.Blank)).toSeq)
    //val sg3 = Utils.withRandomSprites(sg, 0, Marker.Lotags.ENEMY, random.shuffle(Seq(Enemy.LizTroop, Enemy.LizTroopCrouch, Enemy.PigCop, Enemy.Blank)).toSeq)
    Utils.withRandomSprites(sg, 1, Marker.Lotags.RANDOM_ITEM, random.shuffle(Seq(Item.RpgAmmo, Item.Devastator)).toSeq)
  }.modified(standardRoomSetup).modified(withEnemySpriteGroups)

  val greenCastle = NodeTile2(palette.getSG(16)).modified { sg =>
    //val heavies = random.shuffle(Seq(Enemy.AssaultCmdr, Enemy.OctaBrain, Enemy.Blank, Enemy.Blank)).toSeq
    val enemies = random.shuffle(Seq(Enemy.LizTroop, Enemy.OctaBrain, Enemy.OctaBrain, Enemy.Enforcer, Enemy.Blank)).toSeq
    val powerups = random.shuffle(Seq(Item.AtomicHealth, Item.Rpg, Item.Devastator, Item.ShrinkRay, Item.FreezeRay, Item.Medkit)).toSeq
    // val sg2 = Utils.withRandomSprites(sg, 0, Marker.Lotags.ENEMY, heavies)
    val sg3 = Utils.withRandomSprites(sg, 1, Marker.Lotags.ENEMY, enemies)
    Utils.withRandomSprites(sg3, 0, Marker.Lotags.RANDOM_ITEM, powerups)
  }.modified(standardRoomSetup).modified(withEnemySpriteGroups).modified(withItemSpriteGroups)

  val buildingEdge = NodeTile2(palette.getSG(17)).modified(standardRoomSetup).modified(withEnemySpriteGroups)

  val cavern = NodeTile2(palette.getSG(18)).modified(standardRoomSetup).modified(withEnemySpriteGroups)

  val nukeSymbolCarpet = NodeTile2(palette.getSG(19)).modified(standardRoomSetup).modified(withEnemySpriteGroups)

  val parkingGarage = NodeTile2(palette.getSG(20)).modified(standardRoomSetup).modified(withEnemySpriteGroups)

  // can have key OR heavy weapon (but only want it in the level once)
  val stoneVaults = {
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
    Seq(100, 101, 102, 103, 104, 105, 106, 107).zipWithIndex.foreach{ case (connId, i) =>
      val conn = sg2.getRedwallConnector(connId)
      sg2 = sg2.withGroupAttachedAutoRotate(gameCfg, conn, chunks(i))(get100conn)
    }
    val sg3 = PipeRoom.fixTunnelRedwallConnIds(sg2)
    NodeTile2(sg3).modified(standardRoomSetup)
  }

  val fountain = NodeTile2(palette.getSG(21)).modified(standardRoomSetup)
    .withRandomItems(random, Seq(Item.Blank, Item.SmallHealth, Item.SmallHealth, Item.SmallHealth, Item.MediumHealth, Item.MediumHealth, Item.MediumHealth, Item.MediumHealth, Item.Rpg))

  val sushi = NodeTile2(palette.getSG(22)).modified(standardRoomSetup).modified(withSpriteGroups)

  val sewer = NodeTile2(PipeRoom.makePipeRoom(gameCfg, random, sewerPalette)).modified(standardRoomSetup)
    // .withEnemies(random, Seq(Enemy.OctaBrain, Enemy.Blank), hitag=1)

  val rooftopGate = {
    val mainRoof = palette.getSG(23)
    val decor = random.randomElement(
      Seq(
        palette.getSG(24),
        palette.getSG(25),
        palette.getSG(26),
      )
    ).withAlternateFloors(random)

    val roof2 = mainRoof.withGroupAttached(gameCfg, mainRoof.getRedwallConnector(100), decor, decor.getRedwallConnector(100)).autoLinked

    NodeTile2(roof2).modified(standardRoomSetup).modified(withEnemySpriteGroups)
  }

  val caveGate = NodeTile2(palette.getSG(27)).modified(standardRoomSetup).modified(withEnemySpriteGroups)

  // val randomMoonRoom = {
  //   NodeTile2(RandomMoonRoom.makeRoom(gameCfg, random, randomMoonRoomPalette)).modified(standardRoomSetup)
  // }

  val spaceStation = NodeTile2(palette.getSG(28)).modified(standardRoomSetup).modified(withEnemySpriteGroups)

  val chessRoom = NodeTile2(palette.getSG(29)).modified(standardRoomSetup).modified(withEnemySpriteGroups)

  val militaryComplex = NodeTile2(palette.getSG(30)).modified(standardRoomSetup).modified(withEnemySpriteGroups)

  val sushiRestaurant = NodeTile2(palette.getSG(31)).modified(standardRoomSetup).modified(withEnemySpriteGroups)

  def validateGate(gate: NodeTile2): NodeTile2 = {
    gate.sg.allRedwallConnectors.find(c => c.getConnectorId == 99).getOrElse {
      throw new SpriteLogicException(s"gate room id=${gate.sg.getGroupId} missing connector with id 99")
    }
    gate
  }

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
      bathrooms, parkingGarage, fountain, sushi, sewer,
      // randomMoonRoom, TODO this one isnt good
      spaceStation, chessRoom, militaryComplex, sushiRestaurant,
    ) ++ others.map(t => toPowerUp(random, t))


    // val keyRoom = if(random.nextBool()){
    //   normalRooms.append(stoneVaults.modified(sg => sg.withItem2(Item.Rpg)))
    //   blueItemRoom
    // }

    val gateRooms = Seq(redGate, rooftopGate, caveGate).map(validateGate)

    // NOTE:  can't set key colors here - key hasnt been inserted
    DropTileSet(
      startRoom,
      exitRoom,
      random.randomElement(gateRooms),
      keyRoom,
      sushiRestaurant +: random.shuffle(normalRooms).toSeq,
    )
  }
}
