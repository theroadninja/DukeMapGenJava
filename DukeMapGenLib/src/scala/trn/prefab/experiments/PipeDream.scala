package trn.prefab.experiments

import trn.prefab._
import trn.{MapLoader, PointXY, PointXYZ, Wall, Map => DMap}

import scala.collection.JavaConverters._
import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.util.Random


class PipeBuilder(val outMap: DMap, palette: PrefabPalette) extends MapBuilder {

  def sectorCount: Int = outMap.getSectorCount

  def openConnectors: Seq[RedwallConnector] = pastedSectorGroups.flatMap{ psg =>
      psg.unlinkedConnectors.collect{case cc: RedwallConnector => cc}
    }

  def pasteAndLink(
    existingConn: RedwallConnector,
    newSg: SectorGroup,
    newConn: RedwallConnector
  ): PastedSectorGroup = {
    val cdelta = newConn.getTransformTo(existingConn)
    val (psg, idmap) = pasteSectorGroup2(newSg, cdelta)
    val pastedConn2 = newConn.translateIds(idmap, cdelta)
    existingConn.linkConnectors(outMap, pastedConn2)
    psg
  }

  def findUnlinkedRedwallConnectors(): Seq[RedwallConnector] = {
    pastedSectorGroups.flatMap(_.unlinkedConnectors).flatMap { c =>
      c match {
        case n: RedwallConnector => Some(n)
        case _ => None
      }
    }
  }

  /**
    * Given a connector in a pasted sector group, see if it has been pasted on top of a perfectly matching
    * connector, and link them.
    * @param pastedConnector
    */
  def findAndLinkMatch(pastedConnector: RedwallConnector): Unit = {
    val unlinked: Seq[RedwallConnector] = this.findUnlinkedRedwallConnectors()

    val cr = pastedConnector
    val cOpt = unlinked.find(c0 => c0.isMatch(cr) && cr.getTransformTo(c0).equals(PointXYZ.ZERO))
    cOpt.foreach{ ccc =>
      ccc.linkConnectors(this.outMap, cr)
      println("FIND AND LINK SUCCESSFUL")
    }

  }


  /**
    * Tests if the given box is available (empty).  This tests the entire (axis-aligned) box, which means it is
    * an inefficient measure for sector groups that are not box-like.
    * @param bb the bounding box
    * @return true if the bounding box is in bounds and there are no existing groups in that area.
    */
  def spaceAvailable(bb: BoundingBox): Boolean = {
    bb.isInsideInclusive(MapBuilder.mapBounds) &&
      pastedSectorGroups.filter(psg => psg.boundingBox.intersect(bb).map(_.area).getOrElse(0) > 0).isEmpty
  }
}


object PipeDream {
  val FILENAME = "pipe.map"

  // TODO - next: support multiwall connectors

  // TODO - idea: priorities connectors on groups with more open connectors?
  // (i.e. check ratio)

  def run(mapLoader: MapLoader): DMap = {
    val sourceMap = mapLoader.load(FILENAME)
    val palette: PrefabPalette = PrefabPalette.fromMap(sourceMap);
    val builder = new PipeBuilder(DMap.createNew(), palette)


    val sanityCheck = palette.getSectorGroup(13).getConnector(101).asInstanceOf[RedwallConnector]
    val sanityCheck2 = palette.getSectorGroup(700).getConnector(101).asInstanceOf[RedwallConnector]
    require(sanityCheck.isMatch(sanityCheck2))

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
              builder.sectorCount + sg.getSectorCount <= MapBuilder.MAX_SECTOR_GROUPS &&
                builder.spaceAvailable(bb.translate(cdelta.asXY()))
            }
        conns.map((sg, _))
      }
    }

    val stays = palette.getStaySectorGroups.asScala
    stays.foreach { sg =>
      builder.pasteSectorGroup(sg, PointXYZ.ZERO) // no translate == leave where it is
    }

    // TODO - check for existing sector groups also!

    //builder.pasteSectorGroup(palette.getSectorGroup(2), PointXYZ.ZERO)

    for(_ <- 0 to 60){
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
          val psg = builder.pasteAndLink(c, newSg, newSgConn)

          //4. see if any open connectors happen to be next to each other
          psg.unlinkedRedwallConnectors.foreach(builder.findAndLinkMatch(_))
        }

      }

    }

    println(s"Sector count: ${builder.outMap.getSectorCount}")
    builder.setAnyPlayerStart()
    builder.clearMarkers()
    builder.outMap
  }
}
