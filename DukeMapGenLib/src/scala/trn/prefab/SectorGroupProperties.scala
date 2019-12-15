package trn.prefab

import trn.{MapImplicits, Sprite, Map => DMap}
import scala.collection.JavaConverters._
import trn.MapImplicits._

object SectorGroupProperties {

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


  def scanMap(map: DMap): SectorGroupProperties = {
    //public List<Sprite> findSprites(Integer picnum, Integer lotag, Integer sectorId){
    // List<Sprite> idSprite = clipboard.findSprites(PrefabUtils.MARKER_SPRITE_TEX, PrefabUtils.MarkerSpriteLoTags.GROUP_ID, null);
    //map.findSprites(PrefabUtils.MARKER_SPRITE_TEX)
    val groupIds = findMarkers(map, PrefabUtils.MarkerSpriteLoTags.GROUP_ID).map(_.getHiTag.toInt)
    SpriteLogicException.throwIf(groupIds.size > 1, s"too many group id sprites in group size=${groupIds.size}")
    new SectorGroupProperties(groupIds.headOption, hasMarker(map, PrefabUtils.MarkerSpriteLoTags.STAY))
  }
}
class SectorGroupProperties(val groupId: Option[Int], stay: Boolean) {

  def stayFlag: Boolean = stay

}
