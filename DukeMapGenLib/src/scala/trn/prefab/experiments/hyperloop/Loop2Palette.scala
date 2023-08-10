package trn.prefab.experiments.hyperloop

import trn.RandomX
import trn.prefab.{PrefabPalette, GameConfig, SectorGroup}

class Loop2Palette (
  gameCfg: GameConfig,
  random: RandomX,
  palette: PrefabPalette,
) extends HyperLoopPalette {
  /** only exist to show us how big the core area is */
  def coreSizeGroup: SectorGroup = palette.getSG(1)
  def innerSizeGroup: SectorGroup = palette.getSG(2)
  def midSizeGroup: SectorGroup = palette.getSG(3)

  val innerBlank: SectorGroup = palette.getSG(4)
  val midBlank: SectorGroup = palette.getSG(5)
  val outerBlank: SectorGroup = palette.getSG(6)
  val innerBlankDiag: SectorGroup = palette.getSG(7)
  val midBlankDiag: SectorGroup = palette.getSG(8)
  val outerBlankDiag: SectorGroup = palette.getSG(9)

  val outerHallway: SectorGroup = palette.getSG(10)
  val outerHallwayDiag: SectorGroup = palette.getSG(11)
  val playerStart: SectorGroup = palette.getSG(12)

  val innerEnd: SectorGroup = palette.getSG(13)
  val innerEndDiag: SectorGroup = palette.getSG(14)

  /** outer section with 3 different spots for the standard space door */
  val outerDoor150:  Seq[SectorGroup] = Seq(  // the connectors all have different angles though
    palette.getSG(15),
    palette.getSG(16),
    palette.getSG(17),
  )
  val itemRoomForOuterDoor150: SectorGroup = palette.getSG(18)

  val outerDoor150Diag: Seq[SectorGroup] = Seq(
    palette.getSG(19),
    palette.getSG(20),
  )
  val itemRoomForOuterDoor150Diag: SectorGroup = palette.getSG(21)

  // TODO val outerDoor150Diag = ...



  // def getBlank(ringIndex: Int, angleType: Int): SectorGroup = { // TODO ?
  def getInnerBlank(angleType: Int): SectorGroup = angleType match {
    case RingLayout.AXIS => innerBlank
    case RingLayout.DIAG => innerBlankDiag
  }

  def getMidBlank(angleType: Int): SectorGroup = angleType match {
    case RingLayout.AXIS => midBlank
    case RingLayout.DIAG => midBlankDiag
  }

  def getOuterBlank(angleType: Int): SectorGroup = angleType match {
    case RingLayout.AXIS => outerBlank
    case RingLayout.DIAG => outerBlankDiag
  }

  def getPlayerStartOuter(angleType: Int): SectorGroup = angleType match {
    case RingLayout.AXIS => {
      outerHallway.withGroupAttachedById(gameCfg, 123, playerStart, playerStart.allRedwallConnectors.head)
    }
    case RingLayout.DIAG => {
      outerHallwayDiag.withGroupAttachedById(gameCfg, 123, playerStart, playerStart.allRedwallConnectors.head)
    }
  }

  def getEnd(angleType: Int): SectorGroup = angleType match {
    case RingLayout.AXIS => innerEnd
    case RingLayout.DIAG => innerEndDiag
  }

  def getItemOuter(angleType: Int): SectorGroup = angleType match {
    case RingLayout.AXIS => {
      val outer = random.randomElement(outerDoor150)
      val conn = outer.getRedwallConnector(123)
      val otherConn = itemRoomForOuterDoor150.allUnlinkedRedwallConns.filter(c => c.couldMatch(conn)).head
      outer.withGroupAttached(gameCfg, conn, itemRoomForOuterDoor150, otherConn)
    }
    case RingLayout.DIAG => {
      val outer = random.randomElement(outerDoor150Diag)
      val conn = outer.getRedwallConnector(123)
      val otherConn = itemRoomForOuterDoor150Diag.allUnlinkedRedwallConns.filter(c => c.couldMatch(conn)).head
      outer.withGroupAttached(gameCfg, conn, itemRoomForOuterDoor150Diag, otherConn)
    }
  }


}
