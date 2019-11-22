package trn

import scala.collection.mutable

object FuncUtils {

  def defaultHistoFn[U, V](item: U): V = item.asInstanceOf[V]

  // TODO - as fun as this was to write, apparently groupBy can do the same thing (i thought it only took predicates)
  def histogram[K,T](items: Traversable[T], fn: T => K = defaultHistoFn[T, K] _): scala.collection.Map[K, Seq[T]] = {
    val results: mutable.Map[K, Seq[T]] = mutable.Map[K, Seq[T]]()

    items.foreach { item =>
      val key: K = fn(item)
      results(key) = results.getOrElse(key, Seq()) ++ Seq(item)
    }
    results.toMap
  }


  /**
    * Returns one of each items that appears in the collection more than once.
    *
    * @param items
    * @tparam A
    * @return
    */
  def duplicates[A](items: Traversable[A]): Iterable[A] = items.groupBy(identity).map(_._2).filter(_.size > 1).map(_.head)

}

object FuncImplicits {

  implicit class TravImproved[A](val s: Traversable[A]){
    def histogram[K](fn: A => K = FuncUtils.defaultHistoFn[A, K] _): scala.collection.Map[K, Seq[A]] = FuncUtils.histogram(s, fn)

    def duplicates: Iterable[A] = FuncUtils.duplicates(s)
  }

}
