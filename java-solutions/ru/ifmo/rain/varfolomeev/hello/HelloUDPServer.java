package ru.ifmo.rain.varfolomeev.hello;

import java.io.IOException;
import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.SocketException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.*;

import static ru.ifmo.rain.varfolomeev.hello.HelloUDPUtil.getStringFromPacket;

public class HelloUDPServer extends AbstractHelloServer {
    private DatagramSocket datagramSocket = null;
    private ExecutorService executorService = null;

    @Override
    public synchronized void startImplementation(int port, int threadCount) {
        try {
            datagramSocket = new DatagramSocket(port);
        } catch (SocketException e) {
            throw new RuntimeException("Can't create DatagramSocket instance", e);
        }

        executorService = new ThreadPoolExecutor(threadCount, threadCount, 500, TimeUnit.MILLISECONDS,
                new LinkedBlockingQueue<>(1000), new ThreadPoolExecutor.DiscardPolicy());
    }


    void runServer() {
        while (!datagramSocket.isClosed()) {
            try {
                DatagramPacket request = new DatagramPacket(new byte[datagramSocket.getReceiveBufferSize()],
                        datagramSocket.getReceiveBufferSize());
                datagramSocket.receive(request);
                executorService.submit(() -> respond(request));
            } catch (IOException e) {
                if (isStarted()) {
                    System.out.println("Failed to receive packet: " + e.getMessage());
                }
            }
        }
    }

    private void respond(DatagramPacket request) {
        String requestMessage = getStringFromPacket(request);
        byte[] responseData = (PREFIX + requestMessage).getBytes(StandardCharsets.UTF_8);
        DatagramPacket response = new DatagramPacket(responseData, responseData.length, request.getSocketAddress());
        try {
            datagramSocket.send(response);
        } catch (IOException e) {
            if (isStarted()) {
                System.out.println("Failed to send packet: " + e.getMessage());
            }
        }
    }

    @Override
    public void close() {
        datagramSocket.close();
        executorService.shutdownNow();
        super.close();
    }

    /**
     * Starts HelloUDPServer with given arguments
     *
     * @param args {@link #start(int, int)} arguments
     */
    public static void main(String[] args) {
        main(new HelloUDPServer(), args);
    }
}
