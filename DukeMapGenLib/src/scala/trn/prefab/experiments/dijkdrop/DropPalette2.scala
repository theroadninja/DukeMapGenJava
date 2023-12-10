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

  // create a floor tunnel sprite group
  def getFloorTunnel(connectorId: Int, tunnelTheme: Int): SectorGroup = {
    val tSG = floorTunnels(tunnelTheme).sg
    tSG.withModifiedSprites { sprite =>
      if (isFallConn(sprite)) {
        sprite.setHiTag(connectorId)
      }
    }
  }

  // create a ceiling tunnel sprite group
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

  def stdRoomSetup(sg: SectorGroup): SectorGroup = {
    val sg2 = withEnemySpriteGroups(withItemSpriteGroups(sg))
    sg2.withInlineSpriteGroupsResolved(random)
  }

  def standardRoomSetup(sg: SectorGroup): SectorGroup = {
    // TODO get rid of all this -- but what to do about items?
    val sg2 = Utils.withRandomSprites(sg, STANDARD_AMMO, Marker.Lotags.RANDOM_ITEM, StandardAmmo)
    val sg3 = Utils.withRandomSprites(sg2, BASIC_AMMO, Marker.Lotags.RANDOM_ITEM, BasicAmmo)
    // val sg4 = Utils.withRandomSprites(sg3, SpriteGroups.FOOT_SOLDIERS, Marker.Lotags.ENEMY, SpriteGroups.FootSoldiers)
    // val sg5 = Utils.withRandomSprites(sg4, SpriteGroups.OCTABRAINS, Marker.Lotags.ENEMY, SpriteGroups.Octabrains)
    // val sg6 = Utils.withRandomSprites(sg5, SpriteGroups.SPACE_FOOT_SOLDIERS, Marker.Lotags.ENEMY, SpriteGroups.SpaceFootSoldiers)
    val sg7 = Utils.withRandomSprites(sg3, SpriteGroups.BASIC_GUNS, Marker.Lotags.RANDOM_ITEM, SpriteGroups.BasicGuns)
    val sg8 = sg7.withInlineSpriteGroupsResolved(random)
    // withSpriteGroups(sg8)
    sg8
  }

  // TODO should probably moved to SectorGroup class (not sure about making marker 8 always work this way though)
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

  // TODO should probably moved to SectorGroup class (using marker 31 to set sprite groups is set it stone, but not sure
  // about enshrining the relationship to marker 23, although it does make sense since item marker is 9)
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

  def withSpriteGroups(sg: SectorGroup): SectorGroup = {
    withEnemySpriteGroups(withItemSpriteGroups(sg))
  }

  // val blueRoom = NodeTile2(palette.getSG(1), DropPalette2.Blue)
  // val bluePentagon = NodeTile2(palette.getSG(8))

  val blueItemRoom = NodeTile2(palette.getSG(9)).modified(stdRoomSetup)
    // .withEnemies(random, spriteGroups2(2), hitag = 2)
    // .withEnemies(random, spriteGroups2(3), hitag = 3)
    // .modified(standardRoomSetup)


  val redGate = NodeTile2(palette.getSG(10)).modified(stdRoomSetup)
  val exitRoom = NodeTile2(palette.getSG(11)).modified(stdRoomSetup)
  val startRoom = NodeTile2(palette.getSG(12)).modified(stdRoomSetup)
  val castleStairs = NodeTile2(palette.getSG(13)).modified(stdRoomSetup)
  val moon3way = NodeTile2(palette.getSG(14)).modified(stdRoomSetup)
  val bathrooms = NodeTile2(palette.getSG(15)).modified(stdRoomSetup)
  val greenCastle = NodeTile2(palette.getSG(16)).modified(stdRoomSetup)
  val buildingEdge = NodeTile2(palette.getSG(17)).modified(stdRoomSetup)
  val cavern = NodeTile2(palette.getSG(18)).modified(stdRoomSetup)
  val nukeSymbolCarpet = NodeTile2(palette.getSG(19)).modified(stdRoomSetup)
  val parkingGarage = NodeTile2(palette.getSG(20)).modified(stdRoomSetup)
  val fountain = NodeTile2(palette.getSG(21)).modified(stdRoomSetup)
  val sushi = NodeTile2(palette.getSG(22)).modified(stdRoomSetup)

  val caveGate = NodeTile2(palette.getSG(27)).modified(stdRoomSetup)
  val spaceStation = NodeTile2(palette.getSG(28)).modified(stdRoomSetup)
  val chessRoom = NodeTile2(palette.getSG(29)).modified(stdRoomSetup)
  val militaryComplex = NodeTile2(palette.getSG(30)).modified(stdRoomSetup)
  val sushiRestaurant = NodeTile2(palette.getSG(31)).modified(stdRoomSetup)

  // ------------------

  val sewer = NodeTile2(PipeRoom.makePipeRoom(gameCfg, random, sewerPalette)).modified(stdRoomSetup)
  val stoneVaults = CustomRooms.stoneVaults(gameCfg, random, stonePalette).modified(stdRoomSetup)

  // TODO make this work using an alternative, "algorithm-specific" marker based on texture 310
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
      random.shuffle(normalRooms).toSeq,
    )
  }
}
