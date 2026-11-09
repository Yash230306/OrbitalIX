package com.orbitalix.util;

import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.stream.Stream;
import java.util.stream.StreamSupport;

/** Generic bounded history using a circular array; iteration is oldest to newest. */
public final class RingBuffer<T> implements Iterable<T> {
    private final Object[] values;
    private int start;
    private int size;

    public RingBuffer(int capacity) {
        if (capacity < 1) throw new IllegalArgumentException("capacity must be positive");
        values = new Object[capacity];
    }

    public void add(T value) {
        int index = (start + size) % values.length;
        values[index] = value;
        if (size == values.length) start = (start + 1) % values.length;
        else size++;
    }

    public int size() { return size; }
    public int capacity() { return values.length; }
    public void clear() {
        java.util.Arrays.fill(values, null);
        start = 0;
        size = 0;
    }
    public Stream<T> stream() { return StreamSupport.stream(spliterator(), false); }
    public T get(int index) {
        if (index < 0 || index >= size) throw new IndexOutOfBoundsException(index);
        @SuppressWarnings("unchecked") T value = (T) values[(start + index) % values.length];
        return value;
    }

    @Override public Iterator<T> iterator() {
        return new Iterator<>() {
            private int index;
            @Override public boolean hasNext() { return index < size; }
            @Override public T next() {
                if (!hasNext()) throw new NoSuchElementException();
                return get(index++);
            }
        };
    }
}
