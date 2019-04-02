package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapLoader, MapUtil, PointXY, PointXYZ, Map => DMap}

import scala.collection.JavaConverters._


object Node {

  def neighboors(gridX: Int, gridY: Int): Seq[(Int,Int)] = {
    Seq((gridX+1, gridY), (gridX-1, gridY), (gridX, gridY+1), (gridX, gridY-1))
  }
  def neighboors(loc: (Int, Int)): Seq[(Int, Int)] = neighboors(loc._1, loc._2)
}

trait Node {
  def gridX: Int
  def gridY: Int
  def location: (Int, Int);

  def east: Option[SimpleConnector]
  def west: Option[SimpleConnector]
  def north: Option[SimpleConnector]
  def south: Option[SimpleConnector]

  // def neighboors: Seq[(Int,Int)] = {
  //   Seq((gridX+1, gridY), (gridX-1, gridY), (gridX, gridY+1), (gridX, gridY-1))
  // }
  def neighboors: Seq[(Int, Int)] = Node.neighboors(gridX, gridY)

  /**
    * @param n2
    * @returns the pair connectors that would be between two adjacent nodes, returning None if the node does
    *          not have a connector in that direction.
    */
  def matchingConnectors(n2: Node): (Option[SimpleConnector], Option[SimpleConnector]) = {
    val n1 = this
    if(n2.gridX == n1.gridX - 1){ // n2 is to the west
      return (n2.east, n1.west)
    }else if(n2.gridX == n1.gridX + 1){ // n2 is to the right
      return (n1.east, n2.west)
    }else if(n2.gridY == n1.gridY - 1){ // n2 is to the north
      return (n2.south, n1.north)
    }else if(n2.gridY == n1.gridY + 1){ // n2 is to the south
      return (n1.south, n2.north)
    }else{
      throw new IllegalArgumentException("nodes are not adjacent")
    }
  }

  def matchingConnector(loc: (Int, Int)): Option[SimpleConnector] = {
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
  sg: SectorGroup,
  connectors: Map[Int, SimpleConnector]
) extends Node {

  override def location: (Int, Int) = (gridX, gridY)

  override def east: Option[SimpleConnector] = connectors.get(ConnectorType.HORIZONTAL_EAST)
  override def west: Option[SimpleConnector] = connectors.get(ConnectorType.HORIZONTAL_WEST)
  override def north: Option[SimpleConnector] = connectors.get(ConnectorType.VERTICAL_NORTH)
  override def south: Option[SimpleConnector] = connectors.get(ConnectorType.VERTICAL_SOUTH)

  def asPasted(psg: PastedSectorGroup): PastedGridNode = {
    val idmap = psg.copystate.idmap
    PastedGridNode(
      gridX,
      gridY,
      psg,
      east.map(_.translateIds(idmap)),
      west.map(_.translateIds(idmap)),
      north.map(_.translateIds(idmap)),
      south.map(_.translateIds(idmap)),
    )
  }
}

case class PastedGridNode(
  gridX: Int,
  gridY: Int,
  psg: PastedSectorGroup,
  east: Option[SimpleConnector],
  west: Option[SimpleConnector],
  north: Option[SimpleConnector],
  south: Option[SimpleConnector],
) extends Node {
  override def location: (Int, Int) = (gridX, gridY)
}


object GridMapBuilder {
  val gridSize = 5 * 1024

  def compatibleSg(sg: SectorGroup): Boolean = {
    sg.boundingBox.fitsInside(gridSize, gridSize) && horiz(sg).size <= 2 && vert(sg).size <= 2
  }

  def horiz(sg: SectorGroup): Seq[Connector] = {
    sg.connectors.asScala.filter(c => Seq(ConnectorType.HORIZONTAL_EAST, ConnectorType.HORIZONTAL_WEST).contains(c.getConnectorType))
  }

  def vert(sg: SectorGroup): Seq[Connector] = {
    sg.connectors.asScala.filter(c => Seq(ConnectorType.VERTICAL_NORTH, ConnectorType.VERTICAL_SOUTH).contains(c.getConnectorType))
  }
}

class GridMapBuilder(val outMap: DMap, random: RandomX = new RandomX()) extends MapBuilder {
  import GridMapBuilder.gridSize

  // grid coordinates
  val minX = 0
  val minY = 0
  val maxX = 16
  val maxY = 16

  val topLeftRealX = -61440
  val topLeftRealY = topLeftRealX

  val grid = scala.collection.mutable.Map[(Int, Int), PastedGridNode]()

