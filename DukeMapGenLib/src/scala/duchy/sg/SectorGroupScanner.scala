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

  def isChild: Boolean = teleChildSprite.isDefined || childPointerSprite.isDefined
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

  private[sg] def sortFragments(
    fragments: TraversableOnce[SectorGroupFragment]
  ): (Map[Int, SectorGroupFragment], Seq[SectorGroupFragment], Seq[SectorGroupFragment], Seq[SectorGroupFragment]) = {
    val numberedGroups = mutable.Map[Int, SectorGroupFragment]()
    val redwallChildren = mutable.ArrayBuffer[SectorGroupFragment]()
    val teleportChildren = mutable.ArrayBuffer[SectorGroupFragment]()
    val anonymous = mutable.ArrayBuffer[SectorGroupFragment]()
    fragments.foreach { fragment =>
      if(fragment.groupIdSprite.isDefined) {
        val groupId = fragment.getGroupId
        if (numberedGroups.contains(groupId)) {
          val loc1 = numberedGroups(groupId).groupIdSprite.get.getLocation.asPointXY
          val loc2 = fragment.groupIdSprite.get.getLocation.asPointXY
          throw new SpriteLogicException(s"More than one sector group contains id=${groupId}", Seq(loc1, loc2).asJava)
        }
        numberedGroups.put(groupId, fragment)
      }else if(fragment.childPointerSprite.isDefined){
        redwallChildren.append(fragment)
      }else if(fragment.teleChildSprite.isDefined){
        teleportChildren.append(fragment)
      }else{
        anonymous.append(fragment)
      }
    }

    (numberedGroups.toMap, redwallChildren, teleportChildren, anonymous)
  }

  def assembleFragments(
    cfg: GameConfig,
    tagGenerator: TagGenerator,
    fragments: Seq[SectorGroupFragment],
  ): SgPaletteScala = {

    val (numberedFrags, redwallChildFrags, teleChildFrags, anonFrags) = sortFragments(fragments)

    // val numberedSectorGroups = numberedFrags.map {
    //   case (groupId, fragment) => (groupId -> fragment.sectorGroup)
    // }
    // val anonymousSectorGroups = anonFrags.map(_.sectorGroup)

    val redwallChildren = mutable.Map[Int, mutable.ArrayBuffer[SectorGroup]]()
    val teleportChildren = mutable.Map[Int, mutable.ArrayBuffer[SectorGroup]]()
    redwallChildFrags.foreach { fragment =>
      val parentId = fragment.childPointerSprite.get.getHiTag
      if(! numberedFrags.contains(parentId)){
        throw new SpriteLogicException(s"Sprite refers to parent sector group id ${parentId} but no group exists", fragment.childPointerSprite.get)
      }
      fragment.checkChildPointer()
      val children = redwallChildren.getOrElseUpdate(parentId, mutable.ArrayBuffer[SectorGroup]())
      children.append(fragment.sectorGroup)
    }
    teleChildFrags.foreach { fragment =>
      val parentId = fragment.teleChildSprite.get.getHiTag
      if (!numberedFrags.contains(parentId)) {
        throw new SpriteLogicException(s"Sprite refers to parent sector group id ${parentId} but no group exists", fragment.childPointerSprite.get)
      }
      val children = teleportChildren.getOrElseUpdate(parentId, mutable.ArrayBuffer[SectorGroup]())
      children.append(fragment.sectorGroup)
    }

    // merge the redwall children with their parents
    val numberedSectorGroups = numberedFrags.map {
      case (parentGroupId: Int, parentFragment: SectorGroupFragment) => if (redwallChildren.contains(parentGroupId)) {
        parentGroupId -> parentFragment.sectorGroup.connectedToChildren2(redwallChildren(parentGroupId).asJava, tagGenerator, cfg)
      } else {
        parentGroupId -> parentFragment.sectorGroup
      }
    }

    SgPaletteScala(numberedSectorGroups, teleportChildren.toMap, anonFrags.map(_.sectorGroup))
  }

  // TODO:  `sourceMap` does not need to be a mutable map --- something like an immutable "MapView" would be fine
  def scanMap(cfg: GameConfig, tagGenerator: TagGenerator, sourceMap: DMap): SgPaletteScala = {
    assembleFragments(
      cfg,
      tagGenerator,
      scanFragments(sourceMap, cfg)
    )
  }
}
