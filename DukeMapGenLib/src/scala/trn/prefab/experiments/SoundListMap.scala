package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, Wall, Map => DMap}

import scala.collection.JavaConverters._
import trn.MapImplicits._
import trn.duke.{MusicSFXList, TextureList}


object SoundMapBuilder {

  def westConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.WestConnector).asInstanceOf[RedwallConnector]
  def eastConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.EastConnector).asInstanceOf[RedwallConnector]
  def westConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.WestConnector).asInstanceOf[RedwallConnector]
  def eastConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.EastConnector).asInstanceOf[RedwallConnector]

  def translateToTopMiddle(sg: SectorGroup): PointXYZ ={
    sg.boundingBox.topLeft.translateTo(new PointXY(0, DMap.MIN_Y)).withZ(0);
  }
}

class SoundMapBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {

  var currentPsg: Option[PastedSectorGroup] = None


  val leftHall = palette.getSectorGroup(2)
  val pastedMainHall = pasteSectorGroup(leftHall, SoundMapBuilder.translateToTopMiddle(leftHall))
  var row = 1 // 1-based because hitag 0 means no connector id

  var currentPsgLeft: Option[PastedSectorGroup] = None
  var rowLeft = 1

  // TODO - this should probably become part of MapBuilder
  def joinWalls(c1: Connector, c2: Connector): Unit = {
    // TODO - this should throw if the walls are already joined!
    if(c1 == null || c2 == null) throw new IllegalArgumentException
    c1.asInstanceOf[RedwallConnector].linkConnectors(outMap, c2.asInstanceOf[RedwallConnector])
  }

  def getRowStart(row: Int): RedwallConnector  ={
    if(row < 0) throw new IllegalArgumentException
    if(row <= 5){
      pastedMainHall.getRedwallConnectorsById(row).get(0)
    }else{
      throw new IllegalArgumentException(s"row ${row} not supported yet")
    }
  }
  def getLeftRowStart(row: Int): RedwallConnector = {
    if(row < 0) throw new IllegalArgumentException
    if(row <= 5){
      pastedMainHall.getRedwallConnectorsById(row + 10).get(0)
    }else{
      throw new IllegalArgumentException(s"left row ${row} not supported yet")
    }
  }

  def addRoom(room: SectorGroup): Unit = {
    val wconn = SoundMapBuilder.westConnector(room)
    val econn = currentPsg match {
      case None => getRowStart(row)
      case Some(psg) =>
        val econn = SoundMapBuilder.eastConnector(psg)
        if(econn.getAnchorPoint.x < DMap.MAX_X - (1024 * 5)){
          econn
        }else{
          row += 1 // need a new row
          getRowStart(row)
        }
    } // match
    val cdelta = wconn.getTransformTo(econn)
    val pastedRoom = pasteSectorGroup(room, cdelta)
    joinWalls(econn, SoundMapBuilder.westConnector(pastedRoom))
    currentPsg = Some(pastedRoom)
  }

  def addLeft(room: SectorGroup): Unit = {
    val wconn = currentPsgLeft match {
      case None => getLeftRowStart(rowLeft)
      case Some(psg) =>
        val wconn = SoundMapBuilder.westConnector(psg)
        if(wconn.getAnchorPoint.x > DMap.MIN_Y + (1025 * 5)){
          wconn
        }else{
          rowLeft += 1
          getLeftRowStart(rowLeft)
        }
    }
    val econn = SoundMapBuilder.eastConnector(room)
    val cdelta = econn.getTransformTo(wconn)
    val pastedRoom = pasteSectorGroup(room, cdelta)
    joinWalls(wconn, SoundMapBuilder.eastConnector(pastedRoom))
    currentPsgLeft = Some(pastedRoom)
  }

}


object WallDetails {
  def apply(texture: Int, xRepeat: Option[Int], yRepeat: Option[Int], sounds: java.util.List[Integer]): WallDetails = {
    WallDetails(texture, xRepeat, yRepeat, sounds.asScala.map(_.toInt).toSet, 0)
  }
}
case class WallDetails(texture: Int, xRepeat: Option[Int], yRepeat: Option[Int], sounds: Set[Int], palette: Int) {
  def applyTo(w: Wall): Unit ={
    w.setTexture(texture)
    xRepeat.foreach(w.setXRepeat(_))
    yRepeat.foreach(w.setYRepeat(_))
    w.setPal(palette)
  }

  def matches(sound: Int): Boolean = sounds.contains(sound)
}

object SoundListMap {

  val FILENAME = "sound.map"