  def randomElement[E](collection: Iterable[E]): E = random.randomElement(collection)

  /** align the sector group so that any connectors are lined up with the edges of the grid cell */
  def snapToGridCell(sg: SectorGroup, dest: BoundingBox, simpleConns: Map[Int, SimpleConnector]): PointXY = {

    val east = simpleConns.contains(ConnectorType.HORIZONTAL_EAST)
    val west = simpleConns.contains(ConnectorType.HORIZONTAL_WEST)
    val north = simpleConns.contains(ConnectorType.VERTICAL_NORTH)
    val south = simpleConns.contains(ConnectorType.VERTICAL_SOUTH)
    val size2 = simpleConns.size

    if(simpleConns.size != size2){
      println(s"connector problem with sector group ${sg.getGroupId}")
      println(s"\tthere are ${sg.connectors.size}; they are:")
      sg.connectors.asScala.foreach(c => println(s"\tconnector type: ${c.getConnectorType}"))
    }

    val bb = sg.boundingBox

    // Horizontal
    val newX:Int = (east, west) match {
      case (true, true) => {
        require(dest.w == bb.w, "sector group has wrong width")
        dest.xMin
      }
      case (true, false) => dest.xMin + (dest.w - bb.w) // align to right
      case (false, true) => dest.xMin  // align to left (west)
      case (false, false) => (dest.xMin + dest.w/2) - bb.w/2 // center
    }

    val newY = (north, south) match {
      case (true, true) => {
        require(dest.h == bb.h, "sector group has wrong height")
        dest.yMin
      }
      case (true, false) => dest.yMin // align to top (north)
      case (false, true) => dest.yMin + (dest.h - bb.h) // align to bottom
      case (false, false) => (dest.yMin + dest.h/2) - bb.h/2 // center
    }
    new PointXY(newX, newY)
  }

  def findEmptyConnectableNeighboors: Set[((Int, Int))] = {
    def hasOpenConnector(loc: (Int, Int)): Boolean = {
      val reverseNeighboors = Node.neighboors(loc).flatMap(n => grid.get(n))
      reverseNeighboors.filter(_.matchingConnector(loc).nonEmpty).nonEmpty
    }
    if(grid.isEmpty) throw new IllegalStateException("grid is empty")
    val emptyNeighboors = grid.values.flatMap(_.neighboors).toSet.filter(! grid.contains(_))
    emptyNeighboors.filter(n => inBounds(n._1, n._2)).filter(hasOpenConnector)
  }

  def inBounds(gridX: Int, gridY: Int): Boolean ={
    0 <= gridX && gridX < maxX && 0 <= gridY && gridY < maxY
  }



  def gridCellBoundingBox(gridX: Int, gridY: Int): BoundingBox = {
    val dest = BoundingBox(gridX * gridSize, gridY * gridSize, (gridX + 1) * gridSize, (gridY + 1) * gridSize)
    // this works because they are negative:
    dest.translate(new PointXY(topLeftRealX, topLeftRealY))
  }

  def placeInGrid(sg: SectorGroup, gridX: Int, gridY: Int, allowMismatch: Boolean = false): Boolean = {

    val bb = sg.boundingBox
    if(! bb.fitsInside(gridSize, gridSize)){
      throw new IllegalArgumentException("sector group too large for grid")
    }

    //val dest = BoundingBox(gridX * gridSize, gridY * gridSize, (gridX + 1) * gridSize, (gridY + 1) * gridSize)
    val dest = gridCellBoundingBox(gridX, gridY)
    // println(s"grid bounding box: ${dest}")

    val redwallConns = sg.connectorsWithXYRequrements()
    val connectors: Map[Int, SimpleConnector] = redwallConns.asScala.map(c => (c.getConnectorType, c)).toMap

    val newXY = snapToGridCell(sg, dest, connectors)

    val unpastedNode = GridNode(gridX, gridY, sg, connectors)

    if(!allowMismatch){
      unpastedNode.neighboors.flatMap(grid.get(_)).foreach { neighboor =>
        val cs = unpastedNode.matchingConnectors(neighboor)
        if(1 == Seq(cs._1, cs._2).filter(_.nonEmpty).size){
          return false; // one has a connector, the other doesnt
        }
      }
    }

    val translate = new PointXYZ(bb.getTranslateTo(newXY), 0)
    val psg = new PastedSectorGroup(outMap, MapUtil.copySectorGroup(sg.map, outMap, 0, translate));
    val node = unpastedNode.asPasted(psg)

    // TODO - unit test neighboors, and all this code with the grid nodes!!

    val neighboors:Seq[(Int,Int)] = node.neighboors
    neighboors.map(loc => grid.get(loc)).flatten.map { neighboor =>
      join(node, neighboor)
    }

    grid(node.location) = node

    true
  }

