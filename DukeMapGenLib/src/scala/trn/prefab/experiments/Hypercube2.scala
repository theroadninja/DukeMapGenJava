package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapUtil, PointXY, PointXYZ, Sprite, Map => DMap}
import trn.MapImplicits._
import trn.duke.PaletteList


class Hyper2MapBuilder(val outMap: DMap) extends MapBuilder {

  val grid = scala.collection.mutable.Map[(Int, Int, Int, Int), PastedSectorGroup]()

  // nice comfy origin
  val origin: PointXYZ = new PointXYZ(DMap.MIN_X + 10*1024, DMap.MIN_Y + 10*1024, 0)

  // rooms are six big grid cells wide and anchor is in the middle
  // hallways are one grid cell wide
  val cellDist = (6 + 1) * 1024
  val hallwayWidth = 1024
  val MAXGRID = 2


  // From hypercube1:
  //
  // def placeHorizontalHallway(left: PastedSectorGroup, right: PastedSectorGroup, hallway: SectorGroup): Unit = {
  //   val leftConn = left.findFirstConnector(SimpleConnector.EastConnector)
  //   val rightConn = right.findFirstConnector(SimpleConnector.WestConnector)
  //   val cdelta: PointXYZ = westConnector(hallway).getTransformTo(leftConn)
  //   val pastedHallway = pasteSectorGroup(hallway, cdelta)
  //   joinWalls(pastedHallway.findFirstConnector(SimpleConnector.WestConnector), leftConn)
  //   joinWalls(pastedHallway.findFirstConnector(SimpleConnector.EastConnector), rightConn)
  // }

  def joinWalls(c1: Connector, c2: Connector): Unit = {
    PrefabUtils.joinWalls(outMap, c1.asInstanceOf[RedwallConnector], c2.asInstanceOf[RedwallConnector])
  }


  def addRoom(room: SectorGroup, gridCell: (Int, Int, Int, Int)): PastedSectorGroup = {

    val anchor: PointXYZ = room.getAnchor


    // offsets must shift the max width of the grid for each value
    val offsetForZ = gridCell._3 * cellDist * (MAXGRID + 1)
    val offsetForW = gridCell._4 * cellDist * (MAXGRID + 1) // TODO - actually we'll want W in the same place ...
    val x = gridCell._1 * cellDist + offsetForW
    val y = gridCell._2 * cellDist + offsetForZ
    val z = gridCell._3 * cellDist

    val tr = anchor.getTransformTo(origin.add(new PointXYZ(x, y, z)))
    val psg = pasteSectorGroup(room, tr)

    grid(gridCell) = psg
    psg
  }

  def placeHallwayEW(hallway: SectorGroup, left: (Int, Int, Int, Int), right: (Int, Int, Int, Int)): PastedSectorGroup = {
    if(left._1 + 1 != right._1) throw new IllegalArgumentException
    if(!(left._2 == right._2 && left._3 == right._3 && left._4 == right._4)) throw new IllegalArgumentException

    val leftRoom = grid(left)
    val rightRoom = grid(right)

    // val offsetForZ = left._3 * cellDist * (MAXGRID + 1)
    // val offsetForW = left._4 * cellDist * (MAXGRID + 1) // TODO - actually we'll want W in the same place ...
    // val x = left._1 * cellDist + offsetForW + cellDist / 2 //+ hallwayWidth / 2
    // val y = left._2 * cellDist + offsetForZ
    // val z = left._3 * cellDist

    //val tr = hallway.getAnchor.getTransformTo(origin.add(new PointXYZ(x, y, z)))
    //val tr = hallway.getAnchor.getTransformTo(origin)


    // need to ignore z of anchor b/c of door ...
    //val pastedHallway = pasteSectorGroupAt(hallway, origin.add(new PointXYZ(x, y, z)))
    //val anchor = new PointXYZ(hallway.getAnchor.asPointXY(), 2048)
    //val tr = anchor.getTransformTo(origin.add(new PointXYZ(x, y, z)))
    //val pastedHallway = pasteSectorGroup(hallway, tr)

    def westConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.WestConnector).asInstanceOf[RedwallConnector]




    val leftConn = leftRoom.findFirstConnector(SimpleConnector.EastConnector)
    val rightConn = rightRoom.findFirstConnector(SimpleConnector.WestConnector)
    val cdelta: PointXYZ = westConnector(hallway).getTransformTo(leftConn)
    val pastedHallway = pasteSectorGroup(hallway, cdelta)
    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.WestConnector), leftConn)
    joinWalls(pastedHallway.findFirstConnector(SimpleConnector.EastConnector), rightConn)
    pastedHallway
  }



}
object Hypercube2 {


  // note:  6 large squares ( 1042 ) seem like a good size.

  def run(sourceMap: DMap): DMap = {
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new Hyper2MapBuilder(DMap.createNew())

    val basicRoom = palette.getSectorGroup(100)
    val eastWestHallway = palette.getSectorGroup(200)
    val northSouthWallway = palette.getSectorGroup(201)

    val pastedRoom1 = builder.addRoom(basicRoom, (1, 0, 0, 0))
    //builder.pasteSectorGroupAt(basicRoom, new PointXYZ(0, 0, 0))

    val bigRoomWidth = 6 * 1024
    val hallwayWidth = 1 * 1024

    val a = basicRoom.getAnchor.asPointXY
    // val basicRoom2 = basicRoom.rotateAroundCW(a).rotateAroundCW(a)
    // builder.pasteSectorGroupAt(basicRoom2.asInstanceOf[SectorGroup], new PointXYZ(1024 * 10, 0, 0))

    // val basicRoom3 = basicRoom.flippedX(a.x)
    // builder.pasteSectorGroupAt(basicRoom3.asInstanceOf[SectorGroup], new PointXYZ(1024 * -7, 0, 0))
    val pastedRoom2 = builder.addRoom(basicRoom.flippedX(a.x), (0, 0, 0, 0))


    builder.placeHallwayEW(eastWestHallway, (0, 0, 0, 0), (1, 0, 0, 0))
    //builder.pasteSectorGroupAt(eastWestHallway, new PointXYZ(1024 * -7/2, 0, 0))
    //builder.pasteSectorGroupAt(eastWestHallway, builder.origin)



    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }


}
