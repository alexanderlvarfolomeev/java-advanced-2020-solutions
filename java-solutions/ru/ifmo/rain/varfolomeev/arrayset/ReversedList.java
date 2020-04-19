package ru.ifmo.rain.varfolomeev.arrayset;

import java.util.AbstractList;
import java.util.List;
import java.util.RandomAccess;

class ReversedList<E> extends AbstractList<E> implements RandomAccess {
    private final List<E> list;

    ReversedList(List<E> list) {
        this.list = list;
    }

    @Override
    public E get(int index) {
        return list.get(size() - 1 - index);
    }

    @Override
    public int size() {
        return list.size();
    }
}
