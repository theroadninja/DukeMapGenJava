package trn.javax;

import java.util.Iterator;

public class MultiIterable<T> implements Iterable<T> {

    final Iterable<T>[] array;
    public MultiIterable(Iterable<T> ... list){
        this.array = list;
    }

    @Override
    public Iterator<T> iterator() {
        return new MultiIterator<>(array);
    }
}
