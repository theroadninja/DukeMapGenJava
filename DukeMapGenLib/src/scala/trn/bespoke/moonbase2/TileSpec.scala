package trn.bespoke.moonbase2
import trn.logic.Tile2d
import trn.prefab.Heading

/**
  * Describes a place in the grid for a node to fit into.
  * Replacement for the `Tile2d` type which was not descriptive enough.
  *
  */
case class TileSpec(e: Side, s: Side, w: Side, n: Side) { // does this need to extend RotatesCW[] ?

  def withSide(heading: Int, value: Side): TileSpec = {
    heading match {
      case Heading.E => TileSpec(value, s, w, n)
      case Heading.S => TileSpec(e, value, w, n)
      case Heading.W => TileSpec(e, s, value, n)
      case Heading.N => TileSpec(e, s, w, value)
      case _ => ???
    }
  }

  private[moonbase2] def allSides = Map(Heading.E -> e, Heading.S -> s, Heading.W -> w, Heading.N -> n)

  /**
    * TODO - should be temporary
    * @param default - what value to use when encountering an "optional" conn
    * @return
    */
  def toTile2d(default: Int): Tile2d = {
    def f(conn: Int) = if (conn == TileSpec.ConnOptional) { default } else { conn }
    Tile2d(
      f(e.conn),
      f(s.conn),
      f(w.conn),
      f(n.conn)
    )
  }

  def toOneWayTile2d(default: Int): Tile2d = {

    val zones = allSides.collect { case (_, side) if side.conn == TileSpec.ConnRequired => side.plotZone}.flatten
    val minZone = zones.min
    val maxZone = zones.max
    // val minZone = allSides.filter(_._2.conn == TileSpec.ConnRequired).values.flatMap(_.plotZone).min
    // val maxZone = allSides.filter(_._2.conn == TileSpec.ConnRequired).values.flatMap(_.plotZone).max
    require(minZone + 1 == maxZone, s"${toString} min=${minZone} max=${maxZone}")
    val maxHeading = allSides.collectFirst{ case (heading,side) if side.plotZone == Some(maxZone)  => heading }.get
    toTile2d(default).withSide(maxHeading, TileSpec.SpecialOneWayVal)
  }
}

object TileSpec {
  /** Must have a connection that allows player travel on this side */
  val ConnRequired = 1
  /** Must NOT have a connection that allows player travel on this side */
  val ConnBlocked = 0
  /** No requirement about having a connection or not */
  val ConnOptional = -1

  val SpecialOneWayVal = 2

  def apply(s: Side): TileSpec = TileSpec(s, s, s, s)
}

/**
  *
  * @param conn whether a connection that alows player travel is required, blocked, etc
  * @param plotZone number indicating how early or late in the level that area is (goes from 0 to 3)
  */
case class Side(
  conn: Int,
  plotZone: Option[Int]
) {

}