  def run(sourceMap: DMap): DMap = {

    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new SoundMapBuilder(DMap.createNew(), palette)

    def digitTex(digit: Int): Int = {
      if(digit < 0 || digit > 9) throw new IllegalArgumentException
      val zero = 2930
      return zero + digit
    }
    def isAlphaNum(tex: Int): Boolean = {
      tex >= 2930 && tex < 2966
    }

    // TODO - these should be fields of the WallDetails object and WallDetails should have a matches(wall) method.
    val DUKE_VOCALS: Set[Int] = MusicSFXList.DUKE_VOCALS.ALL.asScala.map(_.toInt).toSet
    val DUKE_NOISES: Set[Int] = MusicSFXList.DUKE_NOISES.ALL.asScala.map(_.toInt).toSet
    val ENEMIES: Set[Int] = MusicSFXList.ENEMIES.ALL.asScala.map(_.toInt).toSet

    val dukeVocals = WallDetails(1405, Some(5), Some(4), MusicSFXList.DUKE_VOCALS.ALL)
    val dukeNoises = WallDetails(1405, Some(5), Some(4), MusicSFXList.DUKE_NOISES.ALL)
    val doors = WallDetails(1173, None, None, MusicSFXList.DOOR_SOUNDS.ALL)
    val slimer = WallDetails(2375, None, None, MusicSFXList.ENEMIES.Slimer.ALL)
    val octabrain = WallDetails(1830, None, None, MusicSFXList.ENEMIES.Octabrain.ALL)
    val trooper = WallDetails(1690, None, None, MusicSFXList.ENEMIES.Trooper.ALL)
    val pigcop = WallDetails(2010, None, None, MusicSFXList.ENEMIES.Pigcop.ALL)
    val pigcopRecon = WallDetails(1961, None, None, MusicSFXList.ENEMIES.PigcopRecon.ALL)
    val enforcer = WallDetails(2125, None, None, MusicSFXList.ENEMIES.Enforcer.ALL)
    val drone = WallDetails(1880, None, None, MusicSFXList.ENEMIES.SentryDrone.ALL)
    val fatCommander = WallDetails(1920, None, None, MusicSFXList.ENEMIES.AssaultCommander.ALL);
    val bossEp1 = WallDetails(2660, None, None, MusicSFXList.ENEMIES.BossEp1.ALL)
    val bossEp2 = WallDetails(2770, None, None, MusicSFXList.ENEMIES.BossEp2.ALL)
    val bossEp3 = WallDetails(2710, None, None, MusicSFXList.ENEMIES.BossEp3.ALL)
    val secretLevel = WallDetails(142, None, None, Set(MusicSFXList.SECRET_LEVEL), 14)
    val weapons = WallDetails(2614, None, None, MusicSFXList.WEAPON_SOUNDS.ALL)
    val inventory = WallDetails(2614, None, None, MusicSFXList.INVENTORY.ALL)

    // TODO - check boss2, boss3, secret level, weapons/inventory, dukeNoises

    val labels = Seq(dukeVocals, dukeNoises, doors, slimer, octabrain, trooper, pigcop, pigcopRecon, enforcer, drone, fatCommander, bossEp1,
      bossEp2, bossEp3, secretLevel, weapons, inventory)


    def makeRoom(sound: Int): SectorGroup ={
      if(sound < 0) throw new IllegalArgumentException
      val room = palette.getSectorGroup(3).copy()
      room.getMap.allSprites.foreach { s =>
        if(s.getTexture == TextureList.TOUCHPLATE || s.getTexture == TextureList.ACTIVATOR || s.getTexture == TextureList.MUSIC_AND_SFX) {
          s.setLotag(sound)
        }else if(isAlphaNum(s.getTexture)){ // red letters/numbers
          val radix = s.getLotag
          val digit = (sound % (radix * 10)) / radix
          s.setTexture(digitTex(digit))
        }
      }
      val wall = room.getMap.allWalls.filter(_.getLotag == 2).head
      labels.foreach{ label =>
        if(label.matches(sound)){
          label.applyTo(wall)
        }
      }
      room
    }


    // TODO - useful:  http://infosuite.duke4.net/index.php?page=references_sound_list
    def getAllSounds: Set[Int] = {

      val missingSounds1: Set[Int] = Set[Int](23, 24, 31, 32, 44, 45, 46, 47, 52, 53, 54, 55)
      val soundGroup1: Set[Int] = (1 to 56).toSet -- missingSounds1

      val missingSounds2: Set[Int] = Set(77)
      val soundGroup2: Set[Int] = Set(61, 64) ++ (67 to 79).toSet -- missingSounds2

      val missingSounds3: Set[Int] = Set(94)
      val bossSounds = (96 to 105).toSet
      val soundGroup3: Set[Int] = (81 to 95).toSet ++ bossSounds -- missingSounds3

      val missingSounds4 = Set(134, 152, 161, 162, 176, 182)
      val soundGroup4: Set[Int] = (106 to 144).toSet ++ (148 to 308) -- missingSounds4
      soundGroup1 ++ soundGroup2 ++ soundGroup3 ++ soundGroup4
    }

    val allSounds = getAllSounds

    // TODO - add DUKE_VOCALS back in

    val ITEMS = (weapons.sounds ++ inventory.sounds).toSeq.sorted

    val rightSounds = allSounds -- DUKE_VOCALS -- ENEMIES -- DUKE_NOISES -- ITEMS
    for(i <- rightSounds.toSeq.sorted){
      builder.addRoom(makeRoom(i))
    }

    val leftSounds: Seq[Int] = DUKE_NOISES.toSeq.sorted ++ ENEMIES.toSeq.sorted ++ ITEMS
    for(i <- leftSounds){
      builder.addLeft(makeRoom(i))
    }

    println(s"Sector count: ${builder.outMap.getSectorCount}")

    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }
}
