package trn.prefab.experiments.hyperloop

import trn.RandomX
import trn.duke.{PaletteList, TextureList}
import trn.prefab.experiments.hyperloop.RingLayout.{DIAG, AXIS}
import trn.prefab.{SectorGroup, PrefabPalette, GameConfig}

/**
  * Represents an abstract section of a ring that can be pasted at a position,
  * e.g. "key room 1" or "shuttle bay" and tries to hide the fact that different sector
  * groups might be needed for different angle types.
  */
trait Section {

  def has(ringType: Int, angleType: Int): Boolean

  def hasRingIndex(ringIndex: Int): Boolean = has(ringIndex, AXIS) && has(ringIndex, DIAG)

  def get(ringType: Int, angleType: Int, threat: Int): Option[SectorGroup]

  final def apply(ringType: Int, angleType: Int, threat: Int = 0): SectorGroup = get(ringType, angleType, threat).get

}

case class SimpleSection (
  innerAxis: Option[SectorGroup],
  innerDiag: Option[SectorGroup],
  outerAxis: Option[SectorGroup],
  outerDiag: Option[SectorGroup],
  threatModifier: (SectorGroup, Int) => SectorGroup = SimpleSection.defaultThreatModifier,
) extends Section {
  val groups = Map(
    RingIndex.InnerRing -> Map(
      AXIS -> innerAxis,
      DIAG -> innerDiag,
    ),
    RingIndex.OuterRing -> Map(
      AXIS -> outerAxis,
      DIAG -> outerDiag,
    ),
  )

  def has(ringType: Int, angleType: Int): Boolean = get(ringType, angleType).isDefined

  def get(ringType: Int, angleType: Int, threat: Int = 0): Option[SectorGroup] = {
    groups(ringType)(angleType).map(sg => threatModifier(sg, threat))
  }
}

object SimpleSection {
  def defaultThreatModifier(sg: SectorGroup, threat: Int): SectorGroup = sg

  def outer(axis: SectorGroup, diag: SectorGroup): SimpleSection = apply(None, None, Some(axis), Some(diag))

  def inner(axis: SectorGroup, diag: SectorGroup): SimpleSection = apply(Some(axis), Some(diag), None, None)
}

object Loop2Palette {

