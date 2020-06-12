package ru.ifmo.rain.varfolomeev.hello;

import java.io.IOException;
import java.net.InetSocketAddress;
import java.net.SocketAddress;
import java.nio.ByteBuffer;
import java.nio.channels.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

import static ru.ifmo.rain.varfolomeev.hello.HelloUDPUtil.BUFFER_SIZE;

public class HelloUDPNonblockingServer extends AbstractHelloServer {
    private Selector selector = null;
    private ByteBuffer buffer = null;

    @Override
    public synchronized void startImplementation(int port, int threadCount) {
        buffer = ByteBuffer.allocate(BUFFER_SIZE);
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException("Can't create new selector", e);
        }
        try {
            DatagramChannel.open().bind(new InetSocketAddress(port)).configureBlocking(false).register(selector, SelectionKey.OP_READ);
        } catch (IOException e) {
            throw new RuntimeException("Can't create acceptor channel", e);
        }
    }

    void runServer() {
        while (!Thread.interrupted()) {
            try {
                selector.select();
            } catch (IOException e) {
                System.out.println("Can't get keys of ready channels: " + e.getMessage());
            }
            for (final Iterator<SelectionKey> iterator = selector.selectedKeys().iterator(); iterator.hasNext(); ) {
                final SelectionKey key = iterator.next();
                try {
                    if (key.isReadable()) {
                        respond(key);
                    }
                } finally {
                    iterator.remove();
                }
            }
        }
    }

    private void respond(SelectionKey key) {
        DatagramChannel datagramChannel = (DatagramChannel) key.channel();
        buffer.put(PREFIX.getBytes(StandardCharsets.UTF_8));
        try {
            SocketAddress sender = datagramChannel.receive(buffer);
            buffer.flip();
            try {
                datagramChannel.send(buffer, sender);
            } catch (IOException e) {
                if (isStarted()) {
                    System.out.println("Failed to sent message: " + e.getMessage());
                }
            }
        } catch (IOException e) {
            if (isStarted()) {
                System.out.println("Failed to receive message: " + e.getMessage());
            }
        }
        buffer.clear();
    }

    @Override
    public void close() {
        super.close();
    }

    /**
     * Starts HelloUDPServer with given arguments
     *
     * @param args {@link #start(int, int)} arguments
     */
    public static void main(String[] args) {
        main(new HelloUDPNonblockingServer(), args);
    }
}
