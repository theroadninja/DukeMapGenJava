package trn.prefab

import trn.{Sector, WallView}

/**
  * Lets you read information from a sector group when you dont care if it is pasted or not.
  */
trait ReadOnlySectorGroup {

  def getWallView(wallId: Int): WallView

  def getSector(sectorId: Int): Sector

  // TODO behavior is undefined if there is more than one connector with that id; it should throw instead
  def getConnector(connectorId: Int): Connector

  // TODO behavior is undefined if there is more than one connector with that id; it should throw instead
  def getRedwallConnector(connectorId: Int): RedwallConnector

  /**
    * Get all "compass" connectors that match a certain direction.
    *
    * A compass connector is a subset of redwall connector that is axis aligned
    * @param heading the direction of the connector, e.g. "east" means the empty
    *                space where the other connector goes is in the +X direction
    */
  def getCompassConnectors(heading: Int): Seq[RedwallConnector]

}
