package trn.prefab

/** an object that can provide unique hitags */
trait TagGenerator {
  def nextUniqueHiTag(): Int
}

class SimpleTagGenerator(val start: Int) extends TagGenerator {
  if(start < 1) throw new IllegalArgumentException

  var hiTagCounter = start

  def nextUniqueHiTag(): Int = {
    val i = hiTagCounter
    hiTagCounter += 1
    i
  }
}

