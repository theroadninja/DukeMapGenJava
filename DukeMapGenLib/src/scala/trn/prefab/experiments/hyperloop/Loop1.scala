package trn.prefab.experiments.hyperloop

import trn.{ISpriteFilter, Sprite, RandomX}
import trn.duke.{PaletteList, TextureList}
import trn.prefab.experiments.hyperloop.RingLayout.{DIAG, AXIS}
import trn.prefab.{CompassWriter, DukeConfig, GameConfig, SectorGroup, PrefabPalette}

import scala.collection.JavaConverters._

/**
  * Loop 1 tilesets.  Tilesets are just sets of textures make to line up together, to make it seem like they
  * are meant to be a part of the same area of the map.
  */
object L1Tileset {
  /** "unsecured" area based around the white hull space texture with the brown stripe; tex 233 */
  val Clear = 0
  val Secure = 1
}

class Loop1(
  gameCfg: GameConfig,
  random: RandomX,
  palette: PrefabPalette,
  spacePalette: PrefabPalette,
) {

  val core = palette.getSG(1)
  val innerSurpriseGuns = palette.getSG(2)
  require(innerSurpriseGuns.props.switchesRequested.size == 1)

  val outerStart = palette.getSG(4)
  val outerLightPulse = palette.getSG(5) // first attempt id=3
  val outerLightsDiag = palette.getSG(6)
  val outerMirror = palette.getSG(7) // https://infosuite.duke4.net/index.php?page=ae_walls_b2

  val midForceFieldDiag = palette.getSG(8)
  val outerForceFieldDiag = palette.getSG(9)
  val innerForceFieldDiag = palette.getSG(10)
  //
  // standard groups
  //
  val innerE = palette.getSG(11)
  val midE = palette.getSG(12)
  val outerE = palette.getSG(13)

  val innerSE = palette.getSG(14)
  val midSE = palette.getSG(15)
  val outerSE = palette.getSG(16)

  // just an outer diag that makes it look like the adjacent (on the anticlockwise side) straigt wall
  // is extended farther
  val outerDiagStraightExtender = palette.getSG(17)
  val outerDoor1 = palette.getSG(18) // has a 1024-wide space door in the outer wall
  val powerUpRoom = palette.getSG(19) // not a ring segment!

  val innerEndDiag = palette.getSG(20)

  val midBigDoor = palette.getSG(21)
  val innerBlocked = palette.getSG(22) // doesnt let you walk through the inner area
  val outerBlocked = palette.getSG(23)

  val outerKeyDoor = palette.getSG(24) // conn 2048 wide
  val doorAdapter = palette.getSG(25) // has a 1024 and a 3072 connection

  val outerLightsWithGap = palette.getSG(26)
  val outerForceFieldDiagWithLights = palette.getSG(27)

  val midForceFieldDiag2 = palette.getSG(28)

  val outerForceFieldDiagLightsNoSwitch = palette.getSG(29)
  val innerForceFieldDiagB = palette.getSG(30)

  //31 - taken
  //32 - taken

  /** outer AXIS piece with a 2048 connector (but no door) on the outside, conn id 123 */
  val outerLightsDoorHole2048 = palette.getSG(33)
  val simpleItemRoom = palette.getSG(34)

  //35 taken - outer item area (clear)
  //36 taken - outer item area
  //37 taken - outer item area
  //38 taken - outer item area

  def innerDefaultTileset(angleType: Int): SectorGroup = angleType match {
    case RingLayout.AXIS => innerE
    case RingLayout.DIAG => innerSE
  }

  def innerLightsTileset(angleType: Int): SectorGroup = angleType match {
    case RingLayout.AXIS => palette.getSG(31)
    case RingLayout.DIAG => palette.getSG(32)
  }


  def outerLights(angleType: Int, red: Boolean = false): SectorGroup = {
    val sg = if(angleType == RingLayout.AXIS){
      outerLightPulse
      // outerLightsWithGap
    }else{
      outerLightsDiag
    }
    if(red){
      withRedCyclers(sg)
    }else{
      sg
    }
  }

  def withRedCyclers(sg: SectorGroup): SectorGroup = {
    val cp = sg.copy()
    val cyclerSpriteIds: Seq[Int] = cp.allSprites.filter(_.getTex == TextureList.CYCLER).map(_.getSectorId.toInt)
    cyclerSpriteIds.toSet.foreach { sectorId: Int =>
      cp.map.getAllSectorWallIds(sectorId).asScala.map(i => cp.map.getWall(i)).foreach { wall =>
        wall.setPal(PaletteList.RED)
      }
      val sector = cp.map.getSector(sectorId)
      sector.setFloorPalette(PaletteList.RED)
      sector.setCeilingPalette(PaletteList.RED)
    }
    cp

  }

  def defaultMid(angleType: Int): SectorGroup = if (angleType == RingLayout.AXIS) {
    midE
  } else {
    midSE
  }

  def defaultOuter(angleType: Int): SectorGroup = if(angleType == RingLayout.AXIS) {
    outerE
  }else{
    outerSE
  }

  /** diagonal section with force field stretching across the corridor */
  def forceFieldDiag: RingSection = {
    RingSection(RingLayout.DIAG, Some(innerForceFieldDiag), Some(midForceFieldDiag), Some(outerForceFieldDiag))
  }

  def forceFieldDiag2(red: Boolean = false): RingSection = {
    val outer = if(red){
      withRedCyclers(outerForceFieldDiagLightsNoSwitch)
    }else{
      outerForceFieldDiagLightsNoSwitch
    }
    val mid = if(red){
      val cp = midForceFieldDiag2.withKeyLockColor(gameCfg, PaletteList.KEYCARD_RED).copy()
      for (i <- 0 until cp.map.getWallCount) {
        val wall = cp.map.getWall(i)
        val ToReplace = Seq(663, 276)
        if (ToReplace.contains(wall.getTex) || ToReplace.contains(wall.getMaskTex)) {
          wall.setPal(PaletteList.RED)
        }
      }
      cp
    }else{
      midForceFieldDiag2
    }
    RingSection(RingLayout.DIAG, Some(innerForceFieldDiagB), Some(mid), Some(outer))
  }

  def sectionWithKey: RingSection = {
    val outer = outerKeyDoor.withGroupAttached(
      gameCfg,
      outerKeyDoor.getRedwallConnector(123),
      doorAdapter,
      doorAdapter.allRedwallConnectors.find(conn => conn.totalManhattanLength() == 3072).get,
    )
    RingSection(RingLayout.AXIS, None, None, Some(outer))
  }

  def outerRoomWithItem(tex: Int): SectorGroup = {
    val r = powerUpRoom.withItem(tex)
    val outerDoorWithKey = outerDoor1.withGroupAttached(
      gameCfg,
      outerDoor1.getRedwallConnector(150), // chose 150 b/c its the door tex in the conn
      r,
      r.getRedwallConnector(150),
    )
    outerDoorWithKey
  }

  def playerStartOuter(angleType: Int): SectorGroup = {
    if(angleType == RingLayout.AXIS){
      outerStart
    }else{
      ???
    }
  }

  def sectionBlockedByDoor(angleType: Int): RingSection = angleType match {
    case AXIS => RingSection(angleType, Some(innerBlocked), Some(midBigDoor), Some(outerBlocked))
    case DIAG => ???
  }

  def outerDoor(angleType: Int): SectorGroup = angleType match {
    case AXIS => {
      val spaceDoor2048 = spacePalette.getSG(1) // tex 155
      outerLightsDoorHole2048.withGroupAttachedById(gameCfg, 123, spaceDoor2048, spaceDoor2048.allRedwallConnectors.head)
    }
    case DIAG => ???
  }

  def outerKeyArea(angleType: Int, keyColor: Int): SectorGroup = angleType match {
    case AXIS => {
      val spaceDoor2048 = spacePalette.getSG(1) // tex 155
      val doorConn = CompassWriter.east(spaceDoor2048).get
      val r = simpleItemRoom.withItem(TextureList.Items.KEY, keyColor).withGroupAttachedById(gameCfg, 123, spaceDoor2048, doorConn)
      outerLightsDoorHole2048.withGroupAttachedById(gameCfg, 123, r, r.allUnlinkedRedwallConns.head)
    }
    case DIAG => ???
  }

  def outerItemArea(angleType: Int, tileset: Int): SectorGroup = (angleType, tileset) match {
    case (AXIS, L1Tileset.Clear) => palette.getSG(35)
    case (AXIS, L1Tileset.Secure) => {
      palette.getSG(37).withGroupAttachedById(gameCfg, 123, powerUpRoom, powerUpRoom.allRedwallConnectors.head)
    }
    case (DIAG, L1Tileset.Clear) => palette.getSG(36)
    case (DIAG, L1Tileset.Secure) => {
      palette.getSG(38).withGroupAttachedById(gameCfg, 123, powerUpRoom, powerUpRoom.allRedwallConnectors.head)
    }

  }
}
