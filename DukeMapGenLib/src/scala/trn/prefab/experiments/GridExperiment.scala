package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapLoader, MapUtil, PointXY, PointXYZ, Map => DMap}

import scala.collection.JavaConverters._


trait Node {
  def gridX: Int
  def gridY: Int
  def location: (Int, Int);

  def east: Option[RedwallConnector]
  def west: Option[RedwallConnector]
  def north: Option[RedwallConnector]
  def south: Option[RedwallConnector]

  def neighboors: Seq[(Int, Int)] = GridUtil.neighboors(gridX, gridY)

  /**
    * @param n2
    * @returns the pair connectors that would be between two adjacent nodes, returning None if the node does
    *          not have a connector in that direction.
    */
  def matchingConnectors(n2: Node): Seq[Option[RedwallConnector]] = {
    val n1 = this
    if(n2.gridX == n1.gridX - 1){ // n2 is to the west
      return Seq(n2.east, n1.west)
    }else if(n2.gridX == n1.gridX + 1){ // n2 is to the right
      return Seq(n1.east, n2.west)
    }else if(n2.gridY == n1.gridY - 1){ // n2 is to the north
      return Seq(n2.south, n1.north)
    }else if(n2.gridY == n1.gridY + 1){ // n2 is to the south
      return Seq(n1.south, n2.north)
    }else{
      throw new IllegalArgumentException("nodes are not adjacent")
    }
  }

  def matchingConnector(loc: (Int, Int)): Option[RedwallConnector] = {
    val x = loc._1
    val y = loc._2
    if(x == gridX - 1){
      return west
    }else if(x == gridX + 1){
      return east
    }else if(y == gridY - 1){
      return north
    }else if(y == gridY + 1){
      return south
    }else{
      return None
    }
  }
}

case class GridNode (
  gridX: Int,
  gridY: Int,
  sg: SectorGroup
) extends Node {
  override def location: (Int, Int) = (gridX, gridY)
  override def east: Option[RedwallConnector] = MapWriter.east(sg)
  override def west: Option[RedwallConnector] = MapWriter.west(sg)
  override def north: Option[RedwallConnector] = MapWriter.north(sg)
  override def south: Option[RedwallConnector] = MapWriter.south(sg)

  def asPasted(psg: PastedSectorGroup, delta: PointXYZ): PastedGridNode = PastedGridNode(gridX, gridY, psg)
}

case class PastedGridNode(
  gridX: Int,
  gridY: Int,
  psg: PastedSectorGroup
) extends Node {
  override def location: (Int, Int) = (gridX, gridY)
  override def east: Option[RedwallConnector] = MapWriter.east(psg)
  override def west: Option[RedwallConnector] = MapWriter.west(psg)
  override def north: Option[RedwallConnector] = MapWriter.north(psg)
  override def south: Option[RedwallConnector] = MapWriter.south(psg)
}

object GridMapBuilder {
  val gridSize = 5 * 1024

  private def axisConns(sg: SectorGroup, axis: Seq[Int]): Seq[RedwallConnector] = {
    sg.allRedwallConnectors.filter(c => axis.contains(c.getSimpleHeading))
  }

  def compatibleSg(sg: SectorGroup): Boolean = {
    val horiz = axisConns(sg, Seq(Heading.E, Heading.W)).size
    val vert = axisConns(sg, Seq(Heading.N, Heading.S)).size
    sg.boundingBox.fitsInside(gridSize, gridSize) && horiz <= 2 && vert <= 2
  }

}

class GridMapBuilder(val outMap: DMap, val random: RandomX = new RandomX()) extends MapBuilder {
  import GridMapBuilder.gridSize
  val writer = MapWriter(this)

  // grid coordinates
  val minX = 0
  val minY = 0
  val maxX = 16
  val maxY = 16

  val topLeftRealX = -61440
  val topLeftRealY = topLeftRealX

  val grid = scala.collection.mutable.Map[(Int, Int), PastedGridNode]()

  def randomElement[E](collection: Iterable[E]): E = random.randomElement(collection)

  private def snapH(sg: SectorGroup, dest: BoundingBox): Int = {
    (MapWriter.east(sg).isDefined, MapWriter.west(sg).isDefined) match {
      case (_, true) => dest.xMin  // align left
      case (true, false) => dest.xMin + (dest.w - sg.boundingBox.w) // align right
      case (false, false) => (dest.xMin + dest.w/2) - sg.boundingBox.w/2 // align center
    }
  }

  private def snapV(sg: SectorGroup, dest: BoundingBox): Int = {
    (MapWriter.north(sg).isDefined, MapWriter.south(sg).isDefined) match {
      case (true, _) => dest.yMin // align top
      case (false, true) => dest.yMin + (dest.h - sg.boundingBox.h) // align bottom
      case (false, false) => (dest.yMin + dest.h/2) - sg.boundingBox.h/2 // align center
    }
  }

