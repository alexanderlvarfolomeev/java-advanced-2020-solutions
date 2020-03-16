package ru.ifmo.rain.varfolomeev.concurrent;

import info.kgeorgiy.java.advanced.concurrent.ListIP;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements ListIP {
    @Override
    public <T> T maximum(int threadCount, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("'values' is empty list");
        }
        Function<Stream<? extends T>, T> function = stream -> stream.max(comparator).orElse(null);
        return parallelFunction(threadCount, values, function, function);
    }

    @Override
    public <T> T minimum(int threadCount, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threadCount, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threadCount, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelFunction(
                threadCount, values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue)
        );
    }

    @Override
    public <T> boolean any(int threadCount, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threadCount, values, predicate.negate());
    }

    @Override
    public String join(int threadCount, List<?> values) throws InterruptedException {
        return parallelFunction(
                threadCount, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining())
        );
    }

    @Override
    public <T> List<T> filter(final int threadCount, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return parallelFunction(
                threadCount, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList())
        );
    }

    @Override
    public <T, U> List<U> map(final int threadCount, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelFunction(
                threadCount, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList())
        );
    }

    private <R, S, T> S parallelFunction(final int threadCount,
                                         final List<T> values, Function<? super Stream<? extends T>, R> mapper,
                                         Function<? super Stream<R>, S> finisher) throws InterruptedException {
        checkThreads(threadCount);
        if (values.size() <= threadCount) {
            return finisher.apply(Stream.of(mapper.apply(values.stream())));
        }
        final List<R> results = new ArrayList<>(Collections.nCopies(threadCount, null));
        Thread[] threads = getThreads(threadCount, values, results, mapper);
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        return finisher.apply(results.stream());
    }

    private void checkThreads(int threadCount) throws InterruptedException {
        if (threadCount < 1) {
            throw new InterruptedException("Should be at least 1 thread");
        }
    }

    private <T, R> Thread[] getThreads(int threadCount, List<T> values, List<R> result, Function<? super Stream<? extends T>, R> mapper) {
        Thread[] threads = new Thread[threadCount];
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads[i] = new Thread(() -> result.set(
                    index,
                    mapper.apply(values.subList(
                            getBound(index, threadCount, values.size()),
                            getBound(index + 1, threadCount, values.size())
                    ).stream())));
        }
        return threads;
    }

    private int getBound(long index, int threadCount, int size) {
        return (int) ((index * size) / threadCount);
    }
}
