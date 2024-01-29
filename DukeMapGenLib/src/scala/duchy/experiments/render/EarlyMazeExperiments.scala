package duchy.experiments.render

import duchy.experiments.render.maze.{MutableMazeGraph, BlockCursor, BlockInfo, SimpleBlock, BlockTileset}

import collection.JavaConverters._
import org.apache.commons.lang3.tuple.{ImmutablePair, Pair}
import MutableMazeGraph.NodeId
import duchy.experiments.render.maze.stonetunnels.{ItemBlock, StoneConstants, StartBlock, ExitBlock, NarrowPassageBlock}
import trn.duke.TextureList
import trn.duke.experiments.{WallPrefab, SectorPrefab, SpritePrefab}
import trn.duke.experiments.gridblock.Grid
import trn.{MapUtil, Sector, RandomX, Wall, Main, PlayerStart, Map => DMap}
import trn.prefab.BoundingBox

import scala.+:

trait Block {
  // returns sectorId
  def draw(map: DMap, gridCoordinte: NodeId): Int
}

class E6Block(info: BlockInfo, wallLength: Int = 2048) extends Block {
  override def draw(map: DMap, gridCoordinate: NodeId): Int = {
    val wallTex = info.tileset.wallTex
    val (x, y) = gridCoordinate
    val west = x * wallLength
    val east = (x + 1) * wallLength
    val north = y * wallLength
    val south = (y + 1) * wallLength

    val nw = new Wall(west, north, wallTex, 16, 8) //first wall; also matches the grid coordinate
    val ne = new Wall(east, north, wallTex, 16, 8)
    val se = new Wall(east, south, wallTex, 16, 8)
    val sw = new Wall(west, south, wallTex, 16, 8)
    val sectorId = map.createSectorFromLoop(nw, ne, se, sw);
    val sector = map.getSector(sectorId)
    info.applyToSector(sector)
    sectorId
  }
}


/**
  * This is intended to be a rewrite of early experiments that created mazes, done in Java.
  */
object EarlyMazeExperiments {

  // def getBoundingBoxInclusive(maze: MutableMazeGraph[NodeId, String]): BoundingBox = {
  def getBoundingBoxInclusive(nodes: Iterable[NodeId]): BoundingBox = {
    // inclusive on both ends
    val (xs, ys) = nodes.unzip
    BoundingBox(
      xs.min,
      ys.min,
      xs.max,
      ys.max,
    )
  }

  def gridToStr(maze: MutableMazeGraph[NodeId, String]): String = {
    val bb = getBoundingBoxInclusive(maze.getNodes())
    val str = scala.collection.mutable.ListBuffer[String]()
    for(y <- bb.yMax to bb.yMin by -1){
      for(x <- bb.xMin to bb.xMax){
        if(maze.contains((x, y))){
          str.append("#")
        }else{
          str.append(" ")
        }
        if(maze.containsEdge((x, y), (x+1, y))){
          str.append("-")
        }else{
          str.append(" ")
        }
      }
      str.append("\n")

      // in-between rows
      for(x <- bb.xMin to bb.xMax){
        if(maze.containsEdge((x, y), (x, y - 1))){
          str.append("| ")
        }else{
          str.append("  ")
        }
      }
      str.append("\n")
    }
    str.mkString("")
  }

  def gridToStr2(nodes: Set[NodeId]): String = {
    val bb = getBoundingBoxInclusive(nodes)
    val str = scala.collection.mutable.ListBuffer[String]()
    for (y <- bb.yMax to bb.yMin by -1) {
      for (x <- bb.xMin to bb.xMax) {
        if(nodes.contains((x, y))){
          str.append("#")
        }else{
          str.append(" ")
        }
      }
      str.append("\n")
    }
    str.mkString("")
  }

  def expand[B](maze: MutableMazeGraph[NodeId, B]): Set[NodeId] = {
    maze.getNodes().map { case (x, y) =>
      val newNode = (x * 2, y * 2)
      val fillerNodes = maze.getListFor((x, y)).map { case (neighboorX, neighboorY) =>
        (x + neighboorX, y + neighboorY)
      }
      fillerNodes.add(newNode)
      // fillerNodes.toSet
      fillerNodes
    }.flatten.toSet

  }

  def main(args: Array[String]): Unit = {
    // E8StoneTunnels.main(args)
    // println("test")

    // val maze = MutableMazeGraph.createGridMaze[String](10, 10)
    // println(gridToStr(maze))
    // val maze2 = expand(maze)
    // println(gridToStr2(maze2))

    if(true){
      // experiment6()
      experiment8()
    }else{
      // keep refs alive
      experiment6()
      experiment7()
      experiment8()
    }
  }


  /**
    * From E6CreateMazeWBlocks.java
    */
  def experiment6(): Unit = {
    val graph = MutableMazeGraph.createGridMaze[String](10, 10)
    val random = RandomX()

    val MazeWallTex = 772
    val block0 = BlockTileset(MazeWallTex, 0, 0)
    val block1 = BlockTileset(781, 782, 781)
    val block2 = BlockTileset(800, 801, 800)
    val blocks = Seq(block0, block1, block2)
    val Z_STEP_HEIGHT = 1024
    val oneLevelDown = Sector.DEFAULT_FLOOR_Z + Z_STEP_HEIGHT * 6;
    val floorHeights = Seq(Sector.DEFAULT_FLOOR_Z, Sector.DEFAULT_FLOOR_Z, oneLevelDown)


    val expanded = expand(graph)

    val nodeIdToBlock = expanded.map { nodeId =>
      val block = random.randomElement(blocks)
      val floorz = random.randomElement(floorHeights)
      nodeId -> new E6Block(BlockInfo(floorz, block))
    }.toMap

    val map = createMap(nodeIdToBlock)
    Main.deployTest(map)

  }

