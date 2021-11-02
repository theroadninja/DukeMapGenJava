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
}

object TileSpec {
  /** Must have a connection that allows player travel on this side */
  val ConnRequired = 1
  /** Must NOT have a connection that allows player travel on this side */
  val ConnBlocked = 0
  /** No requirement about having a connection or not */
  val ConnOptional = -1

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
