package trn.prefab.experiments.huntwumpus

import trn.RandomX
import trn.prefab.experiments.huntwumpus.WumpusWorld.{getNextRoom, getRoomName}
import trn.prefab.{RandomItemMarker, SectorGroup, Item, PastedSectorGroup, Marker}

class WumpusRoom(roomId: Int, sg: SectorGroup) {

  def getSG(random: RandomX): SectorGroup = {
    val sg2 = if(WumpusWorld.Flipped.contains(roomId)) {
      sg.rotate180
    } else {
      sg
    }

    // The autotext mutates it, so foldLeft is pointless
    // WumpusWorld.Exits.foldLeft(sg2){(x: SectorGroup, exitId) =>
    //   val nextRoomId = getNextRoom(roomId, exitId)
    //   x.getFirstAutoTextById(exitId).foreach(_.appendText(getRoomName(nextRoomId), x))
    //   x
    // }
    WumpusWorld.Exits.foreach { exitId =>
      val nextRoomId = getNextRoom(roomId, exitId)
      sg2.getFirstAutoTextById(exitId).foreach(_.appendText(getRoomName(nextRoomId), sg2))
    }


    sg2.findFirstMarker(Marker.Lotags.RANDOM_ITEM).foreach{s =>
      RandomItemMarker.writeTo(random.randomElement(WumpusRoom.Items).tex, s)
    }

    sg2
  }

}

object WumpusRoom {
  private val Items = Seq(
    Item.Medkit,
    Item.Steroids,
    Item.Scuba,
    Item.Jetpack,
    Item.Nightvision,
    Item.Boots,
    Item.HoloDuke,
    Item.AtomicHealth,
  )


  def apply(roomId: Int, sg: SectorGroup): WumpusRoom = new WumpusRoom(roomId, sg.copy())
}