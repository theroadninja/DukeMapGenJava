package trn.prefab

import java.util.Random
import scala.util.{Random => ScalaRandom}

class RandomX(seed: Long = System.currentTimeMillis()) {

  val random = new Random(seed)
  val scalaRandom = new ScalaRandom(seed)

  def randomElement[E](collection: Iterable[E]): E = {
    val list = collection.toSeq
    if(list.size < 1) throw new IllegalArgumentException("collection cannot be empty")
    list(random.nextInt(list.size))
  }

  def shuffle[E](collection: TraversableOnce[E]): TraversableOnce[E] = scalaRandom.shuffle(collection)

  def nextInt(bound: Int): Int = random.nextInt(bound)

}
