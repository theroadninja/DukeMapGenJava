package trn.prefab

import java.util

import trn.MapImplicits._
import trn.{ISpriteFilter, IdMap, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, SpriteFilter, Map => DMap}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer


/**
  * TODO - migrate all uses of this to MapWriter2 (or whatever that becomes).
  *
  * Providers extra functionality for placing sectors whose locations are not important (underwater sectors, etc).
  * Automatically separates them, so you don't have to bother hardcoding locations.
  *
  * @Deprecated - moving this to MapWriter2
  */
trait AnywhereBuilder { // TODO rename to AnywhereWriter or something

  def sgPacker: SectorGroupPacker

  // this is a method on MapBuilder trait
  def pasteSectorGroup(sg: SectorGroup, translate: PointXYZ): PastedSectorGroup

  final def placeAnywhere(sg: SectorGroup): PastedSectorGroup = {
    val topLeft = sgPacker.reserveArea(sg)
    val tr = sg.boundingBox.getTranslateTo(topLeft).withZ(0)
    pasteSectorGroup(sg, tr)
  }
}

/**
  * TODO - this doesnt work right; getting that def/val race condition
  * @deprecated This is a temporary provider to provide the game config.  In the future it should be passed in explicitly.
  */
trait HardcodedGameConfigProvider {
  val gameCfg: GameConfig = DukeConfig.loadHardCodedVersion()
}

/**
  * TODO - put a comment here saying where I can find a basic, vanilla builder
  * TODO - this should go away in favor of SgMapBuilder + MapWriter
  */
trait MapBuilder
  extends ISectorGroup
  with TagGenerator
{
  val outMap: DMap

  // WARNING:  because of def/val/trait bullshit, you need to defined this in the class's primary constructor (between
  // the parens after the class name).  Otherwise this may be null.
  def gameCfg: GameConfig

  val sgBuilder = new SgMapBuilder(outMap, gameCfg)

  override def nextUniqueHiTag(): Int = sgBuilder.nextUniqueHiTag()

  override def getMap(): DMap = outMap

  override def findSprites(picnum: Int, lotag: Int, sectorId: Int): util.List[Sprite] = {
    getMap().findSprites(picnum, lotag, sectorId)
  }

  override def findSprites(filters: ISpriteFilter*): java.util.List[Sprite] = {
    getMap().findSprites4Scala(filters.asJava)
  }

  // gets all of the water connections from the pasted sector group, in sorted order
  def getWaterConns(psg: PastedSectorGroup): Seq[TeleportConnector] = {
    val conns = psg.findConnectorsByType(ConnectorType.TELEPORTER).asScala.map(_.asInstanceOf[TeleportConnector])
    val waterConns = conns.filter(_.isWater).map(w => (w, w.getSELocation(psg))).sortBy(t => MapWriter.waterSortKey(t._2))
    waterConns.unzip._1.toSeq
  }

  def linkAllWater(psg1: PastedSectorGroup, psg2: PastedSectorGroup): Unit = {
    //
    // TODO - for now this assumes that all water connectors in both sector groups are part of the same
    // connection (and dont go to some third sector group)

    val waterConns1 = getWaterConns(psg1)
    val waterConns2 = getWaterConns(psg2)
    if(waterConns1.size != waterConns2.size) throw new SpriteLogicException()
    // TODO - we could also check the relative distances bewteen all of them

    waterConns1.zip(waterConns2).foreach { case (c1: TeleportConnector, c2: TeleportConnector) =>
        //TeleportConnector.linkTeleporters(c1, psg1, c2, psg2, nextUniqueHiTag())
        TeleportConnector.linkTeleporters(c1, this, c2, this, nextUniqueHiTag())
    }
  }


  def getWaterConns2(groups: Seq[PastedSectorGroup]): Seq[TeleportConnector] = {
    val conns = groups.flatMap { psg => psg.findConnectorsByType(ConnectorType.TELEPORTER).asScala.map(_.asInstanceOf[TeleportConnector]) }
    val waterConns = conns.filter(_.isWater).map(w => (w, w.getSELocation(this))).sortBy(t => MapWriter.waterSortKey(t._2))
    waterConns.unzip._1
  }

  def linkAllWater2(aboveWater: Seq[PastedSectorGroup], belowWater: Seq[PastedSectorGroup]): Unit = {

    val aboveWaterConns = getWaterConns2(aboveWater)
    val belowWaterConns = getWaterConns2(belowWater)

    aboveWaterConns.zip(belowWaterConns).foreach { case (aboveC: TeleportConnector, belowC: TeleportConnector) =>
        TeleportConnector.linkTeleporters(aboveC, this, belowC, this, nextUniqueHiTag())
    }

  }

  def linkAllWater(singleGroup: SectorGroup): Unit = {
    val conns = singleGroup.getTeleportConnectors().filter(c => c.isWater && !c.isLinked(singleGroup.map))
    MapWriter.linkAllWater(singleGroup, conns, this)
  }



}
