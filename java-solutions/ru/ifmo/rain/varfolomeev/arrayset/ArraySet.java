package ru.ifmo.rain.varfolomeev.arrayset;

import java.util.*;

public final class ArraySet<E> extends AbstractSet<E> implements NavigableSet<E> {

    private final Comparator<? super E> comparator;

    private final List<E> elements;

    private NavigableSet<E> descendingSet = null;

    public ArraySet(Collection<? extends E> collection, Comparator<? super E> comparator) {
        this.comparator = comparator;
        if (isSorted(collection)) {
            elements = List.copyOf(collection);
        } else {
            Set<E> treeSet = new TreeSet<>(comparator);
            treeSet.addAll(collection);
            elements = List.copyOf(treeSet);
        }
    }

    private boolean isSorted(Collection<? extends E> collection) {
        if (collection.isEmpty() || collection.size() == 1) {
            return true;
        }
        Iterator<? extends E> iter = collection.iterator();
        E current, previous = iter.next();
        while (iter.hasNext()) {
            current = iter.next();
            if (notNullComparator().compare(previous, current) >= 0) {
                return false;
            }
            previous = current;
        }
        return true;
    }

    public ArraySet(Collection<? extends E> collection) {
        this(collection, null);
    }

    public ArraySet() {
        this(List.of());
    }

    private ArraySet(List<E> elements, Comparator<? super E> comparator) {
        this(elements, comparator, null);
    }

    private ArraySet(List<E> elements, Comparator<? super E> comparator, NavigableSet<E> descendingSet) {
        this.elements = elements;
        this.comparator = comparator;
        this.descendingSet = descendingSet;
    }

    private int binarySearch(E e) {
        return Collections.binarySearch(elements, Objects.requireNonNull(e), comparator);
    }

    private int indexCounter(E e, int found, int notFound) {
        int x = binarySearch(e);
        int index = x >= 0 ? x + found : notFound - (x + 1);
        return index >= 0 && index < size() ? index : -1;
    }

    private int lowerIndex(E e) {
        return indexCounter(e, -1, -1);
    }

    private int floorIndex(E e) {
        return indexCounter(e, 0, -1);
    }

    private int ceilingIndex(E e) {
        return indexCounter(e, 0, 0);
    }

    private int higherIndex(E e) {
        return indexCounter(e, 1, 0);
    }

    private E bound(int index) {
        return index < 0 ? null : elements.get(index);
    }

    @Override
    public E lower(E e) {
        return bound(lowerIndex(e));
    }

    @Override
    public E floor(E e) {
        return bound(floorIndex(e));
    }

    @Override
    public E ceiling(E e) {
        return bound(ceilingIndex(e));
    }

    @Override
    public E higher(E e) {
        return bound(higherIndex(e));
    }

    @Override
    public E pollFirst() {
        throw new UnsupportedOperationException("pollFirst");
    }

    @Override
    public E pollLast() {
        throw new UnsupportedOperationException("pollLast");
    }

    @Override
    public Iterator<E> iterator() {
        return elements.iterator();
    }

    @Override
    public NavigableSet<E> descendingSet() {
        if (descendingSet == null) {
            descendingSet = new ArraySet<>(
                    new ReversedList<>(elements),
                    Collections.reverseOrder(comparator),
                    this
            );
        }
        return descendingSet;
    }

    @Override
    public Iterator<E> descendingIterator() {
        return descendingSet().iterator();
    }

    @Override
    public NavigableSet<E> subSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (notNullComparator().compare(fromElement, toElement) > 0) {
            throw new IllegalArgumentException();
        }
        return uncheckedSubSet(fromElement, fromInclusive, toElement, toInclusive);
    }

    private NavigableSet<E> uncheckedSubSet(E fromElement, boolean fromInclusive, E toElement, boolean toInclusive) {
        if (isEmpty()) {
            return new ArraySet<>(List.of(), comparator);
        }
        fromElement = fromElement == null ? first() : fromElement;
        toElement = toElement == null ? last() : toElement;
        int fromIndex = fromInclusive ? ceilingIndex(fromElement) : higherIndex(fromElement);
        int toIndex = toInclusive ? floorIndex(toElement) : lowerIndex(toElement);
        return new ArraySet<>(
                fromIndex == -1 || toIndex == -1 || fromIndex > toIndex ?
                        List.of() : elements.subList(fromIndex, toIndex + 1),
                comparator
        );
    }

    @Override
    public NavigableSet<E> headSet(E toElement, boolean inclusive) {
        return uncheckedSubSet(null, true, toElement, inclusive);
    }

    @Override
    public NavigableSet<E> tailSet(E fromElement, boolean inclusive) {
        return uncheckedSubSet(fromElement, inclusive, null, true);
    }

    @Override
    public SortedSet<E> subSet(E fromElement, E toElement) {
        return subSet(fromElement, true, toElement, false);
    }

    @Override
    public SortedSet<E> headSet(E toElement) {
        return headSet(toElement, false);
    }

    @Override
    public SortedSet<E> tailSet(E fromElement) {
        return tailSet(fromElement, true);
    }

    @Override
    public E first() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return elements.get(0);
    }

    @Override
    public E last() {
        if (isEmpty()) {
            throw new NoSuchElementException();
        }
        return elements.get(size() - 1);
    }

    @Override
    @SuppressWarnings("unchecked")
    public boolean contains(Object o) {
        return binarySearch((E) o) >= 0;
    }

    @Override
    public Comparator<? super E> comparator() {
        return comparator;
    }

    @SuppressWarnings("unchecked")
    private Comparator<? super E> notNullComparator() {
        return comparator != null ? comparator : (Comparator<E>) Comparator.naturalOrder();
    }

    @Override
    public int size() {
        return elements.size();
    }
}
