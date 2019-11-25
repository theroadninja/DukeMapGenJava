package trn.prefab

import trn.{IdMap, ImmutableMap, MapUtil, PlayerStart, PointXY, PointXYZ, Sprite, SpriteFilter, Map => DMap}

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.collection.JavaConverters._ // this is the good one
/**
  * Lower-level map builder, that manages pasted sector groups.
  */
class SgMapBuilder(private val map: DMap) extends TagGenerator {

  // when this becomes true, the map is "finalized" and we can no longer add/remove sector groups
  var markersCleared = false

  def getMap: ImmutableMap = map.readOnly
  def sectorCount: Int = map.getSectorCount

  var hiTagCounter = 1
  // TODO - for now, if PSGs are modified (connecting teleporers, elevators...) just mark them as
  // unable to be removed (so a PSG can only be deleted if it hasn't been touched)
  private val pastedSectorGroupsMutable: mutable.Buffer[PastedSectorGroup] = new ListBuffer()
  def pastedSectorGroups: Seq[PastedSectorGroup] = pastedSectorGroupsMutable
  private var pastedStays: Option[Seq[PastedSectorGroup]] = None  // NOTE: will need to check this when removing PSGs

  override def nextUniqueHiTag(): Int = {
    val i = hiTagCounter
    hiTagCounter += 1
    i
  }

  def pasteSectorGroup2(sg: SectorGroup, translate: PointXYZ): (PastedSectorGroup, IdMap)  = {
    require(!markersCleared)
    val copyState = MapUtil.copySectorGroup(sg.map, map, 0, translate);
    val tp = (new PastedSectorGroup(map, copyState), copyState.idmap)
    pastedSectorGroupsMutable.append(tp._1)
    tp
  }

  /**
    * Paste all of the sectors with "stay" markers.
    * @param palette
    */
  def pasteAllStaySectors(palette: PrefabPalette): Unit = {
    require(!pastedStays.isDefined)
    pastedStays = Some(palette.getStaySectorGroups.asScala.map { sg =>
      val (psg, _) = pasteSectorGroup2(sg, PointXYZ.ZERO)  // no translate == leave where it is
      psg
    })
  }

  /**
    * Automatically links any redwall connectors that happen to be perfectly lined up
    * // compare to Hypercube2 autoLinkRooms
    *
    */
  def autoLinkRedwalls(): Int = {
    var count = 0
    val unlinked = pastedSectorGroups.flatMap(psg => psg.unlinkedRedwallConnectors)
    unlinked.foreach(c => require(!c.isLinked(map)))
    unlinked.foreach { x =>
      unlinked.foreach { y =>
        if (x.isFullMatch(y, map)) {
          x.linkConnectors(map, y)
          count += 1
        }
      }
    }
    count
  }

  def linkTeleporters(
    c1: TeleportConnector,
    g1: ISectorGroup,
    c2: TeleportConnector,
    g2: ISectorGroup
  ){
    TeleportConnector.linkTeleporters(c1, g1, c2, g2, nextUniqueHiTag())
  }

  def clearMarkers(): Unit = {
    require(!markersCleared)
    if(!map.hasPlayerStart){
      throw new IllegalStateException("Cannot delete marker sprites - there is no player start set")
    }
    map.deleteSprites(SpriteFilter.texture(PrefabUtils.MARKER_SPRITE_TEX))
    markersCleared = true
  }

  /**
    * Links c1 and c2 (by creating redwalls).
    * @param c1 an unliked connected in a pasted sector group
    * @param c2 an unliked connected in a pasted sector group
    */
  def linkConnectors(c1: RedwallConnector, c2: RedwallConnector): Unit = {
    if(c1 == null || c2 == null) throw new IllegalArgumentException
    c1.linkConnectors(map, c2)
  }

}
