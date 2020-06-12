package ru.ifmo.rain.varfolomeev.hello;

import java.io.IOException;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import static ru.ifmo.rain.varfolomeev.hello.HelloUDPUtil.getStringFromPacket;

public class HelloUDPClient extends AbstractHelloClient {
    @Override
    public void run(String hostname, int port, String prefix, int threadCount, int requestCount) {
        validateParameters(prefix, threadCount, requestCount);

        SocketAddress socketAddress = getSocketAddress(hostname, port);

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
                        String responseMessage = getStringFromPacket(response);
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
     * Runs HelloClient realisation with given arguments
     *
     * @param args {@link #run(String, int, String, int, int)} arguments
     */
    public static void main(String[] args) {
        main(new HelloUDPClient(), args);
    }
}
