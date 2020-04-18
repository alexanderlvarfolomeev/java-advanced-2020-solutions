package ru.ifmo.rain.varfolomeev.concurrent;

import info.kgeorgiy.java.advanced.mapper.ParallelMapper;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class ParallelMapperImpl implements ParallelMapper {
    private final Queue<Runnable> tasks;
    private final List<Thread> threads;

    private static class ListWrapper<R> {
        private final List<R> result;
        private int counter;

        private ListWrapper(int size) {
            result = new ArrayList<>(Collections.nCopies(size, null));
            counter = 0;
        }

        private synchronized void set(int index, R element) {
            result.set(index, element);
            counter++;
            if (counter == result.size()) {
                notify();
            }
        }

        private synchronized List<R> getResult() throws InterruptedException {
            while (counter < result.size()) {
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
        if (threadCount < 1) {
            throw new IllegalArgumentException("Thread count must be positive");
        }
        this.tasks = new LinkedList<>();
        this.threads = Stream.generate(() -> new Thread(() -> {
            try {
                while (!Thread.interrupted()) {
                    consume();
                }
            } catch (InterruptedException ignore) {
            }
        })).limit(threadCount).collect(Collectors.toList());
        threads.forEach(Thread::start);
    }

    private void consume() throws InterruptedException {
        Runnable task;
        synchronized (tasks) {
            while (tasks.isEmpty()) {
                tasks.wait();
            }
            task = tasks.poll();
        }
        task.run();
    }

    private void produce(Runnable task) {
        synchronized (tasks) {
            tasks.add(task);
            tasks.notify();
        }
    }

    public <T, R> List<R> map(Function<? super T, ? extends R> mapper, List<? extends T> args) throws InterruptedException {
        final ListWrapper<R> listWrapper = new ListWrapper<>(args.size());
        RuntimeException[] exception = new RuntimeException[]{null};
        for (int i = 0; i < args.size(); i++) {
            final int index = i;
            produce(() -> {
                try {
                    R result = mapper.apply(args.get(index));
                    synchronized (listWrapper) {
                        listWrapper.set(index, result);
                    }
                } catch (RuntimeException e) {
                    synchronized (exception) {
                        if (exception[0] == null) {
                            exception[0] = e;
                        }
                    }
                }
            });
        }

        if (exception[0] == null) {
            return listWrapper.getResult();
        } else {
            throw exception[0];
        }
    }

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
