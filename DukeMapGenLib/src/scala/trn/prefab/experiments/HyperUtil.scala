package trn.prefab.experiments

import trn.PointXYZ
import trn.prefab.{MapWriter, PastedSectorGroup}

import scala.collection.mutable

/**
  * Creating this for code shared between Hypercube implementations.
  */
object HyperUtil {

  /**
    * try to link all teleporters between the two groups, matching them based on their "connector id" only.
    *
    * TODO: the lower vs higher thing doesnt matter
    *
    * @param writer
    * @param psgLower
    * @param psgHigher
    */
  def tryLinkTeleporters(writer: MapWriter, psgLower: PastedSectorGroup, psgHigher: PastedSectorGroup): Boolean = {
    var linked = false
    psgLower.allTeleportConnectors.foreach { connA =>
      psgHigher.allTeleportConnectors.foreach { connB =>
        if(connA.getConnectorId == connB.getConnectorId){
          writer.sgBuilder.linkTeleporters(connA, psgLower, connB, psgHigher)
          linked = true
        }
      }
    }
    linked
  }

  /**
    * try to link all elevators between the two groups, matching them based on their "connector id" only.
    * @param writer
    * @param psgLower
    * @param psgHigher
    */
  def tryLinkAllElevators(writer: MapWriter, psgLower: PastedSectorGroup, psgHigher: PastedSectorGroup): Unit = {
    for(lower <- psgLower.allElevatorConnectors; higher <- psgHigher.allElevatorConnectors){
      if(lower.getConnectorId == higher.getConnectorId){
        writer.linkElevators(lower, higher, true)
      }
    }
  }

}

/** translates between coordinate systems: between an abstract "grid" of rooms, an actual duke xy coordinates */
trait GridManager4D {

  /**
    *
    * @param gridCell the x,y,z,w position of the abstract cell in the logical grid
    * @return the x, y, z coordinate of the cell in build coordinates (center vs topleft is up to the caller to decide)
    */
  def toXYZ(gridCell: (Int, Int, Int, Int)): PointXYZ

  def maxGridY: Int
  def maxGridX: Int
}
