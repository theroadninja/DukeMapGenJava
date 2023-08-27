package trn.prefab.experiments.subway

import trn.duke.TextureList
import trn.Sprite
import trn.math.RotatesCW
import trn.prefab.{SectorGroup, SpriteLogicException, PrefabPalette, RedwallConnector}

import scala.collection.mutable
import scala.collection.JavaConverters._


case class PlatformEdge(sg: SectorGroup) extends RotatesCW[PlatformEdge] {
  // TODO this seems generic
  lazy val hasGate = sg.allSprites.find(s => TextureList.isLock(s.tex())).isDefined

  lazy val connToTrack: RedwallConnector = sg.getRedwallConnector(ConnectionIds.TypeA)

  override def rotatedCW: PlatformEdge = PlatformEdge(sg.rotateCW)
}

case class PlatformArea(sg: SectorGroup) {
  // TODO isInner, etc
}

case class Door(sg: SectorGroup) {
  lazy val hasGate = sg.allSprites.find(s => TextureList.isLock(s.tex())).isDefined

}

case class SpecialArea(sg: SectorGroup) {

}


object SubwayPalette1 {

  //val Track = 0
  //val PlatformEdge = 1
  //val PlatformArea = 2
  //val Door = 3

  def getConn(sg: SectorGroup, connId: Int): Option[RedwallConnector] = sg.allRedwallConnectors.find(_.getConnectorId == connId)

  // def hasLock(sg: SectorGroup): Boolean = {
  //   sg.allSprites.find(s => TextureList.isLock(s.tex())).isDefined
  // }

  def expectConn(sg: SectorGroup, connId: Int, errorMsg: String): RedwallConnector = {
    getConn(sg, connId).getOrElse {
      throw new SpriteLogicException(errorMsg, sg)
    }
  }

  /** works for track on and/or type A, type B */
  def checkTrackConn(conn: RedwallConnector): Unit = {
    if (!conn.isAxisAligned) {
      throw new SpriteLogicException("Track/Type/A/B Redwall Connector is not axis aligned", conn)
    }
    if (conn.totalManhattanLength() != ConnectionIds.ExpectedLengthA) {
      throw new SpriteLogicException(s"Track/Type/A/B RedwallConnector length ${conn.totalManhattanLength()} != ${ConnectionIds.ExpectedLengthA}")
    }
    if (conn.getWallCount != 1) {
      throw new SpriteLogicException(s"Track/Type/A/B Redwall Connector wall count ${conn.getWallCount} != 1")
    }
  }

  /** only returns connectors in the track sector group */
  def getTrackConns(sg: SectorGroup): Seq[RedwallConnector] = {
    val conns = sg.allRedwallConnectors.filter(conn => ConnectionIds.Track.contains(conn.getConnectorId))
    conns.foreach { conn => checkTrackConn(conn) }
    conns
  }

  def checkPlatformEdge(sg: SectorGroup): Unit = {
    val connCount = sg.allRedwallConnectors.size
    if (connCount < 1) {
      throw new SpriteLogicException("Platform Edge group has no Redwall Connectors")
    } else if (sg.allRedwallConnectors.size != 2) {
      throw new SpriteLogicException(s"Platform Edge group must have exactly two connectors but has ${sg.allRedwallConnectors.size}", sg.allRedwallConnectors.head)
    }
    val trackConn = expectConn(sg, ConnectionIds.TypeA, "Platform group missing type A Redwall Connector")
    val areaConn = expectConn(sg, ConnectionIds.TypeB, "Platform group missing type B Redwall Connector")
    checkTrackConn(trackConn)
    checkTrackConn(areaConn)
  }

  def isPlatformEdge(sg: SectorGroup): Boolean = {
    // platform edge is the ONLY group to have TypeA, because that matches the individual track connectors
    getConn(sg, ConnectionIds.TypeA).map { _ =>
      checkPlatformEdge(sg)
      true
    }.getOrElse(false)
  }

  def isPlatformArea(sg: SectorGroup): Boolean = if (getConn(sg, ConnectionIds.TypeA).isEmpty && getConn(sg, ConnectionIds.TypeB).isDefined) {
    // Has TypeB(edge<->area) but no TypeA(track<->edge)
    if (sg.allRedwallConnectors.filter(conn => conn.getConnectorId == ConnectionIds.TypeC).size < 1) {
      throw new SpriteLogicException(s"Platform Area must have at least one Redwall Connector with id ${ConnectionIds.TypeC}")
    }
    true
  } else {
    false
  }

  def isDoor(sg: SectorGroup): Boolean = {
    if(getConn(sg, ConnectionIds.TypeC).isDefined && getConn(sg, ConnectionIds.TypeD).isDefined){
      if(sg.allRedwallConnectors.size != 2){
        throw new SpriteLogicException("Door group has extra connectors", sg)
      }
      true
    }else{
      false
    }
  }

  def isSpecialArea(sg: SectorGroup): Boolean = {
    if(getConn(sg, ConnectionIds.TypeD).isDefined && getConn(sg, ConnectionIds.TypeC).isEmpty){
      true
    }else{
      false
    }
  }

  def apply(palette: PrefabPalette): SubwayPalette1 = {
    val trackSGs = mutable.ArrayBuffer[SectorGroup]()
    val edges = mutable.ArrayBuffer[PlatformEdge]()
    val areas = mutable.ArrayBuffer[PlatformArea]()
    val doors = mutable.ArrayBuffer[Door]()
    val specialAreas = mutable.ArrayBuffer[SpecialArea]()

    palette.allSectorGroups().asScala.foreach { sg =>
      val trackConns = getTrackConns(sg)
      if (trackConns.size == ConnectionIds.Track.size) {
        // is the track SG
        trackSGs.append(sg)
      } else if (trackConns.size > 0) {
        throw new SpriteLogicException(s"sector group has wrong number of track connections ${trackConns.size} != ${ConnectionIds.Track.size}", trackConns.head)
      } else {
        if (isPlatformEdge(sg)) {
          edges.append(PlatformEdge(sg))
        }else if(isPlatformArea(sg)){
          areas.append(PlatformArea(sg))
        }else if(isDoor(sg)){
          doors.append(Door(sg))
        }else if(isSpecialArea(sg)){
          specialAreas.append(SpecialArea(sg))
        }
      }
    }

    if(trackSGs.size != 1){
      throw new SpriteLogicException(s"There must be exactly 1 track group; detected ${trackSGs.size}")
    }
    // val edgesWithGates = edges.filter(_.hasGate)
    val (edgesWithGates, edgesWithoutGates) = edges.partition(_.hasGate)
    val (doorsWithGates, doorsWithoutGates) = doors.partition(_.hasGate)

    SubwayPalette1(
      trackSGs.head,
      edgesWithGates,
      edgesWithoutGates,
      areas,
      doorsWithGates,
      doorsWithoutGates,
    )
  }

}

case class SubwayPalette1(
  trackSg: SectorGroup, // must be sg 1
  platformEdgesWithGates: Seq[PlatformEdge],
  platformEdgesWithoutGates: Seq[PlatformEdge],
  platformAreas: Seq[PlatformArea],
  doorsWithGates: Seq[Door],
  doorsWithoutGates: Seq[Door],
) {

}