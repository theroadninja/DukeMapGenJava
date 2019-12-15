package trn.prefab.experiments

import trn.MapLoader
import trn.prefab._
import trn.{MapLoader, PointXY, PointXYZ, Wall, Map => DMap}

import scala.collection.JavaConverters._


class PoolBuilder(val outMap: DMap) extends MapBuilder {


  /** TODO - copied from Sushi Builder */
  def pasteOptions(writer: MapWriter, existing: PastedSectorGroup, newGroup: SectorGroup): Seq[Placement] = {
    def isMatch(existing: RedwallConnector, newConn: RedwallConnector, newGroup: SectorGroup): Boolean = {
      if(existing.isMatch(newConn)){
        val t = newConn.getTransformTo(existing)
        //spaceAvailable(newGroup.boundingBox.translate(t.asXY()))
        writer.spaceAvailable(newGroup, t.asXY)
      }else{
        false
      }
    }

    def possibleConnections(g1: PastedSectorGroup, g2: SectorGroup) = {
      val conns1 = g1.unlinkedRedwallConnectors
      val conns2 = g2.allRedwallConnectors
      conns1.flatMap(c1 => conns2.map(c2 => Placement(c1, c2, g2))).filter { p =>
        //case (c1, c2, _) => isMatch(c1, c2, g2)
        isMatch(p.existing, p.newConn, p.newSg)
      }
    }

    // TODO TODO TODO - add a feature c1.isMatch(c2, allowRotation=True) and it tells you what rotation to use!
    // TODO - for now, hacking this together ...
    //val allOptions = possibleConnections(existing, newGroup) ++ possibleConnections(existing, newGroup.rotateCW)
    Seq(newGroup, newGroup.rotateCW, newGroup.rotate180, newGroup.rotateCCW).flatMap { g =>
      possibleConnections(existing, g)
    }
  }

  /** TODO - copied from Sushi Builder (and modified) */
  def tryPasteConnectedTo(writer: MapWriter, existing: PastedSectorGroup, newGroup: SectorGroup): Option[PastedSectorGroup] = {

    val allOptions = pasteOptions(writer, existing, newGroup)
    if(allOptions.size < 1){
      None
    }else{
      //val (c1, c2, g) = random.randomElement(allOptions)
      val p = writer.randomElement(allOptions)
      //Some(pasteAndLink(c1, g, c2))
      Some(writer.pasteAndLink(p.existing, p.newSg, p.newConn))
    }
  }


  def available(sg: SectorGroup): Boolean = {
    if(sg.sectorGroupId == -1 || sg.hints.maxCopies.filter(i => i > 0).isEmpty){
      true
    }else{
      val maxCopies = sg.hints.maxCopies.get
      val copies = sgBuilder.pastedSectorGroups.filter(psg => psg.groupId == Some(sg.sectorGroupId)).size
      copies < maxCopies
    }
  }

}


// TODO - this is for testing the blueprint stuff -- make the prefab file also a unit test input!
object PoolExperiment {
  val FILENAME = "pool.map"

  def hasPlayerStart(psg: PastedSectorGroup): Boolean = psg.allSprites.exists { s =>
    s.getTexture == MapWriter.MarkerTex && s.getLotag == PrefabUtils.MarkerSpriteLoTags.PLAYER_START
  }


  def run(mapLoader: MapLoader): DMap = {
    val sourceMap = mapLoader.load(FILENAME)

    // TODO - a map with a SG that is a box with 4 sides, all with lotag 1, causes this to run out of memory:
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new PoolBuilder(DMap.createNew())
    val writer = MapWriter(builder)
    def randomAvailable(groups: Iterable[SectorGroup]): Option[SectorGroup] = {
      writer.randomElementOpt(groups.filter(builder.available))
    }

    val stays = writer.pasteStays(palette)

    var psg = stays.filter(hasPlayerStart).headOption.getOrElse{
      val startSg = writer.randomElement(palette.allSectorGroups.asScala.filter(_.hasPlayerStart))
      writer.builder.pasteSectorGroup(startSg, PointXYZ.ZERO) // TODO: need to make sure the area is clear first!
    }

    for(_ <- 0 until 35){
      val sg = randomAvailable(palette.allSectorGroups.asScala).get
      val spots = writer.random.shuffle(writer.sgBuilder.pastedSectorGroups)
      spots.find { psg =>
        builder.tryPasteConnectedTo(writer, psg, sg).isDefined
      }
    }

    writer.sgBuilder.autoLinkRedwalls()
    writer.builder.setAnyPlayerStart()
    writer.sgBuilder.clearMarkers()
    writer.outMap
  }
}
