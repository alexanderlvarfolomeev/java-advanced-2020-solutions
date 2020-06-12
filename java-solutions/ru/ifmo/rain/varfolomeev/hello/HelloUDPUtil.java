package ru.ifmo.rain.varfolomeev.hello;

import java.net.DatagramPacket;
import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;

class HelloUDPUtil {
    static final int BUFFER_SIZE = 512;

    static int getIntArgument(String argumentName, String stringArgument) throws NumberFormatException {
        try {
            return Integer.parseInt(stringArgument);
        } catch (NumberFormatException e) {
            throw new NumberFormatException(
                    String.format("Can't parse argument '%s'. Found '%s'. %s",
                            argumentName, stringArgument, e.getMessage()));
        }
    }

    static String getStringFromBuffer(ByteBuffer buffer) {
        return new String(buffer.array(), buffer.position(), buffer.limit(), StandardCharsets.UTF_8);
    }

    static String getStringFromPacket(DatagramPacket packet) {
        return new String(packet.getData(), packet.getOffset(), packet.getLength(), StandardCharsets.UTF_8);
    }
}
