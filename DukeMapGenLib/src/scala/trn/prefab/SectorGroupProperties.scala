package trn.prefab

import trn.{MapImplicits, PointXYZ, Sprite, Map => DMap}

import scala.collection.JavaConverters._
import trn.MapImplicits._

object SectorGroupProperties {

  /** defaults for generating sector groups in code */
  val Default = new SectorGroupProperties(None, false, None, Seq.empty, Seq.empty)

  def findMarkers(map: DMap, lotag: Int, max: Option[Int] = None): Seq[Sprite] = {
    val list = map.allSprites.filter(s => s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == lotag)
    //val list = map.findSprites(PrefabUtils.MARKER_SPRITE_TEX, lotag, null).asScala
    max.map { m =>
      if(list.size > math.max(0, m)){
        throw new SpriteLogicException(s"sector group has too many sprites with lotag ${lotag}", list.head)
      }
    }
    list
  }

  def hasMarker(map: DMap, lotag: Int): Boolean = findMarkers(map, lotag, Some(1)).size == 1


  /**
    * @param map a map containing ONLY the sectors of this sector group
    * @return
    */
  def scanMap(map: DMap): SectorGroupProperties = {
    //public List<Sprite> findSprites(Integer picnum, Integer lotag, Integer sectorId){
    // List<Sprite> idSprite = clipboard.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.GROUP_ID, null);
    //map.findSprites(PrefabUtils.MARKER_SPRITE_TEX)
    val groupIds = findMarkers(map, PrefabUtils.MarkerSpriteLoTags.GROUP_ID).map(_.getHiTag.toInt)
    SpriteLogicException.throwIf(groupIds.size > 1, s"too many group id sprites in group size=${groupIds.size}")

    val zAdjust = findMarkers(map, PrefabUtils.MarkerSpriteLoTags.TRANSLATE_Z).map(_.getHiTag)
    SpriteLogicException.throwIf(zAdjust.size > 1, "only one 'translate z' marker tag allowed in a sector group")

    val axisLocks = findMarkers(map, PrefabUtils.MarkerSpriteLoTags.ALGO_AXIS_LOCK).map(_.getHiTag).map(AxisLock(_))

    val switchesRequested = findMarkers(map, Marker.Lotags.SWITCH_REQUESTED).map(_.getHiTag)

    new SectorGroupProperties(groupIds.headOption, hasMarker(map, PrefabUtils.MarkerSpriteLoTags.STAY), zAdjust.headOption, axisLocks, switchesRequested)
  }
}
class SectorGroupProperties(
  val groupId: Option[Int],
  stay: Boolean,
  val zAdjust: Option[Int],
  val axisLocks: Seq[AxisLock],
  val switchesRequested: Seq[Int], // list of "link ids" we need switches for.  See Marker.Lotags.SWITCH_REQUESTED
) {

  def stayFlag: Boolean = stay

  def zAdjustTrx: PointXYZ = new PointXYZ(0, 0, zAdjust.getOrElse(0) << 4)

}
