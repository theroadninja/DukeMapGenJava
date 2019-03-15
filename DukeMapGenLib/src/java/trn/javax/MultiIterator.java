package trn.javax;

import java.util.*;

public class MultiIterator<T> implements Iterator<T> {

    //private final List<Iterable<T>> myIterables;

    private final Iterator<Iterable<T>> collectionIterator;

    private Iterator<T> itemIterator = null;

    public MultiIterator(Iterable<T> ... list){
        List<Iterable<T>> myIterables = Arrays.asList(list);
        collectionIterator = myIterables.iterator();
    }


    @Override
    public boolean hasNext() {
        if(itemIterator != null && itemIterator.hasNext()){
            return true;
        } else {
            if(collectionIterator.hasNext()){
                itemIterator = collectionIterator.next().iterator();
                return itemIterator.hasNext();
            }else{
                return false;
            }
        }
    }

    @Override
    public T next() {
        if(itemIterator != null && itemIterator.hasNext()){
            return itemIterator.next();
        } else {
            if(collectionIterator.hasNext()){
                itemIterator = collectionIterator.next().iterator();
                return itemIterator.next();
            } else {
                throw new NoSuchElementException();
            }
        }
    }
}
