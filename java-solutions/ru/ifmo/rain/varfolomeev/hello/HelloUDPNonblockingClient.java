package ru.ifmo.rain.varfolomeev.hello;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.net.*;
import java.nio.ByteBuffer;
import java.nio.channels.DatagramChannel;
import java.nio.channels.SelectionKey;
import java.nio.channels.Selector;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.*;
import java.util.stream.IntStream;

import static ru.ifmo.rain.varfolomeev.hello.HelloUDPUtil.BUFFER_SIZE;

public class HelloUDPNonblockingClient extends AbstractHelloClient {
    @Override
    public void run(String hostname, int port, String prefix, int threadCount, int requestCount) {
        validateParameters(prefix, threadCount, requestCount);

        SocketAddress socketAddress = getSocketAddress(hostname, port);

        Selector selector;
        try {
            selector = Selector.open();
        } catch (IOException e) {
            throw new RuntimeException("Can't create new selector", e);
        }

        IntStream.range(0, threadCount)
                .forEach(((ThrowingIntConsumer) i -> DatagramChannel.open()
                        .bind(null)
                        .connect(socketAddress)
                        .configureBlocking(false)
                        .register(selector, SelectionKey.OP_WRITE, new AbstractMap.SimpleEntry<>(i, 0)))
                        .toIntConsumer());

        process(new ClientContext(selector, ByteBuffer.allocate(BUFFER_SIZE), threadCount, requestCount, prefix));
    }

    private void process(ClientContext context) {
        while (!context.isFinished()) {
            try {
                if (context.getSelector().select(200) == 0) {
                    for (SelectionKey key : context.getSelector().keys()) {
                        if (key.interestOps() == SelectionKey.OP_READ) {
                            key.interestOps(SelectionKey.OP_WRITE);
                        }
                    }
                }
            } catch (IOException e) {
                System.out.println("Can't get keys of ready channels: " + e.getMessage());
            }
            for (final Iterator<SelectionKey> iterator = context.getSelector().selectedKeys().iterator(); iterator.hasNext(); ) {
                final SelectionKey key = iterator.next();
                try {
                    if (key.isWritable()) {
                        send(key, context);
                    }
                    if (key.isReadable()) {
                        receive(key, context);
                    }
                } finally {
                    iterator.remove();
                }
            }
        }
        try {
            context.getSelector().close();
        } catch (IOException e) {
            System.out.println("Can't close selector");
        }
    }

    @SuppressWarnings("unchecked")
    private void send(SelectionKey key, ClientContext context) {
        DatagramChannel datagramChannel = (DatagramChannel) key.channel();
        Map.Entry<Integer, Integer> entry = (Map.Entry<Integer, Integer>) key.attachment();
        String requestMessage = context.getPrefix() + entry.getKey() + '_' + entry.getValue();

        context.getBuffer().put(requestMessage.getBytes(StandardCharsets.UTF_8)).flip();

        try {
            datagramChannel.send(context.getBuffer(), datagramChannel.getRemoteAddress());
            System.out.println("Sending: " + requestMessage);
            key.interestOps(SelectionKey.OP_READ);
        } catch (IOException e) {
            System.out.println("Channel can't get connected remote address: " + e.getMessage());
        }
        context.getBuffer().clear();
    }

    @SuppressWarnings("unchecked")
    private void receive(SelectionKey key, ClientContext context) {
        DatagramChannel datagramChannel = (DatagramChannel) key.channel();
        Map.Entry<Integer, Integer> entry = (Map.Entry<Integer, Integer>) key.attachment();
        String requestMessage = context.getPrefix() + entry.getKey() + '_' + entry.getValue();

        context.getBuffer().clear();
        try {
            datagramChannel.receive(context.getBuffer());
            context.getBuffer().flip();
            String responseMessage = HelloUDPUtil.getStringFromBuffer(context.getBuffer());
            if (responseMessage.contains(requestMessage)) {
                System.out.println("Receiving: " + responseMessage);
                int requestNumber = entry.getValue() + 1;
                if (requestNumber == context.getRequestCount()) {
                    context.countDown();
                    key.cancel();
                    datagramChannel.close();
                } else {
                    entry.setValue(requestNumber);
                    key.interestOps(SelectionKey.OP_WRITE);
                }
            } else {
                System.out.println("Fail on response validation: " + responseMessage);
                key.interestOps(SelectionKey.OP_WRITE);
            }
        } catch (IOException e) {
            System.out.println("Failed: " + requestMessage);
            key.interestOps(SelectionKey.OP_WRITE);
        }
        context.getBuffer().clear();
    }

    private interface ThrowingIntConsumer {
        void accept(int t) throws IOException;

        default IntConsumer toIntConsumer() {
            return t -> {
                try {
                    this.accept(t);
                } catch (IOException e) {
                    throw new UncheckedIOException(e);
                }
            };
        }
    }

    private static class ClientContext {
        private final Selector selector;
        private int unfinishedBranchCount;
        private final ByteBuffer buffer;
        private final int requestCount;
        private final String prefix;


        private ClientContext(Selector selector, ByteBuffer buffer, int unfinishedBranchCount, int requestCount, String prefix) {
            this.selector = selector;
            this.unfinishedBranchCount = unfinishedBranchCount;
            this.buffer = buffer;
            this.requestCount = requestCount;
            this.prefix = prefix;
        }

        private void countDown() {
            unfinishedBranchCount--;
        }

        private boolean isFinished() {
            return unfinishedBranchCount == 0;
        }

        private int getRequestCount() {
            return requestCount;
        }

        private Selector getSelector() {
            return selector;
        }

        private ByteBuffer getBuffer() {
            return buffer;
        }

        private String getPrefix() {
            return prefix;
        }
    }

    /**
     * Runs HelloClient realisation with given arguments
     *
     * @param args {@link #run(String, int, String, int, int)} arguments
     */
    public static void main(String[] args) {
        main(new HelloUDPNonblockingClient(), args);
    }
}
