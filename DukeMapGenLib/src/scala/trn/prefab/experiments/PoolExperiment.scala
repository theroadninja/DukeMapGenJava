package trn.prefab.experiments

import trn.prefab._
import trn.{MapLoader, PointXYZ, Map => DMap}

import scala.collection.JavaConverters._

class PoolBuilder(val outMap: DMap) extends MapBuilder {
  // mostly a test to see if too many copies have been added
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
object PoolExperiment extends PrefabExperiment {
  val Filename = "pool.map"

  def hasPlayerStart(psg: PastedSectorGroup): Boolean = psg.allSprites.exists { s =>
    s.getTexture == MapWriter.MarkerTex && s.getLotag == PrefabUtils.MarkerSpriteLoTags.PLAYER_START
  }

  override def run(mapLoader: MapLoader): DMap = {
    val sourceMap = mapLoader.load(Filename)

    // TODO - a map with a SG that is a box with 4 sides, all with lotag 1, causes this to run out of memory:
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new PoolBuilder(DMap.createNew())
    val writer = MapWriter(builder)
    def randomAvailable(groups: Iterable[SectorGroup]): Option[SectorGroup] = {
      writer.randomElementOpt(groups.filter(builder.available))
    }

    val stays = writer.pasteStays(palette)

    // if none of the "stay" groups has a play start, add one
    var psg = stays.filter(hasPlayerStart).headOption.getOrElse{
      val startSg = writer.randomElement(palette.allSectorGroups.asScala.filter(_.hasPlayerStart))
      writer.builder.pasteSectorGroup(startSg, PointXYZ.ZERO) // TODO: need to make sure the area is clear first!
    }

    for(_ <- 0 until 35){
      val sg = randomAvailable(palette.allSectorGroups.asScala).get
      val spots = writer.random.shuffle(writer.sgBuilder.pastedSectorGroups)
      spots.find { psg =>
        ExperimentalWriter.tryPasteConnectedTo(writer, writer.random, psg, sg).isDefined
      }
    }

    writer.sgBuilder.autoLinkRedwalls()
    writer.builder.setAnyPlayerStart()
    writer.sgBuilder.clearMarkers()
    writer.outMap
  }
}
