package trn.prefab.experiments.hyperloop

import trn.duke.TextureList
import trn.{Sprite, PointXYZ}

/**
  * I think this will be a nice facade over the java object constructors, however I'm not sure about it yet (its baking
  * more duke logic into the code).
  */
object SpriteFactory {

  /**
    * Creates a touchplate sprite
    *
    * @param loc
    * @param sectorId
    * @param channel the lotag of the activator to activate
    * @param activationCount how many times to activate (0 means infinite)
    * @return
    */
  def touchplate(loc: PointXYZ, sectorId: Int, channel: Int, activationCount: Int = 1): Sprite = {
    val touch = new Sprite(loc.asXY, loc.z, sectorId)
    touch.setTexture(TextureList.TOUCHPLATE)
    touch.setHiTag(activationCount)
    touch.setLotag(channel)
    touch
  }

  def sprite(loc: PointXYZ, sectorId: Int, tex: Int, hitag: Int = 0, lotag: Int = 0): Sprite = {
    val s = new Sprite(loc.asXY, loc.z, sectorId)
    s.setTexture(tex)
    s.setHiTag(hitag)
    s.setLotag(lotag)
    s
  }

}
