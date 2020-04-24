package trn.prefab

trait EntropyProvider {
  def random: RandomX
  //
  // Random
  //
  def randomElement[E](collection: Iterable[E]): E = random.randomElement(collection)

  def randomShuffle[E](collection: Iterable[E]): TraversableOnce[E] = random.shuffle(collection)

  def randomElementOpt[E](collection: Iterable[E]): Option[E] = {
    if(collection.isEmpty){
      None
    }else{
      Some(random.randomElement(collection))
    }
  }

}
