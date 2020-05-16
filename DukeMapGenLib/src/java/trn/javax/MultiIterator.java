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
            // need to loop because collections could be empty
            while(collectionIterator.hasNext()){
                itemIterator = collectionIterator.next().iterator();
                if(itemIterator.hasNext()){
                    return true;
                }
            }
            return false;
        }
    }

    @Override
    public T next() {
        if(itemIterator != null && itemIterator.hasNext()){
            return itemIterator.next();
        } else {
            while(collectionIterator.hasNext()){
                itemIterator = collectionIterator.next().iterator();
                if(itemIterator.hasNext()){
                    return itemIterator.next();
                }
            }
            throw new NoSuchElementException();
        }
    }
}
