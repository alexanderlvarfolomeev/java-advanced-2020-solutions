package ru.ifmo.rain.varfolomeev.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.*;

import static ru.ifmo.rain.varfolomeev.hello.HelloUDPUtil.getIntArgument;

public abstract class AbstractHelloServer implements HelloServer {
    static final String PREFIX = "Hello, ";

    private ExecutorService distributionExecutor = null;
    private boolean started = false;

    abstract void startImplementation(int port, int threadCount);

    abstract void runServer();

    @Override
    public synchronized void start(int port, int threadCount) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("Thread count must be positive");
        }

        if (started) {
            throw new IllegalStateException("The server is already started");
        }

        startImplementation(port, threadCount);

        distributionExecutor = Executors.newSingleThreadExecutor();
        distributionExecutor.submit(this::runServer);
        started = true;
    }

    boolean isStarted() {
        return started;
    }

    @Override
    public void close() {
        distributionExecutor.shutdownNow();
        started = false;
    }

    static void main(HelloServer server, String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments can't be null");
        } else if (args.length != 2) {
            System.err.println("Usage: Classname port threadCount");
        } else {
            server.start(getIntArgument("port", args[0]),
                    getIntArgument("threadCount", args[1]));
        }
    }
}
