package trn.prefab.experiments.tiling

import trn.prefab.{GameConfig, PrefabPalette, SectorGroup}
import trn.{HardcodedConfig, MapLoader, RandomX}

/**
  * Trying to put all of the info specific to each prefab/source map into a file like this.
  * Want to minimize this as much as possible -- it would be great if this information could be encoded in the map
  * itself....
  *
  * TODO can we use this?
  *
  * var GENERATOR_INPUT: Int = 10
  */
object PythMap1 extends TileFactory {

  def inputMap = HardcodedConfig.getEduke32Path("PYTH1.MAP")

  lazy val palette: PrefabPalette = MapLoader.loadPalette(inputMap)

  val smallWidth = 4096
  val bigWidth = 4096 * 3


  val Big1 = "BIG1"
  val Big2 = "BIG2" // replaced by Street
  val Small1 = "SMALL1"
  val Small2 = "SMALL2"

  val SmallEnd = "SMALL_END" // SG 26
  val BigEnd = "BIG_END" // SG 27

  val Street = "STREET"

  val BigTiles = Seq(Big1, Street)
  // val BigTiles = Seq(Street)

  /**
    * Decides which sector group will be placed in a tile, and returns its `name` which is a string.
    *
    * Multiple actual sector groups may be used for a single "logical one".  For example, a "Movie Theatre" tile might
    * have variants that include Sector Groups with IDs 42, 43, and 45 (which are chosen between based on some criteria)
    * but those Sector Groups are all considered "the movie theatre".
    *
    * @param tileType - PythTileType.BigTile or PythTileType.SmallTile
    * @param coord - the row,col coordinate of the tile we want to choose
    * @param edges - which edges have (or may have?) connections to other tiles
    * @return
    */
  def chooseTile(random: RandomX, coord: (Int, Int), tileType: Int, planNode: PlanNode, edges: Seq[Int]): String  = {
    // NOTE: passing in tileType her in case I want a Group Name to be able to refer to either size of a tile,
    // for example, there is a small or large version of the "movie theatre" ...


    tileType match {
      case PythTileType.BigTile if(planNode.end) => BigEnd
      case PythTileType.BigTile => random.randomElement(BigTiles)
      case PythTileType.SmallTile if(planNode.end) => SmallEnd
      case PythTileType.SmallTile => random.randomElement(Seq(Small1, Small2))
    }
  }

  def getTileMaker(gameCfg: GameConfig, name: String, tileType: Int): TileMaker = {
    // TODO all these makers should be put into lazy vals
    name match {
      case Big1 => new BigTile1(gameCfg, palette)
      case Big2 => new BigTile2(gameCfg, palette)
      case Street => StreetTile
      case Small1 => new SmallTile1(gameCfg, palette)
      case Small2 => new SmallTile2(gameCfg, palette)
      case SmallEnd => new SmallEndMaker(palette)
      case BigEnd => new BigEndMaker(palette)
    }
  }

  class BigEndMaker(palette: PrefabPalette) extends TileMaker {

    override def makeTile(gameCfg: GameConfig, tile: TileNode): SectorGroup = {
      require(tile.edges.size == 1)
      val sg = palette.getSG(27)
      // TODO need to rotate it!
      // TODO make a generate rotation function:  rotateUntil(sg, Seq[Edges], Seq[Edges] and it rotates until/if the edges line up
      sg
    }

  }
  class SmallEndMaker(palette: PrefabPalette) extends TileMaker {

    override def makeTile(gameCfg: GameConfig, tile: TileNode): SectorGroup = {
      require(tile.edges.size == 1)
      val sg = palette.getSG(26)
      // TODO need to rotate it!
      // TODO make a generate rotation function:  rotateUntil(sg, Seq[Edges], Seq[Edges] and it rotates until/if the edges line up
      sg
    }
  }

  class BigTile1(gameConfig: GameConfig, palette: PrefabPalette) extends TileMaker {
    override def makeTile(gameCfg: GameConfig, name: String, tileType: Int, edges: Seq[Int]): SectorGroup = {
      // TODO should attachments() be a standard part of the trait?
      val attachments = Map(
        BigTileEdge.ES -> palette.getSG(2),
        BigTileEdge.EB -> palette.getSG(3),
        BigTileEdge.SS -> palette.getSG(4),
        BigTileEdge.SB -> palette.getSG(5),
        BigTileEdge.WS -> palette.getSG(6),
        BigTileEdge.WB -> palette.getSG(7),
        BigTileEdge.NS -> palette.getSG(8),
        BigTileEdge.NB -> palette.getSG(9),
      )

      var center = palette.getSG(1)
      edges.foreach { attachId =>
        center = TileMaker.attachHallway(gameConfig, center, attachments, attachId)
      }
      center
    }
  }

