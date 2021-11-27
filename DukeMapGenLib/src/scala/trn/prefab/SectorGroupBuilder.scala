package trn.prefab

import java.util

import trn.{PointXY, PointXYZ, Sprite, Wall, Map => DMap}
import trn.MapImplicits._

import scala.collection.JavaConverters._
import scala.collection.mutable.ListBuffer // this is the good one

// TODO - this should probably become a constructor on SectorGroup object if/when that is ever moved completely to scala
object SectorGroupBuilder {

  def isMarker(lotag: Int, sprite: Sprite): Boolean = {
    if(lotag < 0) throw new IllegalArgumentException
    sprite.getTexture == PrefabUtils.MARKER_SPRITE_TEX && sprite.getLotag == lotag
  }

  // // TODO - duplicate of method in AutoText
  // def getTextSpriteIds(sectorId: Int, map: DMap): Seq[Int] = {
  //   (0 until map.getSpriteCount).filter { spriteId =>
  //     val sprite = map.getSprite(spriteId)
  //     sprite.getSectorId == sectorId && AutoText.isFontSprite(sprite)
  //   }
  // }
  private def getTextSpriteIds(sectorId: Int, map: DMap): Seq[Int] = {
    (0 until map.getSpriteCount).filter { spriteId =>
      val sprite = map.getSprite(spriteId)
      sprite.getSectorId == sectorId && AutoText.isFontSprite(sprite)
    }
  }

  private def createAutoText(marker: Sprite, map: DMap): AutoText = {
    // TODO - currently the IdMap doesnt track sprites, so we cant load them here
    if(getTextSpriteIds(marker.getSectorId, map).size < 1){
      throw new SpriteLogicException("AutoText marker (5) without any texture sprites with nonzero lotags")
    }
    new AutoText(PrefabUtils.hitagToId(marker), Set(marker.getSectorId.toInt))
  }

  def createSectorGroup(map: DMap, sectorGroupId: Int, props: SectorGroupProperties, hints: SectorGroupHints): SectorGroup = {
    val autoTexts = scala.collection.mutable.Map[Int, AutoText]()
    map.allSprites.filter(isMarker(PrefabUtils.MarkerSpriteLoTags.AUTO_TEXT, _)).foreach { sprite =>
      //val autoText = AutoText(sprite, map)
      val autoText = createAutoText(sprite, map)
      val merged = autoTexts.get(autoText.autoTextId).map(_.mergedWith(autoText)).getOrElse(autoText)
      autoTexts(merged.autoTextId) = merged
    }

    // autoTexts.values.asJava
    val sg = SectorGroup.newSG(map, sectorGroupId, props, hints)
    autoTexts.values.foreach(sg.addAutoText(_))

    sg
  }
  def createSectorGroup(map: DMap, props: SectorGroupProperties, hints: SectorGroupHints): SectorGroup = {
    createSectorGroup(map, -1, props, hints)
  }

}