  private def join(n1: PastedGridNode, n2: PastedGridNode): Unit = {

    def join2(c1: Option[SimpleConnector], c2: Option[SimpleConnector]): Unit = {
      if(c1.nonEmpty && c2.nonEmpty){
        PrefabUtils.joinWalls(outMap, c1.get, c2.get)
      }else{
        println("WARNING: connectors not joined! (can happen if allowMismatch is true")
      }
    }
    if(n2.gridX == n1.gridX - 1){ // n2 is to the west
      // println("n2 is to the west")
      join2(n2.east, n1.west)
    }else if(n2.gridX == n1.gridX + 1){ // n2 is to the right
      // println("n2 is to the east")
      join2(n1.east, n2.west)
    }else if(n2.gridY == n1.gridY - 1){ // n2 is to the north
      // println("n2 is to the north")
      join2(n2.south, n1.north)
    }else if(n2.gridY == n1.gridY + 1){ // n2 is to the south
      // println("n2 is to the south")
      join2(n1.south, n2.north)
    }
  }

}

/**
  * based on copy test three in java/trn.duke.experiments.prefab.PrefabExperiment
  *
  * Uses the prefabs in cptest3.map which are mostly designed to fit inside a 5x5 (*1024) grid, which
  * redwall connectors 1(*1024) wide on the outer edges.
  */
object GridExperiment {

  def sanityCheck(): Unit = {
    val fromMap: DMap = MapLoader.loadMap(Main.DOSPATH + "test1.map");
    //

    val palette = PrefabPalette.fromMap(fromMap)

    palette.getSectorGroup(42).connectors.asScala.foreach { c =>
      println(s"connector: ${c.getConnectorType}")
    }
  }

  def run(mapWithPrefabs: DMap): DMap = {
    val palette: PrefabPalette = PrefabPalette.fromMap(mapWithPrefabs);

    val allGroups:Seq[SectorGroup] = palette.allSectorGroups().asScala.filter(GridMapBuilder.compatibleSg).toSeq


    println(s"found ${allGroups.size} groups")
    val startGroups = allGroups.filter(_.hasPlayerStart)
    val endGroups = allGroups.filter(_.hasEndGame)
    val groups = allGroups.filter(g => ! (g.hasPlayerStart || g.hasEndGame))

    if(startGroups.isEmpty){
      throw new SpriteLogicException("missing start groups")
    }
    if(endGroups.isEmpty){
      throw new SpriteLogicException("missing end groups")
    }

    val random = new RandomX()
    val sgStart = random.randomElement(startGroups)
    require(sgStart.connectors.size() > 0)


    // // 10, 16, and 17 are the 4-way areas

    def generateGrid(sgStart: SectorGroup, sgEnd: SectorGroup, normalGroups: Seq[SectorGroup], rand: RandomX): GridMapBuilder = {
      val gBuilder = new GridMapBuilder(DMap.createNew(), random)
      gBuilder.placeInGrid(sgStart, 8, 8)
      gBuilder.placeInGrid(sgEnd, rand.nextInt(16), rand.nextInt(8) * 2 - 1) // force odd row, so it wont hit start

      var hasEnd = false
      for(i <- 0 until 64){
        println(i)
        val neighboors = gBuilder.findEmptyConnectableNeighboors
        if(neighboors.size > 0){
          val nextPlace = gBuilder.randomElement(gBuilder.findEmptyConnectableNeighboors)
          val nextGroup = gBuilder.randomElement(groups)
          if(nextGroup.containsSprite{s =>
            s.getTexture == DukeConstants.TEXTURES.NUKE_BUTTON && s.getLotag == DukeConstants.LOTAGS.NUKE_BUTTON_END_LEVEL
          }){
            if(hasEnd){
              // skip
              println("skipping endgame sector group")
            } else {
              hasEnd = true
              gBuilder.placeInGrid(nextGroup, nextPlace._1, nextPlace._2)
            }
          } else {
            gBuilder.placeInGrid(nextGroup, nextPlace._1, nextPlace._2)
          }
        } else { println("ran out of neighboors") }
      }
      return gBuilder
    }

    val gBuilder = generateGrid(sgStart, endGroups(0), groups, random)

    gBuilder.setPlayerStart()
    gBuilder.clearMarkers()
    gBuilder.outMap
  }

}
