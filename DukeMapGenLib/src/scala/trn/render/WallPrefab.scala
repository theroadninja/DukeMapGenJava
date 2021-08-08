package trn.render

import trn.{PointXY, Wall}

/**
  * Older version of this class:  trn.duke.experiments.WallPrefab
  */
case class WallPrefab(
  tex: Option[Texture],

  xrepeat: Option[Int], // the official x-repeat value which is NOT how many times it repeats -- see XRepeat.md

  xTileCount: Option[Int]  // the number of times the texture is repeated
  // TODO? xscale: Option[Double]  // TODO make sure this cannot be set along with x-repeat
){
  require(xrepeat.isEmpty || xTileCount.isEmpty)
  require(tex.isDefined || xTileCount.isEmpty)

  def writeTo(wall: Wall): Unit = {
    tex.foreach(t => wall.setTexture(t.picnum))
    xrepeat.foreach(xr => wall.setXRepeat(xr))
    xTileCount.foreach { repeat =>
      val xrepeat = repeat * tex.get.widthPx / 8
      wall.setXRepeat(xrepeat)
    }


    wall.setYRepeat(8) // TODO

    // TODO ensure x and y repeat get set to 8 if nothing is passed in
  }

  def create(p: PointXY): Wall = {
    val wall = new Wall(p.x, p.y)
    this.writeTo(wall)
    wall
  }
}


object WallPrefab {

  def apply(tex: Texture): WallPrefab = WallPrefab(Some(tex), Some(8), None)
}