  def createMap(nodeToBlock: Map[NodeId, Block]): DMap = {
    val map = DMap.createNew()
    map.setPlayerStart(new PlayerStart(512, 512, 0, PlayerStart.SOUTH))

    // create the sectors
    val nodeIdToSector = nodeToBlock.map { case(nodeId, blockInfo) =>
      val sectorId = createSector(map, nodeId, blockInfo)
      nodeId -> sectorId
    }

    // link them together
    nodeIdToSector.foreach { case ((x, y), sectorId) =>
      val east = (x + 1, y)
      val south = (x, y + 1)
      nodeIdToSector.get(east).foreach(eastId => MapUtil.autoLinkWalls(map, sectorId, eastId))
      nodeIdToSector.get(south).foreach(southId => MapUtil.autoLinkWalls(map, sectorId, southId))
    }

    map
  }

  def createSector(map: DMap, gridCoordinate: NodeId, block: Block): Int = {
    block.draw(map, gridCoordinate)
  }

  def experiment7(): Unit = {
    ??? // this is from E7BetterBlocks.java.  I don't think it is worth the effort to preserve.
  }

  /**
    * This is a reimplementation of trn.duke.experiments.E8StoneTunnels
    */
  def experiment8(): Unit = {
    val cursor = new BlockCursor(0, 1)
    val start = new StartBlock(cursor.get) //0,1

    def passageBlock(gridCoordinate: Pair[Integer, Integer]): SimpleBlock = {
      val sb = new SimpleBlock(gridCoordinate)
      sb.setWallPrefab(new WallPrefab(StoneConstants.UPPER_WALL_TEX).setShade(StoneConstants.SHADE))
      sb.setSectorPrefab(new SectorPrefab(StoneConstants.UPPER_FLOOR, StoneConstants.UPPER_CEILING).setFloorShade(StoneConstants.SHADE).setCeilingShade(StoneConstants.SHADE))
      sb
    }


    val grid = new Grid
    grid.add(start)
    grid.add(new NarrowPassageBlock(cursor.moveNorth, NarrowPassageBlock.VERTICAL))
    //this blows up, but not very cleanly (should the grid check?)
    //grid.add(new PassageBlock(new ImmutablePair<Integer, Integer>(-1, 0)));
    grid.add(passageBlock(cursor.moveNorth)) // 0, -1
    grid.add(passageBlock(cursor.moveEast)) // 1, -1

    //grid.add(new ItemBlock(new ImmutablePair<Integer, Integer>(2, -1), new SpritePrefab(TextureList.Items.CARD)));//grid.add(new ItemBlock(new ImmutablePair<Integer, Integer>(2, -1), new SpritePrefab(TextureList.Items.CARD)));
    grid.add(new ItemBlock(new ImmutablePair[Integer, Integer](2, -(1)), new SpritePrefab(TextureList.Items.ARMOR)))
    grid.add(passageBlock(new ImmutablePair[Integer, Integer](3, -(1))))
    grid.add(passageBlock(new ImmutablePair[Integer, Integer](4, -(1))))
    grid.add(passageBlock(new ImmutablePair[Integer, Integer](5, -(1))))

    grid.add(passageBlock(new ImmutablePair[Integer, Integer](5, 0)))

    grid.add(new ItemBlock(new ImmutablePair[Integer, Integer](5, 1), new SpritePrefab(TextureList.Enemies.LIZTROOP)))

    grid.add(passageBlock(new ImmutablePair[Integer, Integer](5, 2)))
    grid.add(new ExitBlock(new ImmutablePair[Integer, Integer](5, 3)))

    val map = createMap8(grid, start.getPlayerStart)

    Main.deployTest(map)
  }

  def createMap8(grid: Grid, ps: PlayerStart): DMap = {
    val map = DMap.createNew
    map.setPlayerStart(ps)

    //create the sectors//create the sectors
    grid.getNodes.asScala.foreach { p =>
      grid.getBlock(p).draw(map)

    }

    def toPair(tuple2: (Int, Int)): Pair[Integer, Integer] = {
      val (x, y) = tuple2
      new ImmutablePair[Integer, Integer](x, y)
    }

    def fromPair(pair: Pair[Integer, Integer]): (Int, Int) = {
      (pair.getLeft.toInt, pair.getRight.toInt)
    }

    // link the sectors
    grid.getNodes.asScala.foreach { p =>
      val (x, y) = fromPair(p)
      val east = (x + 1, y)
      val south = (x, y + 1)

      val currentBlock = grid.getBlock(p)
      if(grid.contains(toPair(east))){
        currentBlock.getEastConnector().draw(map, grid.getBlock(toPair(east)))
      }
      if(grid.contains(toPair(south))){
        currentBlock.getSouthConnector().draw(map, grid.getBlock(toPair(south)))
      }
    }

    map
  }

}
