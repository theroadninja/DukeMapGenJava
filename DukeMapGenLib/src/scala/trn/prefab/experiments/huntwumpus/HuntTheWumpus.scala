package trn.prefab.experiments.huntwumpus

import trn.prefab.experiments.huntwumpus.WumpusWorld.getExitIdTo
import trn.{BuildConstants, PointXY, RandomX, HardcodedConfig, ScalaMapLoader, Map => DMap}
import trn.prefab.{MapWriter, TeleportConnector, DukeConfig, GameConfig, SpriteLogicException}
import trn.prefab.experiments.{BaseExperiment, ExpUtil}
import trn.prefab.layout.GridLayout


case class WumpusInput(pent1: DMap)


object HuntTheWumpus {

  def main(args: Array[String]): Unit = {

    val input = WumpusInput(BaseExperiment.getMapLoader().load("prefab/pent/pentagon1.map"))
    val output = tryRun(BaseExperiment.gameConfig, input)
    ExpUtil.write(output)

  }

  def tryRun(gameCfg: GameConfig, input: WumpusInput): DMap = {
    val writer = MapWriter(gameCfg)
    try {
      val random = RandomX()
      run(gameCfg, random, writer, input)
    } catch {
      case e: SpriteLogicException => {
        e.printStackTrace()
        ExpUtil.finish(writer, removeMarkers = false, errorOnWarnings = false)
        writer.outMap
      }
    }
  }

  def run(gameCfg: GameConfig, random: RandomX, writer: MapWriter, input: WumpusInput): DMap = {

    val mainPalette = ScalaMapLoader.paletteFromMap(gameCfg, input.pent1)


    val EMPTY = 3
    val RPG = 4
    val START = 5
    val WUMPUS = 6
    val AMBUSH = 7
    val ITEM = 8

    // val emptyRoom = mainPalette.getSG(3)
    // val rpgRoom = mainPalette.getSG(4)
    // val playerStart = mainPalette.getSG(5)
    // val wumpusRoom = mainPalette.getSG(6)
    // val ambushRoom = mainPalette.getSG(7)
    // val itemRoom = mainPalette.getSG(8)



    val rooms = random.shuffle(Seq(START, WUMPUS, RPG, AMBUSH, ITEM, ITEM, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY, EMPTY)).toSeq
    require(rooms.size == 12)



    val layout = new GridLayout(BuildConstants.MapBounds, 5, 5)

    val allRooms = rooms.zipWithIndex.map { case (roomType, index) =>

      val roomId = index + 1
      val room = WumpusRoom(roomId, mainPalette.getSG(roomType))

      val psg = writer.tryPasteInside(room.getSG(random), layout.bbForIndex(index)).get
      roomId -> psg
    }.toMap

    // connect teleporters
    (1 to 12).foreach { roomId =>
      WumpusWorld.Exits.foreach { exitId =>
        val otherRoomId = WumpusWorld.getNextRoom(roomId, exitId)
        if(roomId < otherRoomId){  // <-- hack to prevent connecting twice
          val connA = allRooms(roomId).getTeleportConnector(exitId)
          val connB = allRooms(otherRoomId).getTeleportConnector(getExitIdTo(otherRoomId, roomId))
          writer.linkTeleporters(connA, connB)
        }

      }

    }



    ExpUtil.finish(writer)
    writer.outMap

  }

}
