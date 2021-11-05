package trn.bespoke.moonbase2

import trn.bespoke.moonbase2.MoonBase2.{getTileSpec, rotateToMatch}
import trn.logic.{Point3d, Tile2d}
import trn.{AngleUtil, HardcodedConfig, MapLoader, RandomX}
import trn.prefab.{CompassWriter, DukeConfig, GameConfig, MapWriter, PrefabPalette, PrefabUtils, SectorGroup}

import scala.collection.mutable


// set lotag to 13, "ALGO_HINT"
object AlgoHint {
  val OneWay = 1 // does the angle of the sprite indicate which side connects to the higher zone?
  // val OnewayFromE = 0
  // val OnewayFromS = 0
  // val OnewayFromW = 0
  // val OnewayFromN = 0
  val Gate = 4
  // something about a key? = 5

  /** allows the group to function as a "wildcard" that can connect with any number of connections.  It MUST have four
    * connections
    */
  val WildcardConns = 6
  val Unique = 69

  // TODO a "dont-auto-scan" marker for things that have multiple components (or need to be manually built for some other reason)
}
object MoonBase3 {


  def autoReadTile(sg: SectorGroup): Tile2d = Tile2d(
    CompassWriter.east(sg).map(_ => Tile2d.Conn).getOrElse(Tile2d.Blocked),
    CompassWriter.south(sg).map(_ => Tile2d.Conn).getOrElse(Tile2d.Blocked),
    CompassWriter.west(sg).map(_ => Tile2d.Conn).getOrElse(Tile2d.Blocked),
    CompassWriter.north(sg).map(_ => Tile2d.Conn).getOrElse(Tile2d.Blocked),
  )

  def readTileSectorGroup(gameCfg: GameConfig, palette: PrefabPalette, groupId: Int): TileSectorGroup = {
    val sg = palette.getSG(groupId)
    val connsOptional: Boolean = sg.containsSprite(s => PrefabUtils.isMarker(s, AlgoHint.WildcardConns, PrefabUtils.MarkerSpriteLoTags.ALGO_HINT))
    val tile = if(connsOptional){
      val tmp = autoReadTile(sg)
      require(tmp == Tile2d(Tile2d.Conn), s"group ${groupId} marked as wildcard but does not have all 4 connections")
      Tile2d(Tile2d.Wildcard)
    }else{
      autoReadTile(sg)

    }
    val tags = mutable.Set[String]()
    if(sg.hasPlayerStart){
      tags.add(RoomTags.Start)
    }
    if(sg.hasEndGame){
      tags.add(RoomTags.End)
    }
    if(sg.containsSprite(s => gameCfg.isKeycard(s.getTex))){
      tags.add(RoomTags.Key)
    }
    if(sg.containsSprite(s => PrefabUtils.isMarker(s, AlgoHint.Gate, PrefabUtils.MarkerSpriteLoTags.ALGO_HINT))){
      tags.add(RoomTags.Gate)
    }
    if(sg.containsSprite(s => PrefabUtils.isMarker(s, AlgoHint.Unique, PrefabUtils.MarkerSpriteLoTags.ALGO_HINT))){
      tags.add(RoomTags.Unique)
    }
    val oneway = sg.allSprites.find(s => PrefabUtils.isMarker(s, AlgoHint.OneWay, PrefabUtils.MarkerSpriteLoTags.ALGO_HINT))
    oneway.foreach { s =>
      tags.add(RoomTags.OneWay)
    }
    if(oneway.isDefined){
      TileSectorGroup.oneWay(groupId.toString, tile, sg, tags.toSet, oneway.get.getAngle)
    }else{
      TileSectorGroup(groupId.toString, tile, sg, tags.toSet)
    }
  }


  /**
    * Make sure that we have all the shapes X room types that we need.
    * @param rooms
    */
  def sanityCheck(rooms: Seq[TileSectorGroup]): Unit = {
    val Single = Tile2d(Tile2d.Conn, Tile2d.Blocked, Tile2d.Blocked, Tile2d.Blocked)   // single

    val StandardShapes = Seq(
      Single,
      Tile2d(Tile2d.Conn, Tile2d.Conn, Tile2d.Conn, Tile2d.Conn),   // four way
      Tile2d(Tile2d.Conn, Tile2d.Conn, Tile2d.Conn, Tile2d.Blocked), // T
      Tile2d(Tile2d.Conn, Tile2d.Blocked, Tile2d.Conn, Tile2d.Blocked), // straight
      Tile2d(Tile2d.Conn, Tile2d.Conn, Tile2d.Blocked, Tile2d.Blocked), // corner
    )

    val StartShapes = Seq(Single)
    StartShapes.foreach { tile =>
      if(!rooms.exists(r => r.tags.contains(RoomTags.Start) && r.tile.couldMatch(tile))){
        throw new Exception(s"no start room matches tile ${tile}")
      }
      if(!rooms.exists(r => r.tags.contains(RoomTags.End) && r.tile.couldMatch(tile))){
        throw new Exception(s"no start room matches tile ${tile}")
      }
    }

    StandardShapes.foreach { tile =>
      if(! rooms.exists(r => r.tags.contains(RoomTags.Key) && r.tile.couldMatch(tile))){
        throw new Exception(s"no key room matches tile ${tile}")
      }
      if(! rooms.exists(r => r.tags.intersect(RoomTags.Special).isEmpty && r.tile.couldMatch(tile))){
        throw new Exception(s"no normal room matches tile ${tile}")
      }
    }

    // for now, we are assuming gates only have two connectors
    val GateShapes = Seq(
      Tile2d(Tile2d.Conn, Tile2d.Blocked, Tile2d.Conn, Tile2d.Blocked), // straight
      Tile2d(Tile2d.Conn, Tile2d.Conn, Tile2d.Blocked, Tile2d.Blocked), // corner
    )
    GateShapes.foreach { tile =>
      if(! rooms.exists(r => r.tags.contains(RoomTags.Gate) && r.tile.couldMatch(tile))){
        throw new Exception(s"no gate room matches tile ${tile}")
      }

      // TODO this isnt enough, because a (1,1,0,0) is really (2,1,0,0) vs (1,2,0,0) ... one conn goes to a room with
      // the higher zone, so they are not interchangeable
      if(! rooms.exists(r => r.tags.contains(RoomTags.OneWay) && r.tile.couldMatch(tile))){
        throw new Exception(s"no one-way room matches tile ${tile}")
      }
    }


  }

