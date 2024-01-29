package duchy.experiments.render.maze

import trn.{WallBrush, Wall}

object WallPrefab {

  def apply(tex: Int): WallPrefab = WallPrefab(
    texture=tex,
    xrepeat=Some(16),
    yrepeat=Some(8),
    shade=None,
  )

  def apply(copyMe: WallPrefab): WallPrefab = copyMe.copy()

}

/**
  * Legacy class
  *
  * TODO there is a newer one at trn.render.WallPrefab
  */
case class WallPrefab (
  texture: Integer,
  var xrepeat: Option[Short],
  var yrepeat: Option[Short],
  var shade: Option[Short],
) extends WallBrush {

  override def writeToWall(w: Wall): Unit = {
    w.setTexture(texture)
    xrepeat.foreach(w.setXRepeat)
    yrepeat.foreach(w.setYRepeat)
    shade.foreach(w.setShade)
  }

  def withTexture(tex: Int): WallPrefab = this.copy(texture=tex)

  def setXRepeat(xrepeat: Int): WallPrefab = {
    this.xrepeat = Some(xrepeat.toShort)
    this
  }

  def setYRepeat(yrepeat: Int): WallPrefab = {
    this.yrepeat = Some(yrepeat.toShort)
    this
  }

  def setShade(shade: Short): WallPrefab = {
    this.shade = Some(shade)
    this
  }

  def writeToWalls(walls: Wall*): Unit = {
    for (w <- walls) {
      writeToWall(w)
    }
  }
}
