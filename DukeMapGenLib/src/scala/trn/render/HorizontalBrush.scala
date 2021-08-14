package trn.render

import trn.Sector

/**
  * Describes the appearance of a floor, or a ceiling.
  */
case class HorizontalBrush(
  tex: Option[Int],
  shade: Option[Int],
  pal: Option[Int],
  relative: Option[Boolean],
  smaller: Option[Boolean]   // a.k.a. "double smooshiness"
) {
  def withShade(s: Int): HorizontalBrush = this.copy(shade=Some(s))
  def withPal(pal: Int): HorizontalBrush = this.copy(pal=Some(pal))
  def withRelative(b: Boolean): HorizontalBrush = this.copy(relative=Some(b))
  def withSmaller(b: Boolean): HorizontalBrush = this.copy(smaller=Some(b))

  def writeToFloor(sector: Sector): Unit = {
    tex.foreach(picnum => sector.setFloorTexture(picnum))
    shade.foreach(s => sector.setFloorShade(s.toShort))
    pal.foreach(p => sector.setFloorPalette(p))
    relative.foreach(b => sector.setFloorRelative(b))
    smaller.foreach(b => sector.setFloorSmaller(b))
  }

  def writeToCeil(sector: Sector): Unit = {
    tex.foreach(picnum => sector.setCeilingTexture(picnum))
    shade.foreach(s => sector.setCeilingShade(s.toShort))
    pal.foreach(p => sector.setCeilingPalette(p))
    relative.foreach(b => sector.setCeilingRelative(b))
    smaller.foreach(b => sector.setCeilingSmaller(b))
  }
}

object HorizontalBrush {

  def apply(): HorizontalBrush = {
    HorizontalBrush(None, None, None, None, None)
  }

  def apply(tex: Int): HorizontalBrush = {
    HorizontalBrush(Some(tex), None, None, None, None)
  }

  def apply(tex: Texture): HorizontalBrush = apply(tex.picnum)

}
