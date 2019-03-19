package trn.prefab

import java.util.Random

class RandomX(seed: Long = System.currentTimeMillis()) {

  val random = new Random(seed)

  def randomElement[E](collection: Iterable[E]): E = {
    val list = collection.toSeq
    if(list.size < 1) throw new IllegalArgumentException("collection cannot be empty")
    list(random.nextInt(list.size))
  }

  def nextInt(bound: Int): Int = random.nextInt(bound)
}
