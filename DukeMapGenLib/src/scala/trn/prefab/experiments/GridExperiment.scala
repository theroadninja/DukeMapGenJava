package trn.prefab.experiments

import trn.duke.experiments.prefab.MapBuilder
import trn.prefab._
import trn.{Main, MapUtil, PointXY, PointXYZ, Wall, Map => DMap}

import scala.collection.JavaConverters._



case class GridNode(
  gridX: Int,
  gridY: Int,
  east: Option[SimpleConnector],
  west: Option[SimpleConnector],
  north: Option[SimpleConnector],
  south: Option[SimpleConnector],
  psg: PastedSectorGroup
) {
  def location: (Int, Int) = (gridX, gridY)
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

class GridMapBuilder(outMap: DMap) {
  import GridMapBuilder.gridSize

  val grid = scala.collection.mutable.Map[(Int, Int), GridNode]()

  def placeInGrid(sg: SectorGroup, gridX: Int, gridY: Int): Unit = {

    val bb = sg.boundingBox
    if(! bb.fitsInside(gridSize, gridSize)){
      throw new IllegalArgumentException("sector group too large for grid")
    }

    val dest = BoundingBox(gridX * gridSize, gridY * gridSize, (gridX + 1) * gridSize, (gridY + 1) * gridSize)

    val size1 = sg.connectors.size()
    val connectors: Map[Int, SimpleConnector] = sg.connectors.asScala.map(c => (c.getConnectorType, c.asInstanceOf[SimpleConnector])).toMap
    val east = connectors.contains(ConnectorType.HORIZONTAL_EAST)
    val west = connectors.contains(ConnectorType.HORIZONTAL_WEST)
    val north = connectors.contains(ConnectorType.VERTICAL_NORTH)
    val south = connectors.contains(ConnectorType.VERTICAL_SOUTH)
    val size2 = connectors.size
    if(size1 != size2){
      println(s"connector problem with sector group ${sg.getGroupId}")
      println(s"\tthere are ${sg.connectors.size}; they are:")
      sg.connectors.asScala.foreach(c => println(s"\tconnector type: ${c.getConnectorType}"))
    }

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


    // TODO - next check for existing groups, to make sure connectors line up

    val translate = new PointXYZ(bb.translateTo(new PointXY(newX, newY)), 0)
    val psg =new PastedSectorGroup( outMap, MapUtil.copySectorGroup(sg.map, outMap, 0, translate));
    // paletteConnector.translateIds(result.copystate.idmap);
    val idmap = psg.copystate.idmap
    val node = GridNode(
      gridX,
      gridY,
      connectors.get(ConnectorType.HORIZONTAL_EAST).map(_.translateIds(idmap)),
      connectors.get(ConnectorType.HORIZONTAL_WEST).map(_.translateIds(idmap)),
      connectors.get(ConnectorType.VERTICAL_NORTH).map(_.translateIds(idmap)),
      connectors.get(ConnectorType.VERTICAL_SOUTH).map(_.translateIds(idmap)),
      psg
    )


    // TODO - actually we should refuse to place if there is a connector mismatch
    val neighboors:Seq[(Int,Int)] = Seq((gridX+1, gridY), (gridX-1, gridY), (gridX, gridY+1), (gridX, gridY-1))
    neighboors.map(loc => grid.get(loc)).flatten.map { neighboor =>
      join(node, neighboor)
    }

    grid(node.location) = node

  }

  private def join(n1: GridNode, n2: GridNode): Unit = {

    def join2(c1: Option[SimpleConnector], c2: Option[SimpleConnector]): Unit = {
      if(c1.nonEmpty && c2.nonEmpty){
        PrefabUtils.joinWalls(outMap, c1.get, c2.get)
      }else{
        println("WARNING: connectors not joined!")
      }
    }
    if(n2.gridX == n1.gridX - 1){ // n2 is to the west
      println("n2 is to the west")
      join2(n2.east, n1.west)
    }else if(n2.gridX == n1.gridX + 1){ // n2 is to the right
      println("n2 is to the east")
      join2(n1.east, n2.west)
    }else if(n2.gridY == n1.gridY - 1){ // n2 is to the north
      println("n2 is to the north")
      join2(n2.south, n1.north)
    }else if(n2.gridY == n1.gridY + 1){ // n2 is to the south
      println("n2 is to the south")
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
    val fromMap: DMap = Main.loadMap(Main.DOSPATH + "test1.map");
    //

    val palette = PrefabPalette.fromMap(fromMap)

    palette.getSectorGroup(42).connectors.asScala.foreach { c =>
      println(s"connector: ${c.getConnectorType}")
    }
  }


  def run(mapWithPrefabs: DMap): DMap = {
    val palette: PrefabPalette = PrefabPalette.fromMap(mapWithPrefabs);
    val mb: MapBuilder = new MapBuilder(palette);
    //palette.fromMap(fromMap);

    // next ... find all groups with bounding box of a certain size
    // for(SectorGroup sg : palette.allSectorGroups()){
    //   // TODO - add bboxHeight() and bboxWidth() to SectorGroup and print out all bounding box dimensions ...
    //   System.out.println("sector group " + sg.getGroupId() + " bounding box: " + sg.bbWidth() + " x " + sg.bbHeight());
    // }

    // palette.allSectorGroups().forEach()
    //val groups:Seq[SectorGroup] = palette.allSectorGroups().asScala.filter(_.boundingBox.fitsInside(5 * 1024, 5 * 1024)).toSeq
    val groups:Seq[SectorGroup] = palette.allSectorGroups().asScala.filter(GridMapBuilder.compatibleSg).toSeq
    println(s"found ${groups.size} groups")

    val playerStarts = groups.filter(_.hasPlayerStart)
    if(playerStarts.isEmpty){
      throw new SpriteLogicException("no available groups have player starts")
    }


    val gBuilder = new GridMapBuilder(mb.getOutMap())

    val sgStart = playerStarts(0)
    //palette.pasteSectorGroup(sgStart, mb.getOutMap, new PointXYZ(0, 0, 0))

    gBuilder.placeInGrid(sgStart, 0, 0)

    // 10, 16, and 17 are the 4-way areas
    gBuilder.placeInGrid(palette.getSectorGroup(10), 1, 0)
    gBuilder.placeInGrid(palette.getSectorGroup(16), 2, 0)
    gBuilder.placeInGrid(palette.getSectorGroup(17), 1, 1)
    gBuilder.placeInGrid(palette.getSectorGroup(10), 2, 1)

    gBuilder.placeInGrid(palette.getSectorGroup(12), 3, 0)
    gBuilder.placeInGrid(palette.getSectorGroup(12), 5, 0)
    gBuilder.placeInGrid(palette.getSectorGroup(12), 4, 0)


    mb.selectPlayerStart()
    mb.clearMarkers()
    mb.getOutMap()
  }

}