  class BigTile2(gameConfig: GameConfig, palette: PrefabPalette) extends TileMaker {
    override def makeTile(gameConfg: GameConfig, name: String, tileType: Int, edges: Seq[Int]): SectorGroup = palette.getSG(15)
  }

  object StreetTile extends TileMaker {

    override def makeTile(gameCfg: GameConfig, tile: TileNode): SectorGroup = {

      // tile.edges.get(BigTileEdge.SB).foreach { edge =>
      //   // edge.info
      //
      // }

      val center = palette.getSG(21)
      // TileMaker.attachHallway(gameConfig, center, )

      val southeast = tile.edges.get(BigTileEdge.SB).filter(_.info.getOrElse("") == Street).map(_ => palette.getSG(24)).getOrElse(palette.getSG(22))
      // val southeast = palette.getSG(22)
      // center.withGroupAttached(gameCfg, center.getRedwallConnectorsById(BigTileEdge.SB).head, southeast, southeast.getRedwallConnectorsById(BigTileEdge.SB).head)
      val center2 = TileMaker.attachByConnId(gameCfg, center, southeast, BigTileEdge.SB)


      // TODO dont attach any SG if there is no edge!
      val northEdge = tile.edges.get(BigTileEdge.NB)
      println(s"street tile at ${tile.coord} north edge: ${northEdge}")

      val northId = northEdge.map(_.info.getOrElse("DEFAULT")).map { s =>
        s match {
          case Street => 25
          case _ => 23
        }
      }
      println(s"street tile at ${tile.coord} NB edge=${tile.edges.get(BigTileEdge.NB)} selected edge SG id: ${northId}")
      val northSg = northId.map(palette.getSG)

      //val northeast = tile.edges.get(BigTileEdge.NB).filter(_.info.getOrElse("") == Street).map(_ => palette.getSG(25)).getOrElse(palette.getSG(23))
      // val northeast = tile.edges.get(BigTileEdge.NB).map { edge =>
      //   edge.info.filter(_ == Street).map(_ => palette.getSG(25)).getOrElse(palette.getSG(23))
      // }


      val center3 = if(northSg.isDefined){
        TileMaker.attachByConnId(gameCfg, center2, northSg.get, BigTileEdge.NB)
      }else {
        center2
      }
      center3
    }
  }

  class SmallTile1(gameConfig: GameConfig, palette: PrefabPalette) extends TileMaker {
    override def makeTile(gameCfg: GameConfig, name: String, tileType: Int, edges: Seq[Int]): SectorGroup = {
      val attachments = Map(
        SmallTileEdge.E -> palette.getSG(11),
        SmallTileEdge.S -> palette.getSG(12),
        SmallTileEdge.W -> palette.getSG(13),
        SmallTileEdge.N -> palette.getSG(14),
      )
      var center = palette.getSG(10)
      edges.foreach { attachId =>
        center = TileMaker.attachHallway(gameConfig, center, attachments, attachId)
      }
      center
    }
  }

  class SmallTile2(gameConfig: GameConfig, palette: PrefabPalette) extends TileMaker {

    val DeadEnd = palette.getSG(20)
    val Corner = palette.getSG(16)
    val Straight = palette.getSG(17)
    val T = palette.getSG(18)
    val Fourway = palette.getSG(19)

    override def makeTile(gameCfg: GameConfig, name: String, tileType: Int, edges: Seq[Int]): SectorGroup = {
      edges.size match {
        case 1 => {
          // the prefab starts facing east
          val angle = SmallTileEdge.rotationToMatch(SmallTileEdge.E, edges.head)
          angle * DeadEnd
        }
        case 2 if SmallTileEdge.opposite(edges(0), edges(1)) => {
          // the prefab starts E-W
          if(edges.contains(SmallTileEdge.E)){
            Straight
          }else{
            Straight.rotatedCW
          }
        }
        case 2 => {
          // it starts out with connections a N and E
          if(edges.contains(SmallTileEdge.N) && edges.contains(SmallTileEdge.E)){
            // E is Less than N so we hardcode this one
            Corner
          } else {
            val angle = SmallTileEdge.rotationToMatch(SmallTileEdge.N, edges.sorted.head)
            angle * Corner
          }
        }
        case 3 => {
          // the missing edge stats off S
          val angle = SmallTileEdge.rotationToMatch(SmallTileEdge.S ,SmallTileEdge.all.filterNot(edges.contains).head)
          angle * T
        }
        case 4 => Fourway
        case _ => throw new Exception(s"invalid edges: ${edges}")
      }
    }

  }

  // TODO implement
  override def edgeInfo(tile: TileNode, edge: TileEdge, neighboor: TileNode): Option[String] = {
    if(tile.name == Street && neighboor.name == Street){
      edge.edgeId match {
        case BigTileEdge.SB => Some(Street)
        case BigTileEdge.NB => Some(Street)
        case _ => None
      }
    }else{
      None
    }
  }

}