  def snapToGridCell(sg: SectorGroup, dest: BoundingBox): PointXY = new PointXY(snapH(sg, dest), snapV(sg, dest))

  def findEmptyConnectableNeighboors: Set[((Int, Int))] = {
    def hasOpenConnector(loc: (Int, Int)): Boolean = {
      val reverseNeighboors = GridUtil.neighboors(loc).flatMap(n => grid.get(n))
      reverseNeighboors.filter(_.matchingConnector(loc).nonEmpty).nonEmpty
    }
    if(grid.isEmpty) throw new IllegalStateException("grid is empty")
    val emptyNeighboors = grid.values.flatMap(_.neighboors).toSet.filter(! grid.contains(_))
    emptyNeighboors.filter(n => inBounds(n._1, n._2)).filter(hasOpenConnector)
  }

  private def inBounds(gridX: Int, gridY: Int): Boolean ={
    0 <= gridX && gridX < maxX && 0 <= gridY && gridY < maxY
  }

  private def gridCellBBox(node: Node): BoundingBox = {
    val gridX = node.gridX
    val gridY = node.gridY
    val dest = BoundingBox(gridX * gridSize, gridY * gridSize, (gridX + 1) * gridSize, (gridY + 1) * gridSize)
    // this works because they are negative:
    dest.translate(new PointXY(topLeftRealX, topLeftRealY))
  }

  private def unmatchedConnectors(n1: GridNode, n2: Node): Boolean = n1.matchingConnectors(n2).filter(_.nonEmpty).size == 1

  def pasteNode(unpastedNode: GridNode): Unit = {
    require(unpastedNode.sg.boundingBox.fitsInside(gridSize, gridSize), "Sector group too large for grid")
    val translate = unpastedNode.sg.boundingBox.getTranslateTo(
      snapToGridCell(unpastedNode.sg, gridCellBBox(unpastedNode))
    ).withZ(0)
    val (psg, _) = sgBuilder.pasteSectorGroup2(unpastedNode.sg, translate)
    val node = unpastedNode.asPasted(psg, translate)
    node.neighboors.flatMap(loc => grid.get(loc)).map { neighboor =>
      writer.autoLink(node.psg, neighboor.psg)
    }
    grid(node.location) = node
  }

  def placeInGrid(sg: SectorGroup, gridX: Int, gridY: Int): Boolean = {
    val unpastedNode = GridNode(gridX, gridY, sg)
    val allowMismatch = false
    val neighboors = unpastedNode.neighboors.flatMap(grid.get(_))
    if(allowMismatch || !neighboors.exists(n => unmatchedConnectors(unpastedNode, n))){
      pasteNode(unpastedNode)
      true
    }else{
      false
    }
  }
}

/**
  * Sequel to FristPrefabExperiment (which was formerly copy test three in
  * java/trn.duke.experiments.prefab.PrefabExperiment)
  *
  * Uses the prefabs in cptest3.map which are mostly designed to fit inside a 5x5 (*1024) grid, which
  * redwall connectors 1(*1024) wide on the outer edges.
  */
object GridExperiment extends PrefabExperiment {
  override val Filename = "cptest3.map"

  override def run(mapLoader: MapLoader): DMap = {
    val mapWithPrefabs = mapLoader.load(Filename)
    val palette: PrefabPalette = PrefabPalette.fromMap(mapWithPrefabs);
    val allGroups:Seq[SectorGroup] = palette.allSectorGroups().asScala.filter(GridMapBuilder.compatibleSg).toSeq

    val startGroups = allGroups.filter(_.hasPlayerStart)
    val endGroups = allGroups.filter(_.hasEndGame)
    val groups = allGroups.filter(g => ! (g.hasPlayerStart || g.hasEndGame))
    require(!startGroups.isEmpty)
    require(!endGroups.isEmpty)

    val random = new RandomX()
    val sgStart = random.randomElement(startGroups)

    val gBuilder = new GridMapBuilder(DMap.createNew(), random)
    gBuilder.placeInGrid(sgStart, 8, 8)
    gBuilder.placeInGrid(endGroups(0), gBuilder.random.nextInt(16), gBuilder.random.nextInt(8) * 2 - 1) // force odd row, so it wont hit start
    generateGrid(gBuilder, groups)
    gBuilder.setPlayerStart()
    gBuilder.clearMarkers()
    gBuilder.outMap
  }
  def generateGrid(gBuilder: GridMapBuilder, groups: Seq[SectorGroup]): Unit = {
    for(_ <- 0 until 64){
      val neighboors = gBuilder.findEmptyConnectableNeighboors
      if(neighboors.size > 0){
        val nextPlace = gBuilder.randomElement(neighboors)
        gBuilder.placeInGrid(gBuilder.randomElement(groups), nextPlace._1, nextPlace._2)
      } else { println("ran out of neighboors") }
    }
  }
}
