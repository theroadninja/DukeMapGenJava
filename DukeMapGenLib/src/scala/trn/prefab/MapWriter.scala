package trn.prefab

import java.util

import trn.duke.{PaletteList, TextureList}
import trn.{BuildConstants, ISpriteFilter, IdMap, PlayerStart, PointXY, PointXYZ, RandomX, Sprite, WallView, Map => DMap}
import trn.MapImplicits._
import trn.FuncImplicits._
import trn.prefab.experiments._
import trn.render.WallAnchor

import scala.collection.JavaConverters._

case class ConnMatch(newConn: RedwallConnector, existingConn: RedwallConnector)

// creating this as an adapter for old (new?) code
class MapBuilderAdapter(val outMap: DMap, val gameCfg: GameConfig) extends MapBuilder {

}

object MapWriter {
  val MaxSectors = BuildConstants.MaxSectors
  val MapBounds = BuildConstants.MapBounds
  //val MapBounds = BoundingBox(DMap.MIN_X, DMap.MIN_Y, DMap.MAX_X, DMap.MAX_Y)
  val MarkerTex = PrefabUtils.MARKER_SPRITE_TEX

  def isMarkerSprite(s: Sprite, lotag: Int): Boolean = {
    s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == lotag
  }

  def isMarker(s: Sprite): Boolean = s.getTexture == PrefabUtils.MARKER_SPRITE_TEX

  def isAnchorSprite(s: Sprite): Boolean = isMarkerSprite(s, PrefabUtils.MarkerSpriteLoTags.ANCHOR)

  def waterSortKey(p: PointXYZ): Long = {
    val x = (p.x + 65535L) << 4
    val y = (p.y + 65535L) << 2
    val z = p.z + 65535L
    x + y + z
  }

  def linkAllWater(singleGroup: SectorGroup, conns: Seq[TeleportConnector], tagGenerator: TagGenerator): Unit = {
    val map = singleGroup.getMap
    val aboveWater: Seq[TeleportConnector] = conns.filter(c => map.getSector(c.getSectorId).getLotag == 1).sortBy(t => MapWriter.waterSortKey(t.getSELocation(singleGroup)))
    val belowWater: Seq[TeleportConnector] = conns.filter(c => map.getSector(c.getSectorId).getLotag == 2).sortBy(t => MapWriter.waterSortKey(t.getSELocation(singleGroup)))
    if(aboveWater.size != belowWater.size){
      throw new SpriteLogicException(s"There are ${aboveWater.size} above water vs ${belowWater.size} below.")
    }
    //println(s"above water size=${aboveWater.size}")

    // TODO - verify the locations have the same relative positions
    aboveWater.zip(belowWater).foreach { case (above, below) => TeleportConnector.linkTeleporters(above, singleGroup, below, singleGroup, tagGenerator.nextUniqueHiTag()) }
  }

  def painted(sg: SectorGroup, colorPalette: Int, excludeTextures: Seq[Int] = Seq.empty): SectorGroup = {
    val sg2 = sg.copy
    sg2.getMap.allWalls.foreach { w=>
      if (! excludeTextures.contains(w.getTexture)){
        w.setPal(colorPalette)
      }
    }
    sg2
  }

  // this includes the floors
  // TODO - see if we can merge with painted()
  def painted2(sg: SectorGroup, colorPalette: Int, excludeTextures: Seq[Int] = Seq.empty): SectorGroup = {
    val sg2 = sg.copy
    sg2.getMap.allWalls.foreach { w=>
      if (! excludeTextures.contains(w.getTexture)){
        w.setPal(colorPalette)
      }
    }
    sg2.allSectorIds.map(sg2.getMap.getSector(_)).foreach { sector =>
      sector.setFloorPalette(colorPalette)
      sector.setCeilingPalette(colorPalette)
    }
    sg2
  }

  def withElevatorsLinked(
    sg: SectorGroup,
    lowerConn: ElevatorConnector,
    higherConn: ElevatorConnector,
    hitag: Int,
    startLower: Boolean
  ): SectorGroup = {
    val sg2 = sg.copy
    ElevatorConnector.linkElevators(lowerConn, sg2, higherConn, sg2, hitag, startLower)
    sg2
  }

