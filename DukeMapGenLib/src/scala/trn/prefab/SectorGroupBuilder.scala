package trn.prefab

import java.util

import trn.{DukeConstants, ISpriteFilter, PointXY, PointXYZ, Sprite, Wall, Map => DMap}
import trn.MapImplicits._

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer // this is the good one

// TODO - this should probably become a constructor on SectorGroup object if/when that is ever moved completely to scala
object SectorGroupBuilder {

  def isMarker(lotag: Int, sprite: Sprite): Boolean = {
    if(lotag < 0) throw new IllegalArgumentException
    sprite.getTexture == PrefabUtils.MARKER_SPRITE_TEX && sprite.getLotag == lotag
  }

  def createSectorGroup(map: DMap, sectorGroupId: Int): SectorGroup = {
    val autoTexts = scala.collection.mutable.Map[Int, AutoText]()
    map.allSprites.filter(isMarker(PrefabUtils.MarkerSpriteLoTags.AUTO_TEXT, _)).foreach { sprite =>
      val autoText = AutoText(sprite, map)
      val merged = autoTexts.get(autoText.autoTextId).map(_.mergedWith(autoText)).getOrElse(autoText)
      autoTexts(merged.autoTextId) = merged
    }

    // autoTexts.values.asJava
    val sg = new SectorGroup(map, sectorGroupId)
    autoTexts.values.foreach(sg.addAutoText(_))

    sg
  }
  def createSectorGroup(map: DMap): SectorGroup = {
    createSectorGroup(map, -1)
  }

}
