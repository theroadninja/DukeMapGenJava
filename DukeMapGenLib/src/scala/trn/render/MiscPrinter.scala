package trn.render

class MiscPrinter {

  // From working on StairPrinter:
  // // This code makes the north wall a curved dome
  // val nw = new PointXY(0, 0)
  // val ne = new PointXY(2048, 0)
  // val sw = new PointXY(0, 1024)
  // val se = new PointXY(2048, 1024)
  // // i want the curve to be a half circle if my "control arms" are facing straight out
  // // multiplying the "arm" length by 2/3 seems to do the trick
  // val handleWidth = 2048 * 2 / 3
  // val handle = new PointXY(0, -handleWidth)
  // val topRow = Interpolate.cubic(nw, nw.add(handle), ne.add(handle), ne, 8)
  // val loop = topRow.map(p => w(p)) ++ Seq(w(se), w(sw))
  // val sectorId = map.createSectorFromLoop(loop: _*)

}
