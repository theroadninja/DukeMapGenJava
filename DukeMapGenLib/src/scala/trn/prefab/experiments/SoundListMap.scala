package trn.prefab.experiments

import trn.prefab._
import trn.{DukeConstants, Main, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, Map => DMap}

import scala.collection.JavaConverters._
import trn.MapImplicits._
import trn.duke.{MusicSFXList, TextureList}


object SoundMapBuilder {

  def westConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.WestConnector).asInstanceOf[RedwallConnector]
  def eastConnector(sg: SectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.EastConnector).asInstanceOf[RedwallConnector]
  def westConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.WestConnector).asInstanceOf[RedwallConnector]
  def eastConnector(sg: PastedSectorGroup): RedwallConnector = sg.findFirstConnector(SimpleConnector.EastConnector).asInstanceOf[RedwallConnector]


  def translateToTopLeft(sg: SectorGroup): PointXYZ ={
    sg.boundingBox.topLeft.translateTo(DMap.TOP_LEFT).withZ(0)
  }
}
class SoundMapBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {

  var currentPsg: Option[PastedSectorGroup] = None

  val leftHall = palette.getSectorGroup(2)
  val pastedLeftHall = pasteSectorGroup(leftHall, SoundMapBuilder.translateToTopLeft(leftHall))
  var row = 1 // 1-based because hitag 0 means no connector id


  // TODO - this should probably become part of MapBuilder
  def joinWalls(c1: Connector, c2: Connector): Unit = {
    // TODO - this should throw if the walls are already joined!
    if(c1 == null || c2 == null) throw new IllegalArgumentException
    c1.asInstanceOf[RedwallConnector].linkConnectors(outMap, c2.asInstanceOf[RedwallConnector])
  }

  def getRowStart(row: Int): RedwallConnector  ={
    if(row < 0) throw new IllegalArgumentException
    if(row <= 4){
      pastedLeftHall.getRedwallConnectorsById(row).get(0)
    }else{
      throw new IllegalArgumentException(s"row ${row} not supported yet")
    }
    // row match {
    //   case 1 => pastedLeftHall.getRedwallConnectorsById(row).get(0)
    //   case 2 => pastedLeftHall.getRedwallConnectorsById(row).get(0)
    //   case 3 => pastedLeftHall.getRedwallConnectorsById(row).get(0)
    //   case _ =>
    // }
  }

  def addRoom(room: SectorGroup): Unit ={
    currentPsg match {
      case None => {

        //val delta = room.boundingBox.topLeft.translateTo(new PointXY(DMap.MIN_X, DMap.MIN_Y))
        val econn = getRowStart(row)
        val wconn = SoundMapBuilder.westConnector(room)
        val cdelta = wconn.getTransformTo(econn)
        val pastedRoom = pasteSectorGroup(room, cdelta)
        joinWalls(econn, SoundMapBuilder.westConnector(pastedRoom))
        currentPsg = Some(pastedRoom)
      }
      case Some(psg) =>
        val econn = SoundMapBuilder.eastConnector(psg)
        if(econn.getAnchorPoint.x < DMap.MAX_X - (1024 * 5)){
          val wconn = SoundMapBuilder.westConnector(room)
          val cdelta = wconn.getTransformTo(econn)
          val pastedRoom = pasteSectorGroup(room, cdelta)
          joinWalls(econn, SoundMapBuilder.westConnector(pastedRoom))
          currentPsg = Some(pastedRoom)

        }else{
          println("NEW ROW")
          row += 1
          // need a new row
          val econn = getRowStart(row)
          val wconn = SoundMapBuilder.westConnector(room)
          val cdelta = wconn.getTransformTo(econn)
          val pastedRoom = pasteSectorGroup(room, cdelta)
          joinWalls(econn, SoundMapBuilder.westConnector(pastedRoom))
          currentPsg = Some(pastedRoom)
        }

    }
  }
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

    val DUKE_VOCALS: Set[Int] = MusicSFXList.DUKE_VOCALS.ALL.asScala.map(_.toInt).toSet
    val DOORS: Set[Int] = MusicSFXList.DOOR_SOUNDS.ALL.asScala.map(_.toInt).toSet


    def makeRoom(sound: Int): SectorGroup ={
      if(sound < 0) throw new IllegalArgumentException
      val room = palette.getSectorGroup(3).copy()
      room.getMap.allSprites.foreach { s =>
        if(s.getTexture == TextureList.TOUCHPLATE || s.getTexture == TextureList.ACTIVATOR || s.getTexture == TextureList.MUSIC_AND_SFX) {
          s.setLotag(sound)
        }else if(isAlphaNum(s.getTexture)){ // red letters/numbers
          val radix = s.getLotag
          // val digit = radix match {
          //   case 1 => (sound % (radix * 10)) / 1
          //   case 10 => (sound % (radix * 10)) / 10
          //   case 100 => (sound % (radix * 10)) / 100
          // }
          val digit = (sound % (radix * 10)) / radix
          s.setTexture(digitTex(digit))
          //val zero = 2930
          //if(sound < 10){
          //  s.setTexture(zero + sound)
          //}
        }
      }
      val wall = room.getMap.allWalls.filter(_.getLotag == 2).head
      if(DUKE_VOCALS.contains(sound)){
        wall.setTexture(1405)
        wall.setXRepeat(5)
        wall.setYRepeat(4)
      }else if(DOORS.contains(sound)){
        wall.setTexture(1173)
      }
      room
    }

    // top row can fit at least 60


    // TODO - incorporate this info into the MusicSFXList ...

    // TODO - useful:  http://infosuite.duke4.net/index.php?page=references_sound_list

    val missingSounds1: Set[Int] = Set[Int](23, 24, 31, 32, 44, 45, 46, 47, 52, 53, 54, 55)
    val soundGroup1: Set[Int] = (1 to 56).toSet -- missingSounds1


    val missingSounds2: Set[Int] = Set(77)
    val soundGroup2: Set[Int] = Set(61, 64) ++ (67 to 79).toSet -- missingSounds2

    val missingSounds3: Set[Int] = Set(94)
    val bossSounds = (96 to 105).toSet
    val soundGroup3: Set[Int] = (81 to 95).toSet ++ bossSounds -- missingSounds3


    val missingSounds4 = Set(134, 152, 161, 162, 176, 182)
    val soundGroup4: Set[Int] = (106 to 144).toSet ++ (148 to 308) -- missingSounds4

    // TODO - make a list of sounds that are duke quotes, and change texture to be his face
    // (optionally omit them entirely)

    val allSounds = soundGroup1 ++ soundGroup2 ++ soundGroup3 ++ soundGroup4 -- DUKE_VOCALS
    for(i <- allSounds.toSeq.sorted){
      println(s"${i}")
      builder.addRoom(makeRoom(i))
    }

    println(s"Sector count: ${builder.outMap.getSectorCount}")

    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }
}
