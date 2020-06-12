package ru.ifmo.rain.varfolomeev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.Objects;

import static ru.ifmo.rain.varfolomeev.hello.HelloUDPUtil.getIntArgument;

public abstract class AbstractHelloClient implements HelloClient {
    void validateParameters(String prefix, int threadCount, int requestCount) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("Thread count must be positive");
        }

        if (requestCount < 0) {
            throw new IllegalArgumentException("Request count can't be negative");
        }

        if (prefix == null) {
            throw new IllegalArgumentException("Request prefix can't be 'null'");
        }
    }

    SocketAddress getSocketAddress(String hostname, int port) {
        try {
            return new InetSocketAddress(InetAddress.getByName(hostname), port);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }
    }

    static void main(HelloClient client, String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments can't be null");
        } else if (args.length != 5) {
            System.err.println("Usage: Classname hostname port prefix threadCount requestCount");
        } else {
            client.run(args[0], getIntArgument("port", args[1]), args[2],
                    getIntArgument("threadCount", args[3]), getIntArgument("requestCount", args[4]));
        }
    }
}
