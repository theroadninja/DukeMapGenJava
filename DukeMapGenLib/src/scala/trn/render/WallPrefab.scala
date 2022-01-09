package trn.render

import trn.prefab.{DukeConfig, TexturePack}
import trn.{PointXY, Wall, WallStat}

/**
  * Older version of this class:  trn.duke.experiments.WallPrefab
  *
  * TODO:  make a distinction between structure vs appearance.  This is only for appearance.
  *
  * TODO:  call this WallBrush ?
  */
case class WallPrefab(
  tex: Option[Texture],
  overpic: Option[Texture],
  shade: Option[Int],
  pal: Option[Int],

  xrepeat: Option[Int], // the official x-repeat value which is NOT how many times it repeats -- see XRepeat.md
  yrepeat: Option[Int],
  xpan: Option[Int],
  ypan: Option[Int],

  xTileRepeat: Option[Int],  // the number of times the texture is repeated

  // TODO  can't use this directly:  we need the texture pack AND the wall length!
  xscale: Option[Double],

  blockable: Option[Boolean],
  alignBottom: Option[Boolean],
  xflip: Option[Boolean],
  mask: Option[Boolean],
  hitscan: Option[Boolean],
  transparent: Option[Boolean],
){
  require(xrepeat.isEmpty || xTileRepeat.isEmpty)
  require(tex.isDefined || xTileRepeat.isEmpty)

  def writeTo(wall: Wall): Unit = {
    tex.foreach(t => wall.setTexture(t.picnum))
    overpic.foreach(t => wall.setMaskTex(t.picnum))
    shade.foreach(s => wall.setShade(s.toShort))
    pal.foreach(p => wall.setPal(p))

    xrepeat.foreach(xr => wall.setXRepeat(xr))
    yrepeat.foreach(yr => wall.setYRepeat(yr))
    xTileRepeat.foreach { repeat =>
      val xrepeat = repeat * tex.get.widthPx / 8
      wall.setXRepeat(xrepeat)
    }
    xpan.foreach(x => wall.setXPanning(x))
    ypan.foreach(y => wall.setYPanning(y))

    blockable.foreach(b => wall.setStat(wall.getStat.withValueChanged(WallStat.BLOCKABLE, b)))
    alignBottom.foreach{ b =>
      wall.setStat(wall.getStat.withAlignBottom(b))
    }
    xflip.foreach(b => wall.setStat(wall.getStat.withValueChanged(WallStat.XFLIP, b)))
    mask.foreach(b => wall.setStat(wall.getStat.withValueChanged(WallStat.MASK_2SIDE, b)))
    hitscan.foreach(b => wall.setStat(wall.getStat.withValueChanged(WallStat.HITSCAN, b)))
    transparent.foreach(b => wall.setStat(wall.getStat.withValueChanged(WallStat.TRANSPARENT, b)))
  }

  def withOverpic(tex: Texture): WallPrefab = this.copy(overpic=Option(tex))

  def withXRepeatForNRepetitions(repetitionCount: Int): WallPrefab = {
    this.copy(xrepeat=Some(tex.get.xRepeatForNRepetitions(repetitionCount)))
  }

  def withXRepeatForScale(scaleFactor: Double, wallSize: Int): WallPrefab = {
    this.copy(xrepeat=Some(tex.get.xRepeatForScaleF(scaleFactor, wallSize)))
  }

  /** set the raw xrepeat and yrepeat values */
  def withRepeats(xr: Int, yr: Int): WallPrefab = this.copy(xrepeat=Some(xr), yrepeat=Some(yr))
  def withXScale(scale: Double): WallPrefab = this.copy(xscale=Some(scale))

  def withXPan(xp: Int): WallPrefab = this.copy(xpan=Some(xp))
  def withYPan(yp: Int): WallPrefab = this.copy(ypan=Some(yp))

  def create(p: PointXY): Wall = {
    val wall = new Wall(p.x, p.y)
    this.writeTo(wall)
    wall
  }

  def withShade(s: Int): WallPrefab = this.copy(shade=Some(s))

  def withPal(pal: Int): WallPrefab = this.copy(pal=Some(pal))

  def withBlockable(b: Boolean = true): WallPrefab = this.copy(blockable=Some(b))
  def withAlignBottom(b: Boolean = true): WallPrefab = this.copy(alignBottom=Some(b))
  def withXflip(b: Boolean = true): WallPrefab = this.copy(xflip=Some(b))
  def withMask(b: Boolean = true): WallPrefab = this.copy(mask=Some(b))
  def withHitscan(b: Boolean = true): WallPrefab = this.copy(hitscan=Some(b))
  def withTransparent(b: Boolean = true): WallPrefab = this.copy(transparent=Some(b))
}


object WallPrefab {

  def Empty = WallPrefab(Texture(0, 64))

  def apply(tex: Texture): WallPrefab = {
    WallPrefab(Some(tex), None, None, None, Some(8), Some(8), None, None, None, None, None, None, None, None, None, None)
  }

  def apply(tex: Int, texWidth: Int): WallPrefab = WallPrefab(Texture(tex, texWidth))
}
