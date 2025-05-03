package trn.prefab.gatekeytreewalk

import trn.RandomX
import trn.bespoke.moonbase2.LogicalMap
import trn.logic.Point3d

import scala.collection.mutable


case class TWRoom(
  roomType: String,
  zone: Int,
) {
  require(zone >= 0)

  private def roomType2: String = if(roomType == TWRoom.Blank){ "" } else { roomType }

  override def toString(): String = s"${roomType2}${zone}"
}

object TWRoom {
  val Start = "S"
  val Gate = "G"
  val End = "E"
  val Key = "K"

  val Blank = "_"

  val StartRoom = TWRoom(Start, 0)

  def normal(zone: Int): TWRoom = TWRoom(Blank, zone)

  // gate zones are one higher then the zone you enter from
  // and equal to the zone you exit to
  def gate(zone: Int): TWRoom = TWRoom(Gate, zone)

  // the zone is the zone that the key UNLOCKS, not the zone that can reach it
  def key(zone: Int): TWRoom = TWRoom(Key, zone)
  def end(zone: Int): TWRoom = TWRoom(End, zone)
}

class Walker(val r: RandomX) {

  /**
    * return a random step, but only one with the same z coordinate.
    * No diagonals.
    *
    * path - the current path so far
    */
  def step(map: LogicalMap[_, _], from: Point3d, path: Seq[Point3d] = Seq.empty): Option[Point3d] = {
    val available = map.emptyAdj(from).filter(p => p.z == from.z && !path.contains(p))
    r.randomElementOpt(available)
  }

  /**
    * Generates a random path, but does not change the z height
    */
  def flatWalk(map: LogicalMap[_, _], from: Point3d, stepCount: Int): Seq[Point3d] = {
    var current = from
    val path = mutable.ArrayBuffer[Point3d]()
    for(_ <- 0 until stepCount){
      val nextOpt = step(map, current, path)
      val next = nextOpt.getOrElse(throw new Exception("backtracking not implemented yet"))
      path.append(next)
      current = next
    }
    path
  }

  /**
    * @return (gate location, attach point)
    */
  def attachLocation(
    map: LogicalMap[TWRoom, _],
    zones: Seq[Int],
  ): (Point3d, Point3d) = {
    val attachPoints = map.nodes.filter { case (location, existingRoom) =>
      zones.contains(existingRoom.zone) && map.emptyAdj(location).filter(_.z == location.z).nonEmpty
    }.keys
    require(attachPoints.size > 0)
    val current = r.randomElement(attachPoints)
    val gateLoc = step(map, current).get
    (gateLoc, current)
  }
}

/**
  * Generates the abstract "logical map" for GateKeyTreeWalk1
  *
  * e.g. this:
  *
  * K1 - 0 - S
  * |    |
  * G2   G1 - K2
  * |
  * 2  - E
  */
object LogicalMapGenerator {

  /** places a gate key on an existing, empty space */
  private[gatekeytreewalk] def addGateKey2(
    walker: Walker,
    map: LogicalMap[TWRoom, String],
    attachZones: Seq[Int],
    keyZone: Int,
    overwrite: Boolean,
  ): Point3d = {
    val attachPoints = map.nodes.filter{ case (p, room: TWRoom) =>
      attachZones.contains(room.zone) && room.roomType == TWRoom.Blank && map.emptyAdj(p).filter(_.z == p.z).nonEmpty
    }
    val attachPoint = walker.r.randomElement(attachPoints)._1
    if(overwrite){
      // write key directly on the path
      map.put(attachPoint, TWRoom.key(keyZone), overwrite = true)
      attachPoint
    }else{
      // put key in a new space, adjacent to the path
      val keyloc = walker.step(map, attachPoint).get
      map.put(keyloc, TWRoom.key(keyZone), overwrite=false)
      map.putEdgeSafe(attachPoint, keyloc, "")
      keyloc
    }
  }

  private[gatekeytreewalk] def addGateKey(
    walker: Walker,
    map: LogicalMap[TWRoom, String],
    attachZones: Seq[Int],
    keyZone: Int,
  ): Point3d = {
    addGateKey2(walker, map, attachZones, keyZone, walker.r.nextBool())

  }

  def fillEdges[E](map: LogicalMap[_, E], points: Seq[Point3d], edgeFill: E): Unit = {
    points.sliding(2, 1).foreach { pp =>
      map.putEdgeSafe(pp(0), pp(1), edgeFill)
    }
  }

  def attachGate(
    walker: Walker,
    map: LogicalMap[TWRoom, String],
    attachZones: Seq[Int],
    gateZone: Int,
  ): Point3d = {
    val (gateLoc, attachLoc) = walker.attachLocation(map, attachZones)
    map.put(gateLoc, TWRoom.gate(gateZone))
    map.putEdgeSafe(attachLoc, gateLoc, "") // TODO - so "" means normal connection?
    gateLoc
  }


  def generate(
    random: RandomX,
    start: Point3d = Point3d.Zero,
    stepCount: Int = 3,
  ): LogicalMap[TWRoom, String] = {
    val map = LogicalMap[TWRoom, String]

    val walker = new Walker(random)

    var current = start
    map.put(current, TWRoom.StartRoom)

    var path = walker.flatWalk(map, current, stepCount)
    map.putAll(path, TWRoom.normal(0))
    fillEdges(map, current +: path, "")
    current = path.last

    current = attachGate(walker, map, Seq(0), 1)

    path = walker.flatWalk(map, current, stepCount)
    map.putAll(path, TWRoom.normal(1))
    fillEdges(map, current +: path, "")
    current = attachGate(walker, map, Seq(1), 2)

    path = walker.flatWalk(map, current, stepCount)
    map.putAll(path, TWRoom.normal(2))
    fillEdges(map, current +: path, "")
    current = attachGate(walker, map, Seq(2), 3)

    path = walker.flatWalk(map, current, 3)
    map.putAll(path, TWRoom.normal(3))
    fillEdges(map, current +: path, "")
    val end = walker.step(map, path.last).get
    map.put(end, TWRoom.end(3))
    map.putEdgeSafe(path.last, end, "")

    addGateKey(walker, map, Seq(0), 1)
    addGateKey(walker, map, Seq(1), 2)
    addGateKey(walker, map, Seq(2), 3)

    map
  }

  def main(args: Array[String]): Unit = {
    val map = generate(RandomX())
    println(map.toString)
  }

}