  // this is a merge operation
  def connected(sg1: SectorGroup, sg2: SectorGroup, connectorId: Int, gameCfg: GameConfig): SectorGroup = {
    val c1: RedwallConnector = sg1.getRedwallConnector(connectorId)
    val c2: RedwallConnector = sg2.getRedwallConnector(connectorId)
    sg1.connectedTo(c1, sg2, c2, gameCfg, false)
  }

  def apply(map: DMap, gameCfg: GameConfig): MapWriter = {
    val builder = new MapBuilderAdapter(map, gameCfg)
    new MapWriter(builder, builder.sgBuilder)
  }

  def apply(builder: MapBuilder): MapWriter = new MapWriter(builder, builder.sgBuilder)

  def apply(gameCfg: GameConfig): MapWriter = {
    val builder = new MapBuilderAdapter(DMap.createNew(), gameCfg)
    new MapWriter(builder, builder.sgBuilder)
  }

  def apply(gameCfg: GameConfig, sgPacker: Option[SectorGroupPacker]): MapWriter = {
    val builder = new MapBuilderAdapter(DMap.createNew(), gameCfg)
    new MapWriter(builder, builder.sgBuilder, sgPacker = sgPacker)
  }

  def unitTestWriter: MapWriter = apply(DukeConfig.empty)
}
/**
  * TODO - write unit tests for this class
  *
  * @param builder
  * @param sgBuilder
  */
