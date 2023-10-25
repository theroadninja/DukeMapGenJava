package trn

import java.util.Random
import scala.collection.mutable
import scala.util.{Random => ScalaRandom}

class RandomX(seed: Long = System.currentTimeMillis()) {

  val random = new Random(seed)
  val scalaRandom = new ScalaRandom(seed)

  def randomElement[E](collection: Iterable[E]): E = {
    val list = collection.toSeq
    if(list.size < 1) throw new IllegalArgumentException("collection cannot be empty")
    list(random.nextInt(list.size))
  }

  def randomElementOpt[E](collection: Iterable[E]): Option[E] = if(collection.isEmpty){
    None
  }else{
    Some(randomElement(collection))
  }

  def shuffle[E](collection: TraversableOnce[E]): TraversableOnce[E] = scalaRandom.shuffle(collection)

  def nextInt(bound: Int): Int = random.nextInt(bound)

  def nextBool(): Boolean = random.nextBoolean()

  def nextInt(start: Int, end: Int): Int = start + random.nextInt(end - start)

  /** randomly executes 1 of 2 blocks of code and returns the result. */
  def flipCoin[T](head: => T)(tails: => T): T = nextInt(100) match {
    // using nextInt(100) because nextInt(2) behaved a little weirdly in unit tests
    case i if i < 50 => head
    case _ => tails
  }

  /**
    * Randomly pairs all of the elements of the list, but does not allow the same element to be paired with itself.
    * @param collection
    * @tparam E
    * @return (list of pairs, leftovers)
    */
  def randomUnequalPairs[E](collection: TraversableOnce[E]): (Seq[(E, E)], Seq[E]) = {
    val shuffled = shuffle(collection)

    val stack = mutable.ArrayBuffer[E]()
    val results = mutable.ArrayBuffer[(E,E)]()
    shuffled.foreach{ x =>
      if(stack.isEmpty || stack.last == x){
        stack.append(x)
      }else{
        val a = stack.last
        stack.trimEnd(1) // pop
        results.append((a, x))
      }
    }
    (results, stack)
  }

}

object RandomX {
  def apply(seed: Long = System.currentTimeMillis()): RandomX = new RandomX(seed)
}
