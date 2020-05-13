package ru.ifmo.rain.varfolomeev.hello;

import info.kgeorgiy.java.advanced.hello.HelloClient;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Objects;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.ifmo.rain.varfolomeev.hello.HelloUDPUtil.getIntArgument;

public class HelloUDPClient implements HelloClient {
    @Override
    public void run(String hostname, int port, String prefix, int threadCount, int requestCount) {
        if (threadCount < 1) {
            throw new IllegalArgumentException("Thread count must be positive");
        }

        if (requestCount < 0) {
            throw new IllegalArgumentException("Request count can't be negative");
        }

        if (prefix == null) {
            throw new IllegalArgumentException("Request prefix can't be 'null'");

        }

        SocketAddress socketAddress;
        try {
            socketAddress = new InetSocketAddress(InetAddress.getByName(hostname), port);
        } catch (UnknownHostException e) {
            throw new IllegalArgumentException(e);
        }

        ExecutorService executorService = Executors.newFixedThreadPool(threadCount);
        CountDownLatch latch = new CountDownLatch(threadCount);
        for (int thread = 0; thread < threadCount; thread++) {
            final int threadNumber = thread;
            executorService.submit(() -> sendAndReceive(socketAddress, prefix, threadNumber, requestCount, latch));
        }

        executorService.shutdown();
        try {
            latch.await();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void sendAndReceive(SocketAddress socketAddress, String prefix,
                                int threadNumber, int requestCount, CountDownLatch latch) {
        try (DatagramSocket datagramSocket = new DatagramSocket()) {
            datagramSocket.setSoTimeout(500);
            int receiveBufferSize = datagramSocket.getReceiveBufferSize();
            DatagramPacket request = new DatagramPacket(new byte[0], 0, socketAddress);
            DatagramPacket response = new DatagramPacket(new byte[receiveBufferSize], receiveBufferSize);
            for (int requestNumber = 0; requestNumber < requestCount; requestNumber++) {
                String requestMessage = prefix + threadNumber + '_' + requestNumber;
                request.setData(requestMessage.getBytes(StandardCharsets.UTF_8));
                while (!datagramSocket.isClosed()) {
                    try {
                        datagramSocket.send(request);
                        System.out.println("Sending: " + requestMessage);
                        datagramSocket.receive(response);
                        String responseMessage = new String(response.getData(),
                                response.getOffset(), response.getLength(), StandardCharsets.UTF_8);
                        if (responseMessage.contains(requestMessage)) {
                            System.out.println("Receiving: " + responseMessage);
                            break;
                        } else {
                            System.out.println("Fail on response validation: " + responseMessage);
                        }
                    } catch (IOException e) {
                        System.out.println("Failed: " + requestMessage);
                    }
                }
            }
        } catch (SocketException ignored) {
            //Do nothing
        } finally {
            latch.countDown();
        }
    }

    /**
     * Runs HelloUDPClient with given arguments
     * @param args {@link #run(String, int, String, int, int)} arguments
     */
    public static void main(String[] args) {
        if (args == null || Arrays.stream(args).anyMatch(Objects::isNull)) {
            System.err.println("Arguments can't be null");
        } else if (args.length != 5) {
            System.err.println("Usage: HelloUDPClient hostname port prefix threadCount requestCount");
        } else {
            new HelloUDPClient().run(args[0], getIntArgument("port", args[1]), args[2],
                    getIntArgument("threadCount", args[3]), getIntArgument("requestCount", args[4]));
        }
    }
}
