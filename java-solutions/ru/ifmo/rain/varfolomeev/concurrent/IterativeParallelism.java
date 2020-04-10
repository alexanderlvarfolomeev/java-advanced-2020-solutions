package ru.ifmo.rain.varfolomeev.concurrent;

import info.kgeorgiy.java.advanced.concurrent.AdvancedIP;
import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class IterativeParallelism implements AdvancedIP {
    private final ParallelMapper parallelMapper;

    /**
     * Creates instance with specified {@link ParallelMapper}
     *
     * @param mapper thread pool.
     */
    public IterativeParallelism(ParallelMapper mapper) {
        this.parallelMapper = mapper;
    }

    /**
     * Create instance which creates work threads by itself.
     */
    public IterativeParallelism() {
        this.parallelMapper = null;
    }

    @Override
    public <T> T maximum(int threadCount, List<? extends T> values,
                         Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("'values' is empty list");
        }
        Function<Stream<? extends T>, T> function = stream -> stream.max(comparator).orElse(null);
        return parallelMap(threadCount, values, function, function);
    }

    @Override
    public <T> T minimum(int threadCount, List<? extends T> values,
                         Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threadCount, values, comparator.reversed());
    }

    @Override
    public <T> boolean all(int threadCount, List<? extends T> values,
                           Predicate<? super T> predicate) throws InterruptedException {
        return parallelMap(
                threadCount, values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue)
        );
    }

    @Override
    public <T> boolean any(int threadCount, List<? extends T> values,
                           Predicate<? super T> predicate) throws InterruptedException {
        return !all(threadCount, values, predicate.negate());
    }

    @Override
    public String join(int threadCount, List<?> values) throws InterruptedException {
        return parallelMap(
                threadCount, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining())
        );
    }

    @Override
    public <T> List<T> filter(final int threadCount, final List<? extends T> values,
                              final Predicate<? super T> predicate) throws InterruptedException {
        return parallelMap(
                threadCount, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList())
        );
    }

    @Override
    public <T, U> List<U> map(final int threadCount, final List<? extends T> values,
                              final Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelMap(
                threadCount, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList())
        );
    }

    @Override
    public <T> T reduce(final int threadCount, List<T> values, final Monoid<T> monoid) throws InterruptedException {
        Function<Stream<T>, T> function = stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator());
        return parallelMap(threadCount, values, function, function);
    }

    @Override
    public <T, R> R mapReduce(final int threadCount, final List<T> values,
                              final Function<T, R> lift, final Monoid<R> monoid) throws InterruptedException {
        return reduce(threadCount, map(threadCount, values, lift), monoid);
    }

    private <R, S, T> S parallelMap(int threadCount,
                                    final List<T> values, Function<? super Stream<T>, R> mapper,
                                    Function<? super Stream<R>, S> reducer) throws InterruptedException {
        if (threadCount < 1) {
            throw new InterruptedException("Should be at least 1 thread");
        }
        threadCount = Math.min(threadCount, values.size());
        List<Stream<T>> streams = getSubStreams(threadCount, values);
        List<R> results = parallelMapper == null ?
                evaluateResults(threadCount, streams, mapper) : parallelMapper.map(mapper, streams);
        return reducer.apply(results.stream());
    }

    private <T, R> List<R> evaluateResults(final int threadCount, List<Stream<T>> streams,
                                           Function<? super Stream<T>, R> mapper) throws InterruptedException {
        List<R> results = new ArrayList<>(Collections.nCopies(threadCount, null));
        List<Thread> threads = getThreads(threadCount, streams, results, mapper);
        for (Thread thread : threads) {
            thread.start();
        }
        List<InterruptedException> exceptions = new ArrayList<>();
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                exceptions.add(e);
            }
        }
        if (!exceptions.isEmpty()) {
            InterruptedException exception = exceptions.get(0);
            exceptions.stream().skip(1).forEach(exception::addSuppressed);
            throw exception;
        }
        return results;
    }

    private <T> List<Stream<T>> getSubStreams(int threadCount, List<T> values) {
        List<Stream<T>> streams = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            streams.add(values.subList(
                    getBound(i, threadCount, values.size()),
                    getBound(i + 1, threadCount, values.size())
            ).stream());
        }
        return streams;
    }

    private int getBound(long index, int threadCount, int size) {
        return (int) ((index * size) / threadCount);
    }

    private <T, R> List<Thread> getThreads(int threadCount, final List<Stream<T>> streams,
                                           List<R> result, Function<? super Stream<T>, R> mapper) {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads.add(new Thread(() -> result.set(index, mapper.apply(streams.get(index)))));
        }
        return threads;
    }
}
