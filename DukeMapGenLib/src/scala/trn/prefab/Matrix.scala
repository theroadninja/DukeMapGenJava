package trn.prefab


object IntMatrix {

  def apply(rows: Seq[Seq[Int]]): IntMatrix = {
    new IntMatrix(rows)
  }

  def multiply(row: Seq[Int], col: Seq[Int]): Int = {
    if(row.size != col.size) throw new IllegalArgumentException
    row.zip(col).map(rc => rc._1 * rc._2).sum
  }

  /** covenience method to make a 1 column matrix from a vector */
  def toColumn(vector: Seq[Int]): IntMatrix = {
    IntMatrix(vector.map(Seq(_)))
  }
}

class IntMatrix(val rows: Seq[Seq[Int]]) {


  def rowCount: Int = rows.size
  def colCount: Int = if(rows.size > 0){ rows(0).size }else{ 0 }
  def row(i: Int): Seq[Int] = rows(i)
  def apply(rowIndex: Int): Seq[Int] = row(rowIndex)
  def col(i: Int): Seq[Int] = rows.map(row => row(i))


  override def equals(other: Any): Boolean = {
    other match {
      case rh: IntMatrix => rows == rh.rows
      case _ => false
    }
  }

  override def hashCode: Int = {
    rows.hashCode()
  }

  override def toString: String = {
    val s = rows.map{ row =>
      val contents = row.map(_.toString).mkString(",")
      //s"\t[${row.map(_.toString).mkString(",")}]\n"
      s"\t[${contents}]"
    }.mkString("\n")
    s"\n{${s}}\n"
  }

  def cols: Seq[Seq[Int]] = {
    (0 until colCount).map(col => rows.map(row => row(col)))
  }

  def *(m2: IntMatrix): IntMatrix = { // TODO - add a method that takes a Seq[Int] (and a PointXY ... )
    if(this.colCount != m2.rowCount) throw new IllegalArgumentException("matrices are wrong sizes")

    IntMatrix(rows.map{ m1row =>
      m2.cols.map { m2col =>
        IntMatrix.multiply(m1row, m2col)
      }
    })
  }


}
