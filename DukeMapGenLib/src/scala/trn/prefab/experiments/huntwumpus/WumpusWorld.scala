package trn.prefab.experiments.huntwumpus

object WumpusWorld {

  val EXIT1 = 1
  val EXIT2 = 2
  val EXIT3 = 3
  val EXIT4 = 4
  val EXIT5 = 5

  val Exits: Seq[Int] = Seq(EXIT1, EXIT2, EXIT3, EXIT4, EXIT5)

  val Flipped: Set[Int] = Set(5, 6, 7, 8, 10, 9)

  private val RoomNames = "ABCDEFGHIJKL"

  def getRoomName(roomId: Int): String = {
    RoomNames(roomId - 1).toString
  }

  /**
    *
    *   3  / \   4
    *    /     \
    *  /         \
    *  \         /
    * 2 \       / 5
    *    -------
    *       1
    */
  val roomGraph = Map(
    (1, EXIT1) -> 3,
    (1, EXIT2) -> 4,
    (1, EXIT3) -> 5,
    (1, EXIT4) -> 6,
    (1, EXIT5) -> 2,
    (2, EXIT1) -> 3,
    (2, EXIT2) -> 1,
    (2, EXIT3) -> 6,
    (2, EXIT4) -> 8,
    (2, EXIT5) -> 12,
    (3, EXIT1) -> 4,
    (3, EXIT2) -> 1,
    (3, EXIT3) -> 2,
    (3, EXIT4) -> 12,
    (3, EXIT5) -> 11,
    (4, EXIT1) -> 3,
    (4, EXIT2) -> 11,
    (4, EXIT3) -> 10,
    (4, EXIT4) -> 5,
    (4, EXIT5) -> 1,
    (12, EXIT1) -> 3,
    (12, EXIT2) -> 2,
    (12, EXIT3) -> 8,
    (12, EXIT4) -> 7,
    (12, EXIT5) -> 11,
    (11, EXIT1) -> 3,
    (11, EXIT2) -> 12,
    (11, EXIT3) -> 7,
    (11, EXIT4) -> 10,
    (11, EXIT5) -> 4,

    // these are "upside down"
    (5, EXIT1) -> 9,
    (5, EXIT2) -> 6,
    (5, EXIT3) -> 1,
    (5, EXIT4) -> 4,
    (5, EXIT5) -> 10,
    (6, EXIT1) -> 9,
    (6, EXIT2) -> 8,
    (6, EXIT3) -> 2,
    (6, EXIT4) -> 1,
    (6, EXIT5) -> 5,
    (8, EXIT1) -> 9,
    (8, EXIT2) -> 7,
    (8, EXIT3) -> 12,
    (8, EXIT4) -> 2,
    (8, EXIT5) -> 6,
    (7, EXIT1) -> 9,
    (7, EXIT2) -> 10,
    (7, EXIT3) -> 11,
    (7, EXIT4) -> 12,
    (7, EXIT5) -> 8,
    (10, EXIT1) -> 9,
    (10, EXIT2) -> 5,
    (10, EXIT3) -> 4,
    (10, EXIT4) -> 11,
    (10, EXIT5) -> 7,
    (9, EXIT1) -> 5,
    (9, EXIT2) -> 10,
    (9, EXIT3) -> 7,
    (9, EXIT4) -> 8,
    (9, EXIT5) -> 6,
  )

  def getNextRoom(roomId: Int, exitId: Int): Int = roomGraph((roomId, exitId))

  /** figure out which ExitId takes you from the first room to the second */
  def getExitIdTo(fromRoomId: Int, toRoomId: Int): Int = Exits.find { exitId =>
    roomGraph(fromRoomId, exitId) == toRoomId
  }.get


}
