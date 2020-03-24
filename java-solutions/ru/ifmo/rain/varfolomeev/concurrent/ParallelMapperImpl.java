package ru.ifmo.rain.varfolomeev.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParallelMapperImpl implements ParallelMapper {
    private static final int QUEUE_MAX_SIZE = 1_000_000;

    private final Queue<Runnable> tasks;
    private final List<Thread> threads;

    private static class ResultCollector<R> {
        private final List<R> result;
        private AtomicInteger counter;

        ResultCollector(int size) {
            result = new ArrayList<>(Collections.nCopies(size, null));
            counter = new AtomicInteger();
        }

        synchronized void set(int index, R element) {
            result.set(index, element);
            if (counter.incrementAndGet() == result.size()) {
                notify();
            }
        }

        synchronized List<R> getResult() throws InterruptedException {
            while (counter.get() < result.size()) {
                wait();
            }
            return result;
        }
    }

    /**
     * Creates work threads, which can be used for parallelism in {@link #map(Function, List)}.
     *
     * @param threadCount threads count to create.
     */
    public ParallelMapperImpl(int threadCount) {
        this.tasks = new LinkedList<>();
        this.threads = Stream.generate(() -> new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    consume();
                }
            } catch (InterruptedException ignore) {
            } finally {
                Thread.currentThread().interrupt();
            }
        })).limit(threadCount).collect(Collectors.toList());
        for (Thread thread : threads) {
            thread.start();
        }
    }

    void consume() throws InterruptedException {
        Runnable task;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            task = tasks.poll();
            tasks.notifyAll();
        }
        task.run();
    }

    void produce(Runnable task) throws InterruptedException {
        synchronized (tasks) {
            while (tasks.size() == QUEUE_MAX_SIZE) {
                tasks.wait();
            }
            tasks.add(task);
            tasks.notifyAll();
        }
    }

    /**
     * Maps function {@code f} over specified {@code args}.
     * Mapping for each element performs in parallel.
     *
     * @param f    mapper function.
     * @param args values to map.
     * @param <T>  value type.
     * @param <R>  result type.
     * @throws InterruptedException if calling thread was interrupted
     */
    public <T, R> List<R> map(Function<? super T, ? extends R> f, List<? extends T> args) throws InterruptedException {
        final ResultCollector<R> resultCollector = new ResultCollector<>(args.size());
        for (int i = 0; i < args.size(); i++) {
            final int index = i;
            produce(() -> resultCollector.set(index, f.apply(args.get(index))));
        }
        return resultCollector.getResult();
    }

    /**
     * Stops all threads. All unfinished mappings leave in undefined state.
     */
    @Override
    public void close() {
        threads.forEach(Thread::interrupt);
        threads.forEach(thread -> {
            try {
                thread.join();
            } catch (InterruptedException ignored) {
            }
        });
    }
}