class MapWriter(
  val builder: MapBuilder,
  val sgBuilder: SgMapBuilder,
  val random: RandomX = new RandomX(),
  val sgPacker: Option[SectorGroupPacker] = None
)
  extends ISectorGroup
    with EntropyProvider
    with MapWriter2 {

  /** throws if the map has too many sectors */
  def checkSectorCount(): Unit = {
    println(s"checkSectorCount(): Total Sector Count: ${builder.outMap.getSectorCount}")
    if(builder.outMap.getSectorCount > 1024){
      throw new Exception("too many sectors") // I think maps are limited to 1024 sectors
    }
  }

  def sectorCount: Int = builder.outMap.getSectorCount

  def canFitSectors(sg: SectorGroup): Boolean = sectorCount + sg.sectorCount < MapWriter.MaxSectors

  def outMap: DMap = getMap

  //
  //  Pasting and Linking
  //
  def pastedSectorGroups: Seq[PastedSectorGroup] = sgBuilder.pastedSectorGroups

  override def pasteAndLink(
    existingConn: RedwallConnector,
    newSg: SectorGroup,
    newConn: RedwallConnector,
    floatingGroups: Seq[SectorGroup] // I think these are sectors that need to be pasted at the same time, like underwater sectors
  ): PastedSectorGroup = {
    require(Option(existingConn).isDefined)
    require(Option(newSg).isDefined)
    require(Option(newConn).isDefined)
    val cdelta = newConn.getTransformTo(existingConn)


    pasteAndLink2(newSg, cdelta, Seq(ConnMatch(newConn, existingConn)), floatingGroups)
    // val (psg, idmap) = builder.pasteSectorGroup2(newSg, cdelta)
    // val pastedConn2 = newConn.translateIds(idmap, cdelta)
    // sgBuilder.linkConnectors(existingConn, pastedConn2)
    // psg
  }

  def pasteAndLink2(
    newSg: SectorGroup,
    translate: PointXYZ,
    conns: Seq[ConnMatch],
    floatingGroups: Seq[SectorGroup]
  ): PastedSectorGroup = {
    require(conns.size > 0)

    val (psg, idmap) = pasteSectorGroup2(newSg, translate, floatingGroups)
    conns.foreach { cmatch =>
      val newConn = cmatch.newConn.translateIds(idmap, translate, sgBuilder.getMapView)
      sgBuilder.linkConnectors(cmatch.existingConn, newConn)
    }
    psg
  }

  def isAnchor(s: Sprite): Boolean = MapWriter.isAnchorSprite(s)

  /**
    * TODO - candidate for moving to MapWriter
    * paste the sector group so that it's anchor is at the given location.  If no anchor,
    * its top left corner of the bounder box will be used.
    * @param sg
    * @param location
    * @return
    */
  def pasteSectorGroupAt(sg: SectorGroup, location: PointXYZ, mustHaveAnchor: Boolean = false): PastedSectorGroup = {
    val anchor = sg.sprites.find(isAnchor).map(_.getLocation).getOrElse{
      if(mustHaveAnchor){ throw new SpriteLogicException(("no anchor sprite"))}
      new PointXYZ(sg.boundingBox.xMin, sg.boundingBox.yMin, 0) // TODO - bounding box doesnt do z ...
    }
    //pasteSectorGroup(sg, anchor.getTransformTo(location))
    val floating = Seq.empty
    val (psg, _) = sgBuilder.pasteSectorGroup2(sg, anchor.getTransformTo(location), floating, None)
    psg
  }

  def pasteSectorGroupAtCustomAnchor(sg: SectorGroup, mapLocation: PointXYZ, sectorGroupAnchor: PointXYZ): PastedSectorGroup = {
    pasteSectorGroup2(sg, sectorGroupAnchor.getTransformTo(mapLocation), Seq.empty)._1
  }

  def pasteSectorGroup(sg: SectorGroup, translate: PointXYZ): PastedSectorGroup = {
    val (psg, _) = pasteSectorGroup2(sg, translate, Seq.empty)
    psg
  }

  def pasteSectorGroup2(sg: SectorGroup, translate: PointXYZ, floatingGroups: Seq[SectorGroup], changeUniqueTags: Boolean = true): (PastedSectorGroup, IdMap) = {
    sgBuilder.pasteSectorGroup2(sg, translate, floatingGroups, sgPacker, changeUniqueTags)
  }



  /** convenience method that tries to autolink any connectors in two PSGs */
  def autoLink(psg1: PastedSectorGroup, psg2: PastedSectorGroup): Int = {
    var count = 0
    psg1.redwallConnectors.foreach { c1 =>
      psg2.redwallConnectors.foreach { c2 =>
        if(sgBuilder.autoLink(c1, c2)){
          count += 1
        }
      }
    }
    count
  }

  // TODO - get rid of this
  def pasteStays(palette: PrefabPalette): Seq[PastedSectorGroup] = {
    val stays = palette.getStaySectorGroups.asScala
    stays.map { sg =>
      pasteSectorGroup(sg, PointXYZ.ZERO) // no translate == leave where it is
    }.toSeq
  }

  def pasteStays2(palette: PrefabPalette): Seq[(Option[Int], PastedSectorGroup)] = {
    val stays = palette.getStaySectorGroups.asScala
    stays.map(sg =>(sg.props.groupId, pasteSectorGroup(sg, PointXYZ.ZERO))).toSeq
  }

  def spaceAvailable(sg: SectorGroup, tx: PointXY): Boolean = {
    val bb = sg.boundingBox.translate(tx)
    lazy val translatedSg = sg.translatedXY(tx)
    bb.isInsideInclusive(BuildConstants.MapBounds) && {
      !pastedSectorGroups.exists{ psg =>
        psg.boundingBox.intersects(bb) && psg.intersectsWith(translatedSg)
      }
    }
  }

  /**
    * Tests if the given box is available (empty).  This tests the entire (axis-aligned) box, which means it is
    * an inefficient measure for sector groups that are not box-like.
    * For a better approximation, see spaceAvailable(SectorGroup, PointXY)
    * @param bb the bounding box
    * @return true if the bounding box is in bounds and there are no existing groups in that area.
    */
  def spaceAvailable(bb: BoundingBox): Boolean = {
    bb.isInsideInclusive(BuildConstants.MapBounds) &&
      pastedSectorGroups.filter(psg => psg.boundingBox.intersects(bb)).isEmpty
  }

  /**
    * @param existingConn a connect that was already pasted to the map
    * @param newConn a connection on the new sector group, newSg
    * @param newSg the sector group we are interested in pasted to the map
    * @return true if newSg can be placed on the map, with its connection `newConn` connecting to `existingConn` which
    *         is already on the map.
    */
  def canPlaceAndConnect(existingConn: RedwallConnector, newConn: RedwallConnector, newSg: SectorGroup, checkSpace: Boolean = true): Boolean = {
    val skipSpaceCheck = !checkSpace
    existingConn.isMatch(newConn) && (skipSpaceCheck || spaceAvailable(newSg, newConn.getTransformTo(existingConn).asXY))
  }

  // See the (static) MapWriter object for the version that works on unpasted SectorGroups
  def linkElevators(
    lowerConn: ElevatorConnector,
    higherConn: ElevatorConnector,
    startLower: Boolean
  ): Unit = ElevatorConnector.linkElevators(
    lowerConn,
    this,
    higherConn,
    this,
    sgBuilder.nextUniqueHiTag(),
    startLower
  )

  def applyPaletteToAll(textureId: Int, palette: Int): Unit = {
    if(textureId < 0 || palette < 0) throw new IllegalArgumentException
    this.outMap.allWalls.foreach { w =>
      if(w.getTexture == textureId){
        w.setPal(palette)
      }
    }
    this.outMap.sectors.asScala.foreach { s =>
      if(s.getFloorTexture == textureId){
        s.setFloorPalette(palette)
      }
      if(s.getCeilingTexture == textureId){
        s.setCeilingPalette(palette)
      }
    }
  }

  /**
    * Goes through the whole map and sets palette of 3 on any floor or ceiling texture that has a sky pallete that
    * normally kills the player.
    */
  def disarmAllSkyTextures(): Unit = {
    outMap.allSectors.foreach { sector =>
      if(TextureList.isDeadly(sector.getCeilingTexture)){
        sector.setCeilingPalette(PaletteList.DISARM_SKY)
      }
      if(TextureList.isDeadly(sector.getFloorTexture)){
        sector.setFloorPalette(PaletteList.DISARM_SKY)
      }
    }
  }

  /**
    * If the map does not have a player start set yet,
    * sets the player start of the map to the location of the first player start marker sprite it finds
    * @param force if true, will set start marker to 0, 0 on the map, to ensure the map can compile
    */
  def setAnyPlayerStart(force: Boolean = false): Unit = {
    if(!outMap.hasPlayerStart){
      val playerStarts = outMap.allSprites.filter(s =>
        s.getTexture == PrefabUtils.MARKER_SPRITE_TEX && s.getLotag == PrefabUtils.MarkerSpriteLoTags.PLAYER_START
      )
      if(playerStarts.size < 1) {
        if(force){
          outMap.setPlayerStart(new PlayerStart(0, 0, 0, PlayerStart.NORTH))
        }else{
          throw new SpriteLogicException("cannot set player start - there are no player start markers")
        }
      }else{
        outMap.setPlayerStart(new PlayerStart(playerStarts(0)))
      }
    }
  }

  /**
    * Sets the player start to one of the player start markers in the group.
    * @param psg
    */
  def setPlayerStart(psg: PastedSectorGroup): Unit = {
    //
    val sectorIds = psg.getCopyState.destSectorIds().asScala

    val playerStarts = psg.getMap.allSprites.filter(s =>
      s.getTexture == PrefabUtils.MARKER_SPRITE_TEX
        && s.getLotag == PrefabUtils.MarkerSpriteLoTags.PLAYER_START
        && sectorIds.contains(s.getSectorId)
    )
    if(playerStarts.size < 1) {
      throw new SpriteLogicException(s"no player start markers in sector group")
    }
    outMap.setPlayerStart(new PlayerStart(playerStarts(0)))
  }
  def clearMarkers(): Unit = sgBuilder.clearMarkers()

  def getWallAnchor(r: RedwallConnector): WallAnchor = {
    require(r.getWallCount == 1)
    val wall = outMap.getWallView(r.getWallIds.get(0))
    WallAnchor.fromExistingWall2(wall, outMap.getSector(r.getSectorId))
  }


  //
  //  ISectorGroup Methods
  //  TODO - sgBuilder should implement these, not MapWriter
  //
  override def getMap: DMap = builder.outMap

  override def findSprites(picnum: Int, lotag: Int, sectorId: Int): util.List[Sprite] = builder.findSprites(picnum, lotag, sectorId)

  // override def findSprites(filters: ISpriteFilter*): util.List[Sprite] = builder.findSprites(filters:_*)

}
