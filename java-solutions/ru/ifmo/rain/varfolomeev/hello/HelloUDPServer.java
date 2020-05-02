package ru.ifmo.rain.varfolomeev.hello;

import info.kgeorgiy.java.advanced.hello.HelloServer;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

public class HelloUDPServer implements HelloServer {
    private DatagramSocket datagramSocket = null;
    private ExecutorService executorService = null;
    private ExecutorService distributionService = null;
    private boolean started = false;

    @Override
    public synchronized void start(int port, int threadCount) {
        if (started) {
            close();
        }
        try {
            datagramSocket = new DatagramSocket(port);
            distributionService = Executors.newSingleThreadExecutor();
            executorService = new ThreadPoolExecutor(threadCount, threadCount, 500, TimeUnit.MILLISECONDS,
                    new LinkedBlockingQueue<>(1000), new ThreadPoolExecutor.DiscardPolicy());
            distributionService.submit(this::runServer);
            started = true;
        } catch (SocketException ignored) {
            // Do nothing
        }
    }

    private void runServer() {
        while (!datagramSocket.isClosed()) {
            try {
                DatagramPacket request = new DatagramPacket(
                        new byte[datagramSocket.getReceiveBufferSize()],
                        datagramSocket.getReceiveBufferSize());
                datagramSocket.receive(request);
                executorService.submit(() -> respond(request));
            } catch (IOException e) {
                if (started) {
                    System.err.println("Failed to receive packet: " + e.getMessage());
                }
            }
        }
    }

    private void respond(DatagramPacket request) {
        String requestMessage = new String(request.getData(), request.getOffset(),
                request.getLength(), StandardCharsets.UTF_8);
        DatagramPacket response = new DatagramPacket(new byte[0], 0, request.getSocketAddress());
        response.setData(("Hello, " + requestMessage).getBytes(StandardCharsets.UTF_8));
        try {
            datagramSocket.send(response);
        } catch (IOException e) {
            if (started) {
                System.err.println("Failed to send packet: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        distributionService.shutdownNow();
        executorService.shutdownNow();
        datagramSocket.close();
        started = false;
    }
}
