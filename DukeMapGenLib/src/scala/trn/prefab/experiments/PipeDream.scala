package trn.prefab.experiments

import trn.prefab._
import trn.{MapLoader, PointXY, PointXYZ, Wall, Map => DMap}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random


class PipeBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {
  val writer = new MapWriter(this, sgBuilder) // TODO

  def sectorCount: Int = sgBuilder.sectorCount

  def spaceAvailable(b: BoundingBox): Boolean = writer.spaceAvailable(b)

  //def pasteAndLink(existing: RedwallConnector, newSg: SectorGroup, newConn: RedwallConnector): PastedSectorGroup =
  //  writer.pasteAndLink(existing, newSg, newConn)

  // -----------------

  // TODO - does this belong in a more generic place?
  def openConnectors: Seq[RedwallConnector] = writer.pastedSectorGroups.flatMap{ psg =>
    psg.unlinkedConnectors.collect{case cc: RedwallConnector => cc}
  }


  // these are basically just an optimization of SgMapBuilder.autoLinkRedwalls()
  // def findUnlinkedRedwallConnectors(): Seq[RedwallConnector] = {
  //   pastedSectorGroups.flatMap(_.unlinkedConnectors).flatMap { c =>
  //     c match {
  //       case n: RedwallConnector => Some(n)
  //       case _ => None
  //     }
  //   }
  // }
  // /**
  //   * Given a connector in a pasted sector group, see if it has been pasted on top of a perfectly matching
  //   * connector, and link them.
  //   * @param pastedConnector
  //   */
  // def findAndLinkMatch(pastedConnector: RedwallConnector): Int = {
  //   var count = 0
  //   val unlinked: Seq[RedwallConnector] = this.findUnlinkedRedwallConnectors()

  //   val cr = pastedConnector
  //   val cOpt = unlinked.find(c0 => c0.isMatch(cr) && cr.getTransformTo(c0).equals(PointXYZ.ZERO))
  //   cOpt.foreach{ ccc =>
  //     ccc.linkConnectors(this.outMap, cr)
  //     count += 1
  //     println("FIND AND LINK SUCCESSFUL")
  //   }

  //   count
  // }

}


object PipeDream extends PrefabExperiment {
  val Filename = "pipe.map"

  // TODO - next: support multiwall connectors

  // TODO - idea: priorities connectors on groups with more open connectors?
  // (i.e. check ratio)

  override def run(mapLoader: MapLoader): DMap = {
    val sourceMap = mapLoader.load(Filename)
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new PipeBuilder(DMap.createNew(), palette)

    val SIZE = 40


    // TODO - implement multi-wall-multi-sector connector
    // val sanityCheck = palette.getSectorGroup(13).getConnector(101).asInstanceOf[RedwallConnector]
    // val sanityCheck2 = palette.getSectorGroup(700).getConnector(101).asInstanceOf[RedwallConnector]
    // throwIf(sanityCheck.isMatch(sanityCheck2))

    def randomItem[T](list: Seq[T]): T = {
      if(list.size < 1) throw new IllegalArgumentException
      list(Random.nextInt(list.size))
    }

    def matchingSectorGroups(existingConn: RedwallConnector): Iterable[(SectorGroup, RedwallConnector)] = {
      val allSgs = palette.allSectorGroups().asScala
      allSgs
        .flatMap(sg => Seq(sg, sg.rotateCW, sg.rotate180, sg.rotate180.rotateCW))
        .flatMap{ sg =>
        val conns = sg.allRedwallConnectors
            .filter(c2 => c2.isMatch(existingConn))
            .filter{c2 =>
              val bb = sg.boundingBox
              val cdelta: PointXYZ = c2.getTransformTo(existingConn)
              builder.sectorCount + sg.sectorCount <= DMap.MAX_SECTOR_GROUPS &&
                builder.spaceAvailable(bb.translate(cdelta.asXY()))
            }
        conns.map((sg, _))
      }
    }

    //val stays = palette.getStaySectorGroups.asScala
    //stays.foreach { sg =>
    //  builder.pasteSectorGroup(sg, PointXYZ.ZERO) // no translate == leave where it is
    //}
    builder.writer.pasteStays(palette)

    // TODO - check for existing sector groups also!

    //builder.pasteSectorGroup(palette.getSectorGroup(2), PointXYZ.ZERO)

    var linked0 = 0
    for(_ <- 0 to SIZE){
      //1. pick an open connector
      // TODO - refactor PastedSectorGroup so it is easy to find unliked connectors
      val openConnectors = builder.openConnectors
      val existingConnectors = openConnectors.filter(matchingSectorGroups(_).nonEmpty)
      println(s"open connectors: ${openConnectors.size} available: ${existingConnectors.size} sector count: ${builder.sectorCount}")

      if(existingConnectors.size > 0){
        //1b. randomly select one from the list
        val c: RedwallConnector = randomItem(existingConnectors)

        //2. pick a SG that can match
        val matchingSgs = matchingSectorGroups(c)
        //println(matchingSgs.map(_._1.getGroupId))

        //3. paste the SG
        if(matchingSgs.nonEmpty){
          val (newSg, newSgConn) = randomItem(matchingSgs.toSeq)
          val psg = builder.writer.pasteAndLink(c, newSg, newSgConn)
          //val psg = builder.pasteAndLink(c, newSg, newSgConn)

          // 4. see if any open connectors happen to be next to each other
          // linked0 += psg.unlinkedRedwallConnectors.map(builder.findAndLinkMatch(_)).sum
        }

      }

    }
    val linked = builder.sgBuilder.autoLinkRedwalls()
    println(s"linked ${linked0} connectors by old method")
    println(s"linked ${linked} connectors by new method")

    println(s"Sector count: ${builder.outMap.getSectorCount}")
    builder.writer.setAnyPlayerStart()
    builder.writer.clearMarkers()
    builder.outMap
  }
}
