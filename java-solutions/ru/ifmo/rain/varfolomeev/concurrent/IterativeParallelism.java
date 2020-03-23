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

    /**
     * Returns maximum value.
     *
     * @param threadCount number or concurrent threads.
     * @param values      values to get maximum of.
     * @param comparator  value comparator.
     * @param <T>         value type.
     * @return maximum of given values
     * @throws InterruptedException   if executing thread was interrupted.
     * @throws NoSuchElementException if not values are given.
     */
    @Override
    public <T> T maximum(int threadCount, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        if (values.isEmpty()) {
            throw new NoSuchElementException("'values' is empty list");
        }
        Function<Stream<? extends T>, T> function = stream -> stream.max(comparator).orElse(null);
        return parallelFunction(threadCount, values, function, function);
    }

    /**
     * Returns minimum value.
     *
     * @param threadCount number or concurrent threads.
     * @param values      values to get minimum of.
     * @param comparator  value comparator.
     * @param <T>         value type.
     * @return minimum of given values
     * @throws InterruptedException   if executing thread was interrupted.
     * @throws NoSuchElementException if not values are given.
     */
    @Override
    public <T> T minimum(int threadCount, List<? extends T> values, Comparator<? super T> comparator) throws InterruptedException {
        return maximum(threadCount, values, comparator.reversed());
    }

    /**
     * Returns whether all values satisfies predicate.
     *
     * @param threadCount number or concurrent threads.
     * @param values      values to test.
     * @param predicate   test predicate.
     * @param <T>         value type.
     * @return whether all values satisfies predicate or {@code true}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean all(int threadCount, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return parallelFunction(
                threadCount, values,
                stream -> stream.allMatch(predicate),
                stream -> stream.allMatch(Boolean::booleanValue)
        );
    }

    /**
     * Returns whether any of values satisfies predicate.
     *
     * @param threadCount number or concurrent threads.
     * @param values      values to test.
     * @param predicate   test predicate.
     * @param <T>         value type.
     * @return whether any value satisfies predicate or {@code false}, if no values are given.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> boolean any(int threadCount, List<? extends T> values, Predicate<? super T> predicate) throws InterruptedException {
        return !all(threadCount, values, predicate.negate());
    }

    /**
     * Join values to string.
     *
     * @param threadCount number of concurrent threads.
     * @param values      values to join.
     * @return list of joined result of {@link #toString()} call on each value.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public String join(int threadCount, List<?> values) throws InterruptedException {
        return parallelFunction(
                threadCount, values,
                stream -> stream.map(Object::toString).collect(Collectors.joining()),
                stream -> stream.collect(Collectors.joining())
        );
    }

    /**
     * Filters values by predicate.
     *
     * @param threadCount number of concurrent threads.
     * @param values      values to filter.
     * @param predicate   filter predicate.
     * @param <T>         value type.
     * @return list of values satisfying given predicated. Order of values is preserved.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T> List<T> filter(final int threadCount, final List<? extends T> values, final Predicate<? super T> predicate) throws InterruptedException {
        return parallelFunction(
                threadCount, values,
                stream -> stream.filter(predicate).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList())
        );
    }

    /**
     * Maps values.
     *
     * @param threadCount number of concurrent threads.
     * @param values      values to filter.
     * @param f           mapper function.
     * @param <T>         value type.
     * @param <U>         result type.
     * @return list of values mapped by given function.
     * @throws InterruptedException if executing thread was interrupted.
     */
    @Override
    public <T, U> List<U> map(final int threadCount, final List<? extends T> values, final Function<? super T, ? extends U> f) throws InterruptedException {
        return parallelFunction(
                threadCount, values,
                stream -> stream.map(f).collect(Collectors.toList()),
                stream -> stream.flatMap(Collection::stream).collect(Collectors.toList())
        );
    }


    /**
     * Reduces values using monoid.
     *
     * @param threadCount number of concurrent threads.
     * @param values      values to reduce.
     * @param monoid      monoid to use.
     * @param <T>         value type.
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     * @throws InterruptedException if executing thread was interrupted.
     */
    public <T> T reduce(final int threadCount, List<T> values, final Monoid<T> monoid) throws InterruptedException {
        Function<Stream<T>, T> function = stream -> stream.reduce(monoid.getIdentity(), monoid.getOperator());
        return parallelFunction(threadCount, values, function, function);
    }

    /**
     * Maps and reduces values using monoid.
     *
     * @param threadCount number of concurrent threads.
     * @param values      values to reduce.
     * @param lift        mapping function.
     * @param monoid      monoid to use.
     * @param <T>         value type.
     * @param <R>         result type.
     * @return values reduced by provided monoid or {@link Monoid#getIdentity() identity} if not values specified.
     * @throws InterruptedException if executing thread was interrupted.
     */
    public <T, R> R mapReduce(final int threadCount, final List<T> values, final Function<T, R> lift, final Monoid<R> monoid) throws InterruptedException {
        return reduce(threadCount, map(threadCount, values, lift), monoid);
    }

    private <R, S, T> S parallelFunction(int threadCount,
                                         final List<T> values, Function<? super Stream<T>, R> mapper,
                                         Function<? super Stream<R>, S> finisher) throws InterruptedException {
//        long t = System.nanoTime();
//        if (parallelMapper!= null && values.size() > 1) {
//            final int ops = 20 - ((ParallelMapperImpl)parallelMapper).getThreadCount();
//            List<T> list = List.of(values.get(0), values.get(1));
//            for (int i = 0; i < ops; i++) {
//                R el = mapper.apply(list.stream());
//            }
//        }
        checkThreads(threadCount);
        threadCount = Math.min(threadCount, values.size());
        List<Stream<T>> streams = getSubStreams(threadCount, values);
        List<R> results = parallelMapper == null ?
                evaluateResults(threadCount, streams, mapper) : parallelMapper.map(mapper, streams);
//        try {
        return finisher.apply(results.stream());
//        } finally {
//
//            System.out.println((System.nanoTime() - t)/1e6);
//        }
    }

    private <T, R> List<R> evaluateResults(final int threadCount, List<Stream<T>> streams,
                                           Function<? super Stream<T>, R> mapper) throws InterruptedException {
        List<R> results = new ArrayList<>(Collections.nCopies(threadCount, null));
        List<Thread> threads = getThreads(threadCount, streams, results, mapper);
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            thread.join();
        }
        return results;
    }

    private void checkThreads(int threadCount) throws InterruptedException {
        if (threadCount < 1) {
            throw new InterruptedException("Should be at least 1 thread");
        }
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

    private <T, R> List<Thread> getThreads(int threadCount, final List<Stream<T>> streams, List<R> result, Function<? super Stream<T>, R> mapper) {
        List<Thread> threads = new ArrayList<>();
        for (int i = 0; i < threadCount; i++) {
            final int index = i;
            threads.add(new Thread(() -> result.set(index, mapper.apply(streams.get(index)))));
        }
        return threads;
    }
}