  def run(gameCfg: GameConfig): Unit = {

    val random = new RandomX()
    val writer = MapWriter(gameCfg)
    val moonPalette = MapLoader.loadPalette(HardcodedConfig.getEduke32Path("moon3.map"))
    println("loaded moon3.map successfully")

    val allRooms = (1 to moonPalette.numberedSectorGroupCount()).map(i => readTileSectorGroup(gameCfg, moonPalette, i))
    sanityCheck(allRooms)



    val usedTiles = mutable.Set[String]()
    // tiles.foreach { tile =>
    //   println("---------------------")
    //   println(tile.id)
    //   println(tile.tile.toPrettyStr())
    //   println(tile.tags)
    //   println()
    // }




    val logicalMap = new RandomWalkGenerator(random).generate()
    val keycolors: Seq[Int] = random.shuffle(DukeConfig.KeyColors).toSeq
    println(logicalMap)

    val sgChoices: Map[Point3d, TileSectorGroup] = logicalMap.nodes.map { case (gridPoint: Point3d, node: LogicalRoom) =>
      val tileSpec = getTileSpec(logicalMap, gridPoint)
      val target = logicalMap.getTile(gridPoint, Tile2d.Blocked)
      require(target == tileSpec.toTile2d(Tile2d.Blocked))
      val sg = getTile(gameCfg, random, allRooms, usedTiles, node, tileSpec, node.tag, keycolors)
      gridPoint -> sg
    }.toMap

    MoonBase2.pasteRooms(gameCfg, writer, random, logicalMap, sgChoices)
    MoonBase2.finishAndWrite(writer)
  }

  def main(args: Array[String]): Unit = {
    val gameCfg = DukeConfig.load(HardcodedConfig.getAtomicWidthsFile)
    run(gameCfg)
  }


  // TODO DRY with MoonBase2.getTile()
  def getTile(
    gameCfg: GameConfig,
    r: RandomX,
    allRooms: Seq[TileSectorGroup],
    usedTiles: mutable.Set[String],
    node: LogicalRoom,
    tileSpec: TileSpec, // target: Tile2d,
    // wildcardTarget: Tile2d, // stupid hack so that tiles with too many connections are still allowed
    tag: Option[String], // <- the tag requested by the logic map
    keycolors: Seq[Int]
  ): TileSectorGroup = {

    val target = tileSpec.toTile2d(Tile2d.Blocked)

    tag match {
      case Some(RoomTags.Key) => {
        val keycolor = keycolors(node.keyindex.get)
        val options = r.shuffle(allRooms.filter(_.tags.contains(RoomTags.Key))).filter(_.tile.couldMatch(target)).toSeq
        if(options.isEmpty){
          println(target.toPrettyStr)
        }

        val sg = options.head
        MoonBase2.rotateToMatch(sg, target).withKeyLockColor(gameCfg, keycolor)
      }
      case Some(RoomTags.Gate) => {
        val keycolor = keycolors(node.keyindex.get)
        val gateSg = r.shuffle(allRooms.filter(_.tags.contains(RoomTags.Gate))).filter(_.tile.couldMatch(target)).toSeq.head
        MoonBase2.rotateToMatch(gateSg, target).withKeyLockColor(gameCfg, keycolor)

      }
      case Some(RoomTags.OneWay) => {
        val oneWayTarget = tileSpec.toOneWayTile2d(Tile2d.Blocked)
        val room = r.shuffle(allRooms.filter(r => r.tags.contains(RoomTags.OneWay) && r.oneWayTile.isDefined && r.oneWayTile.get.couldMatch(oneWayTarget))).toSeq.head
        MoonBase2.rotateOneWayToMatch(room, room.oneWayTile.get, oneWayTarget)
      }
      case Some(tag) => {
        // start and end are done here
        val t = r.shuffle(allRooms.filter(_.tags.contains(tag))).toSeq.head
        MoonBase2.rotateToMatch(t, target)
      }
      case None => {
        // TODO:  apply the uniqueness requirement to gate and key rooms also!
        val options = allRooms.find{ tsg =>
          tsg.tags.intersect(RoomTags.Special).isEmpty && !TileSectorGroup.uniqueViolation(usedTiles, tsg) && tsg.tile.couldMatch(target)
        }
        if(options.isEmpty){
          println(target.toPrettyStr)
        }
        val room = r.shuffle(options).toSet.head
        usedTiles.add(room.id)
        rotateToMatch(room, target)
      }
    }


  }
}
