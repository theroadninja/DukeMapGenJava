package trn.render

import trn.Sector

/**
  * Describes the appearance of a floor, or a ceiling.
  */
case class HorizontalBrush(
  tex: Option[Int],
  shade: Option[Int],
  pal: Option[Int],
  xpan: Option[Int],
  ypan: Option[Int],
  parallax: Option[Boolean],
  slope: Option[Int],
  relative: Option[Boolean],
  swapXY: Option[Boolean],
  smaller: Option[Boolean]   // a.k.a. "double smooshiness"
) {
  def withShade(s: Int): HorizontalBrush = this.copy(shade=Some(s))
  def withPal(pal: Int): HorizontalBrush = this.copy(pal=Some(pal))
  def withXPan(x: Int): HorizontalBrush = this.copy(xpan=Some(x))
  def withYPan(y: Int): HorizontalBrush = this.copy(ypan=Some(y))

  def withParallax(b: Boolean = true): HorizontalBrush = this.copy(parallax=Some(b))
  def withSlope(angle: Int): HorizontalBrush = this.copy(slope=Some(angle))
  def withRelative(b: Boolean = true): HorizontalBrush = this.copy(relative=Some(b))
  def withSwapXY(b: Boolean = true): HorizontalBrush = this.copy(swapXY=Some(b))
  def withSmaller(b: Boolean = true): HorizontalBrush = this.copy(smaller=Some(b))

  def writeToFloor(sector: Sector): Unit = {
    tex.foreach(picnum => sector.setFloorTexture(picnum))
    shade.foreach(s => sector.setFloorShade(s.toShort))
    pal.foreach(p => sector.setFloorPalette(p))
    xpan.foreach(x => sector.setFloorXPan(x))
    ypan.foreach(y => sector.setFloorYPan(y))
    parallax.foreach(b => sector.setFloorParallax(b))
    slope.foreach { angle =>
      sector.setFloorSloped(true)
      sector.setFloorSlope(angle)
    }
    relative.foreach(b => sector.setFloorRelative(b))
    swapXY.foreach(b => sector.setFloorSwapXY(b))
    smaller.foreach(b => sector.setFloorSmaller(b))
  }

  def writeToCeil(sector: Sector): Unit = {
    tex.foreach(picnum => sector.setCeilingTexture(picnum))
    shade.foreach(s => sector.setCeilingShade(s.toShort))
    pal.foreach(p => sector.setCeilingPalette(p))
    xpan.foreach(x => sector.setCeilingXPan(x))
    ypan.foreach(y => sector.setCeilingYPan(y))
    parallax.foreach(b => sector.setCeilingParallax(b))
    slope.foreach { angle =>
      sector.setCeilingSloped(true)
      sector.setCeilingSlope(angle)
    }
    relative.foreach(b => sector.setCeilingRelative(b))
    swapXY.foreach(b => sector.setCeilingSwapXY(b))
    smaller.foreach(b => sector.setCeilingSmaller(b))
  }
}

object HorizontalBrush {

  def apply(): HorizontalBrush = {
    HorizontalBrush(None, None, None, None, None, None, None, None, None, None)
  }

  def apply(tex: Int): HorizontalBrush = {
    HorizontalBrush(Some(tex), None, None, None, None, None, None, None, None, None)
  }

  def apply(tex: Texture): HorizontalBrush = apply(tex.picnum)

}
