package trn.prefab.experiments

import trn.MapImplicits._
import trn.duke.{MusicSFXList, TextureList}
import trn.prefab._
import trn.{PointXY, PointXYZ, Wall, Map => DMap}

import scala.collection.JavaConverters._


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
  val mainHall = palette.getSectorGroup(2)
  val pastedMainHall = pasteSectorGroup(mainHall, SoundMapBuilder.translateToTopMiddle(mainHall))

  var currentPsgRight: Option[PastedSectorGroup] = None
  var rowRight = 1 // 1-based because hitag 0 means no connector id

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
    val econn = currentPsgRight match {
      case None => getRowStart(rowRight)
      case Some(psg) =>
        val econn = SoundMapBuilder.eastConnector(psg)
        if(econn.getAnchorPoint.x < DMap.MAX_X - (1024 * 5)){
          econn
        }else{
          rowRight += 1 // need a new row
          getRowStart(rowRight)
        }
    } // match
    val cdelta = wconn.getTransformTo(econn)
    val pastedRoom = pasteSectorGroup(room, cdelta)
    joinWalls(econn, SoundMapBuilder.westConnector(pastedRoom))
    currentPsgRight = Some(pastedRoom)
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
  def apply(texture: Int, sounds: java.util.List[Integer]): WallDetails = apply(texture, None, None, sounds)
}

/**
  * Stores settings we want to apply to a certain wall to visually identify the type of sound.
  * Maybe this would make some kind of "wall prefab" ?  Maybe with intelligence to figure out xRepeat
  */
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

  def makeRoom(sound: Int, palette: PrefabPalette, labels: Seq[WallDetails]): SectorGroup ={
    if(sound < 0) throw new IllegalArgumentException
    val room = palette.getSectorGroup(3).copy()
    room.getMap.allSprites.foreach { s =>
      if(s.getTexture == TextureList.TOUCHPLATE || s.getTexture == TextureList.ACTIVATOR || s.getTexture == TextureList.MUSIC_AND_SFX) {
        s.setLotag(sound)
      }
    }
    room.getAutoTextById(1).appendText("%03d".format(sound), room)
    val wall = room.getMap.allWalls.filter(_.getLotag == 2).head
    labels.foreach{ label =>
      if(label.matches(sound)){
        label.applyTo(wall)
      }
    }
    room
  }

  def run(sourceMap: DMap): DMap = {
    val dukeVocals = WallDetails(1405, Some(5), Some(4), MusicSFXList.DUKE_VOCALS.ALL)
    val dukeNoises = WallDetails(1405, Some(5), Some(4), MusicSFXList.DUKE_NOISES.ALL)
    val doors = WallDetails(1173, MusicSFXList.DOOR_SOUNDS.ALL)
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

    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new SoundMapBuilder(DMap.createNew(), palette)

    val DUKE_VOCALS: Set[Int] = MusicSFXList.DUKE_VOCALS.ALL.asScala.map(_.toInt).toSet
    val DUKE_NOISES: Set[Int] = MusicSFXList.DUKE_NOISES.ALL.asScala.map(_.toInt).toSet
    val ENEMIES: Set[Int] = MusicSFXList.ENEMIES.ALL.asScala.map(_.toInt).toSet

    val labels = Seq(dukeVocals, dukeNoises, doors, slimer, octabrain, trooper, pigcop, pigcopRecon, enforcer, drone,
      fatCommander, bossEp1, bossEp2, bossEp3, secretLevel, weapons, inventory)

    // TODO - add DUKE_VOCALS back in
    val allSounds = MusicSFXList.ALL.asScala.map(_.toInt).toSet
    val ITEMS = (weapons.sounds ++ inventory.sounds).toSeq.sorted

    val rightSounds = allSounds -- DUKE_VOCALS -- ENEMIES -- DUKE_NOISES -- ITEMS
    for(i <- rightSounds.toSeq.sorted){
      builder.addRoom(makeRoom(i, palette, labels))
    }

    val leftSounds: Seq[Int] = ITEMS ++ DUKE_NOISES.toSeq.sorted ++ ENEMIES.toSeq.sorted
    for(i <- leftSounds){
      builder.addLeft(makeRoom(i, palette, labels))
    }

    println(s"Sector count: ${builder.outMap.getSectorCount}")
    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }
}