  def makeRed(gameCfg: GameConfig, sg: SectorGroup): SectorGroup = {
    val textures = Seq(
      TextureList.ForceFields.W_FORCEFIELD,
      276, // blue boxes I use around the force field
    )
    val color = PaletteList.RED

    val cp = sg.withKeyLockColor(gameCfg, PaletteList.KEYCARD_RED)
    for (i <- 0 until cp.map.getWallCount) {
      val wall = cp.map.getWall(i)
      if (textures.contains(wall.getTex) || textures.contains(wall.getMaskTex)) {
        wall.setPal(color)
      }
    }
    for(i <- 0 until cp.map.getSectorCount){
      val sector = cp.map.getSector(i)
      if(textures.contains(sector.getFloorTexture)){
        sector.setFloorPalette(color)
      }
      if(textures.contains(sector.getCeilingTexture)){
        sector.setCeilingPalette(color)
      }
    }
    cp

  }
}

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
  val playerStartPartial: SectorGroup = palette.getSG(12)

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

  val forceField: SectorGroup = palette.getSG(22)
  val forceFieldDiag: SectorGroup = palette.getSG(23)

  val droneDoors = SimpleSection(
    Some(palette.getSG(24)),
    Some(palette.getSG(25)),
    Some(palette.getSG(26)),
    Some(palette.getSG(27)),
    (sg: SectorGroup, threat: Int) => {
      if(threat == ThreatLevel.Low){
        val cp = sg.copy()
        val drones = cp.allSpritesWithIndex.filter(s => s._1.getTex == TextureList.Enemies.DRONE)
        require(drones.size > 2)
        cp.map.deleteSprite(drones(0)._2)
        cp.map.deleteSprite(drones(1)._2)
        cp
      }else{
        sg
      }
    },
  )

  val armoryInner: SectorGroup = palette.getSG(28) // red circular "armory" thing with items
  val armoryInnerDiag: SectorGroup = palette.getSG(39)
  val armoryInnerSection = SimpleSection.inner(armoryInner, armoryInnerDiag)

  val suppliesInner: SectorGroup = palette.getSG(29) // blue circular "supplies" thing with items
  val suppliesInnerAxis: SectorGroup = palette.getSG(40)
  val suppliesInnerSection = SimpleSection.inner(suppliesInnerAxis, suppliesInner)

  val tripmineInner: SectorGroup = palette.getSG(30)
  val tripmineOuter: SectorGroup = palette.getSG(31)

  val tripmineInnerDiag: SectorGroup = palette.getSG(32)
  val tripmineOuterDiag: SectorGroup = palette.getSG(33)
  val tripBombs = SimpleSection(
    innerAxis=Some(palette.getSG(30)),
    innerDiag=Some(palette.getSG(32)),
    outerAxis=Some(palette.getSG(31)),
    outerDiag=Some(palette.getSG(33)),
  )

  val screensOuter: SectorGroup = palette.getSG(34) // decor
  val screensOuterDiag: SectorGroup = palette.getSG(35) // decor
  val screensOuterDecor = SimpleSection.outer(
    screensOuter,
    screensOuterDiag,
  )

  val outerConferenceRoom: SectorGroup = palette.getSG(36) // item area

  // separate area with screens
  val screenAreaAxis: SectorGroup = palette.getSG(37)
  val screenAreaDiag: SectorGroup = palette.getSG(38) // only matches door of group 19

  val bigWindowsOuterAxis: SectorGroup = palette.getSG(41)

  // TODO next=42

  // TODO dead end, as an alternative to one of the force fields

  def getTest(ringIndex: Int, angleType: Int): SectorGroup = (ringIndex, angleType) match {
    case (RingIndex.InnerRing, RingLayout.AXIS) => tripmineInner
    case (RingIndex.InnerRing, RingLayout.DIAG) => tripmineInnerDiag
    case (RingIndex.OuterRing, RingLayout.AXIS) => bigWindowsOuterAxis
    case (RingIndex.OuterRing, RingLayout.DIAG) => screenArea(RingLayout.DIAG)
  }

  // def getTestInner(angleType: Int): SectorGroup = angleType match {
  //   case RingLayout.AXIS => tripmineInner // armoryInner
  //   case RingLayout.DIAG => tripmineInnerDiag // suppliesInner
  // }

  // def getTestOuter(angleType: Int): SectorGroup = angleType match {
  //   case RingLayout.AXIS => screenArea(RingLayout.AXIS)
  //   case RingLayout.DIAG => screenArea(RingLayout.DIAG)
  // }

  def screenArea(angleType: Int): SectorGroup = angleType match {
    case RingLayout.AXIS => {
      val outer = palette.getSG(16)
      outer.withGroupAttachedById(gameCfg, 123, screenAreaAxis, screenAreaAxis.allRedwallConnectors.head)
    }
    case RingLayout.DIAG => {
      val outer = palette.getSG(19)
      outer.withGroupAttachedById(gameCfg, 123, screenAreaDiag, screenAreaDiag.allRedwallConnectors.head)
    }
  }

  // // def getBlank(ringIndex: Int, angleType: Int): SectorGroup = { // TODO ?
  // def getInnerBlank(angleType: Int): SectorGroup = angleType match {
  //   case RingLayout.AXIS => innerBlank
  //   case RingLayout.DIAG => innerBlankDiag
  // }

  def getMidBlank(angleType: Int): SectorGroup = angleType match {
    case RingLayout.AXIS => midBlank
    case RingLayout.DIAG => midBlankDiag
  }

  // def getOuterBlank(angleType: Int): SectorGroup = angleType match {
  //   case RingLayout.AXIS => outerBlank
  //   case RingLayout.DIAG => outerBlankDiag
  // }

  def getBlank(ringIndex: Int, angleType: Int): SectorGroup = (ringIndex, angleType) match {
    case (RingIndex.InnerRing, RingLayout.AXIS) => innerBlank
    case (RingIndex.InnerRing, RingLayout.DIAG) => innerBlankDiag
    case (RingIndex.OuterRing, RingLayout.AXIS) => outerBlank
    case (RingIndex.OuterRing, RingLayout.DIAG) => outerBlankDiag
  }

  val playerStart = SimpleSection.outer(
    axis=outerHallway.withGroupAttachedById(gameCfg, 123, playerStartPartial, playerStartPartial.allRedwallConnectors.head),
    diag=outerHallwayDiag.withGroupAttachedById(gameCfg, 123, playerStartPartial, playerStartPartial.allRedwallConnectors.head)
  )

  def getEnd(angleType: Int, keyLockColor: Int): SectorGroup = angleType match {
    case RingLayout.AXIS => innerEnd.withKeyLockColor(gameCfg, keyLockColor)
    case RingLayout.DIAG => innerEndDiag.withKeyLockColor(gameCfg, keyLockColor)
  }

  def getFanOuter(angleType: Int, item: Item): SectorGroup = angleType match {
    case RingLayout.AXIS => {
      val outer = random.randomElement(outerDoor150)
      val conn = outer.getRedwallConnector(123)
      val otherConn = itemRoomForOuterDoor150.allUnlinkedRedwallConns.filter(c => c.couldMatch(conn)).head
      outer.withGroupAttached(gameCfg, conn, itemRoomForOuterDoor150, otherConn)
        .withItem(item.tex, item.pal)
    }
    case RingLayout.DIAG => {
      val outer = random.randomElement(outerDoor150Diag)
      val conn = outer.getRedwallConnector(123)
      val otherConn = itemRoomForOuterDoor150Diag.allUnlinkedRedwallConns.filter(c => c.couldMatch(conn)).head
      outer.withGroupAttached(gameCfg, conn, itemRoomForOuterDoor150Diag, otherConn)
        .withItem(item.tex, item.pal)
    }
  }

  def getWeaponOuter(angleType: Int, item: Item): SectorGroup = angleType match {
    case RingLayout.AXIS => random.randomElement(Seq(1, 2, 3)) match {
      case 1 => getFanOuter(angleType, item)
      case 2 => screenArea(angleType).withItem(item.tex, item.pal)
      case 3 => outerConferenceRoom.withItem(item.tex, item.pal)
    }
    case RingLayout.DIAG => random.randomElement(Seq(1, 2)) match {
      case 1 => getFanOuter(angleType, item)
      case 2 => screenArea(angleType).withItem(item.tex, item.pal)
    }
  }

  // def getWeaponInner(angleType: Int, item: Item): SectorGroup = angleType match {
  //   case RingLayout.AXIS => random.randomElement(Seq(1)) match {
  //     case 1 => armoryInner.withItem(item.tex, item.pal)
  //   }
  //   case RingLayout.DIAG => random.randomElement(Seq(1)) match {
  //     case 1 => armoryInnerDiag.withItem(item.tex, item.pal)
  //     // case 1 => suppliesInner.withItem(item.tex, item.pal)
  //   }
  // }

  def getForceField(angleType: Int): SectorGroup = angleType match {
    case RingLayout.AXIS => forceField
    case RingLayout.DIAG => forceFieldDiag
  }

  // TODO put more things here (and create a check function that takes a Section ?)
  HyperLoopParser.checkInner(droneDoors(RingPrinter2.InnerRing, RingLayout.AXIS))
  HyperLoopParser.checkInner(droneDoors(RingPrinter2.InnerRing, RingLayout.DIAG))
  HyperLoopParser.checkOuter(droneDoors(RingPrinter2.OuterRing, RingLayout.AXIS))
  HyperLoopParser.checkOuter(droneDoors(RingPrinter2.OuterRing, RingLayout.DIAG))

}
