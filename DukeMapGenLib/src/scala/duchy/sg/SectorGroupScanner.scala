package duchy.sg

import trn.prefab.{SectorGroupHints, PrefabUtils, GameConfig, SectorGroup, SectorGroupProperties, ChildPointer, SectorGroupBuilder, SpriteLogicException, TagGenerator}
import trn.{PointXYZ, MapUtil, CopyState, MapImplicits, Sprite, Map => DMap}
import trn.MapImplicits._

import scala.collection.JavaConverters._
import scala.collection.mutable

/**
  * A sector group that we copied from the source map, but haven't finished processing into a real "SectorGroup" yet.
  */
case class SectorGroupFragment (
  clipboard: DMap,
  copyState: CopyState,
  props: SectorGroupProperties,
  hints: SectorGroupHints,
  groupIdSprite: Option[Sprite],
  teleChildSprite: Option[Sprite],
  childPointerSprite: Option[Sprite],
  sectorGroup: SectorGroup,
  groupId: Option[Int],
) {

  // for java
  def getGroupId: Int = groupId.getOrElse(-1)

  def checkChildPointer(): Unit = {
    val childPtr = sectorGroup.getChildPointer
    if (childPtr.connectorId == 0) throw new SpriteLogicException("child pointer connector must have a connector ID")
  }

  /**
    * throws exception if there are any invalid marker sprites
    */
  def requireValidMarkerSprites(): Unit = {
   for(i <- 0 until clipboard.getSpriteCount) {
     PrefabUtils.checkValid(clipboard.getSprite(i))
   }
  }

  def findSprites(picnum: Int, lotag: Int): Seq[Sprite] = SectorGroupScanner.findSprites(clipboard, picnum, lotag)

  def requireAtMostOneAnchor(): Unit = {
    // val anchors = clipboard.findSprites(SectorGroupScanner.MarkerTex, PrefabUtils.MarkerSpriteLoTags.ANCHOR, null).asScala
    val anchors = findSprites(SectorGroupScanner.MarkerTex, PrefabUtils.MarkerSpriteLoTags.ANCHOR)
    if(anchors.size > 1){
      throw new SpriteLogicException("more than one anchor sprite in group", anchors.map(_.getLocation.asPointXY).asJava)

    }
  }

}

object SectorGroupScanner {
  val MarkerTex = PrefabUtils.MARKER_SPRITE_TEX

  def findSprites(map: DMap, picnum: Int, lotag: Int): Seq[Sprite] = map.findSprites(picnum, lotag, null).asScala

  /**
    * Looks for a certain marker sprite and returns it if it exists.  If more then one exist, throws an exception.
    * @param map map to scan for the sprites
    * @return
    */
  def findAtMostOneMarker(map: DMap, lotag: Int): Option[Sprite] = {
    val sprites: Seq[Sprite] = map.findSprites(MarkerTex, lotag, null).asScala // sectorId = null
    if (sprites.size > 1){
      val msg = s"Sector Group contains more than one sprite with lotag=${lotag}"
      throw new SpriteLogicException(msg, sprites.map(_.getLocation.asPointXY).asJava)
    }else{
      sprites.headOption
    }
  }

  /**
    * Scans the map for a sector group containing the given id.
    *
    * A sector group is all sectors that share redwalls with each other (if you think of sectors as nodes and redwalls
    * as edges, then: given a node, it is returning the connected component).
    *
    * @param sectorId any sector id in the group to return
    */
  def scanFragment(sourceMap: DMap, cfg: GameConfig, sectorId: Int): SectorGroupFragment = {
    val clipboard = DMap.createNew()
    val cpstate = MapUtil.copySectorGroup(cfg, sourceMap, clipboard, sectorId, new PointXYZ(0, 0, 0), false)
    val props = SectorGroupProperties.scanMap(clipboard)
    val hints = SectorGroupHints(clipboard)

    val adjusted: DMap = if(props.zAdjust.isDefined){
      System.out.println("doing z transform")
      clipboard.translated(props.zAdjustTrx)
    } else {
      clipboard
    }

    val groupIdSprite = findAtMostOneMarker(adjusted, PrefabUtils.MarkerSpriteLoTags.GROUP_ID)
    val teleChildSprite = findAtMostOneMarker(adjusted, PrefabUtils.MarkerSpriteLoTags.TELEPORT_CHILD)
    val redwallChildSprite = findAtMostOneMarker(adjusted, PrefabUtils.MarkerSpriteLoTags.REDWALL_CHILD)
    val groupSprites = Seq(groupIdSprite, teleChildSprite, redwallChildSprite).filter(_.isDefined).map(_.get)
    if(groupSprites.size == 0) {
      // check for the case where someone added a group id sprite in the 2d view but forgot to give it the marker sprite tex
      val DefaultSprite = 0
      val mistakeCount = SectorGroupScanner.findSprites(clipboard, DefaultSprite, PrefabUtils.MarkerSpriteLoTags.GROUP_ID).size
      if(mistakeCount > 0){
        throw new SpriteLogicException("Sector group has no ID marker sprite but it DOES have a sprite with texture 0")
      }
    }else if(groupSprites.size > 1){
      val lotags = groupSprites.map(_.getLotag)
      val locations = groupSprites.map(_.getLocation.asPointXY)
      throw new SpriteLogicException(
        s"Sector Group contains mutually exclusive sprites lotags=${lotags}",
        locations.asJava,
      )
    }

    val groupId: Int = groupIdSprite.map(_.getHiTag).getOrElse(-1)
    val sg = SectorGroupBuilder.createSectorGroup(clipboard, groupId, props, hints)

    val frag = SectorGroupFragment(
      adjusted,
      cpstate,
      props,
      hints,
      groupIdSprite,
      teleChildSprite,
      redwallChildSprite,
      sg,
      groupIdSprite.map(_.getHiTag),
    )
    frag.requireValidMarkerSprites()
    frag
  }

  def scanFragments(sourceMap: DMap, cfg: GameConfig): Seq[SectorGroupFragment] = {
    val processedSectorIds = mutable.Set[Int]()
    val fragments = mutable.ArrayBuffer[SectorGroupFragment]()
    for(sectorId: Int <- 0 until sourceMap.getSectorCount){
      if(! processedSectorIds.contains(sectorId)){
        val fragment = SectorGroupScanner.scanFragment(sourceMap, cfg, sectorId)
        processedSectorIds ++= fragment.copyState.sourceSectorIds.asScala.map(_.toInt)
        // processedSectorIds.addAll(fragment.copyState.sourceSectorIds.asScala)
        fragments.append(fragment)
      }
    }
    fragments
  }

  def scanFragmentsAsJava(sourceMap: DMap, cfg: GameConfig): java.util.List[SectorGroupFragment] = scanFragments(sourceMap, cfg).asJava

  // TODO make pure scala
  def connectRedwallChildrenJava(
    tagGenerator: TagGenerator,
    cfg: GameConfig,
    parentGroups: java.util.Map[Integer, SectorGroup],
    redwallChildren: java.util.Map[Integer, java.util.List[SectorGroup]],
  ): java.util.Map[Integer, SectorGroup] = {

    parentGroups.asScala.map {
      case (parentGroupId: Integer, parentGroup) => if(redwallChildren.containsKey(parentGroupId)){
        parentGroupId -> parentGroup.connectedToChildren2(redwallChildren.get(parentGroupId), tagGenerator, cfg)
      } else {
        parentGroupId -> parentGroup
      }
    }.toMap.asJava
  }

}
