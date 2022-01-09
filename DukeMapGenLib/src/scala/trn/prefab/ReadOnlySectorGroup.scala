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

}
