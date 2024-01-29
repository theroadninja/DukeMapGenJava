package duchy.experiments.render.maze

import trn.Sector

class SectorPrefab(floorTexture: Int, ceilingTexture: Int) {
  var floorShade: Option[Short] = None // this was copied from java code
  var ceilingShade: Option[Short] = None

  def setFloorShade(floorShade: Short): SectorPrefab = {
    this.floorShade = Some(floorShade)
    this
  }

  def setCeilingShade(ceilingShade: Short): SectorPrefab = {
    this.ceilingShade = Some(ceilingShade)
    this
  }

  def writeTo(sector: Sector): Unit = {
    sector.setFloorTexture(floorTexture)
    sector.setCeilingTexture(ceilingTexture)
    this.floorShade.foreach(sector.setFloorShade)
    this.ceilingShade.foreach(sector.setCeilingShade)
  }
}
