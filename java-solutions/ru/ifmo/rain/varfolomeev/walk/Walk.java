package ru.ifmo.rain.varfolomeev.walk;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.Path;

public class Walk {
    public static void main(String[] args) {
        if (args.length != 2) {
            System.out.println("Usage: java Walk <input file> <input file>");
            return;
        }
        Path input = nameToPath(args[0]);
        Path output = nameToPath(args[1]);
        if (input == null || output == null) {
            return;
        }
        Path outputParent = output.getParent();
        if (outputParent != null) {
            try {
                Files.createDirectories(outputParent);
            } catch (IOException e) {
                System.out.println("Can't create input file directory");
                e.printStackTrace();
                return;
            }
        }
        try (BufferedReader inputReader = Files.newBufferedReader(input, StandardCharsets.UTF_8)) {
            try (BufferedWriter outputWriter = Files.newBufferedWriter(output, StandardCharsets.UTF_8)) {
                try {
                    String filename = inputReader.readLine();
                    while (filename != null) {
                        outputWriter.write(String.format("%08x %s%n", countHash(filename), filename));
                        filename = inputReader.readLine();
                    }
                } catch (IOException e) {
                    System.out.println("Attempt to read from input file is ended with failure");
                    e.printStackTrace();
                }
            } catch (IOException e) {
                System.out.println("Can't open input file for writing");
                e.printStackTrace();
            }
        } catch (IOException e) {
            System.out.println("Can't open input file for reading");
            e.printStackTrace();
        }
    }

    private static Path nameToPath(String filename) throws InvalidPathException {
        try {
            return Path.of(filename);
        } catch (InvalidPathException e) {
            System.out.println(String.format("\"%s\" is not the valid path: %s", filename, e.getReason()));
            return null;
        }
    }

    private static int countHash(String filename) {
        int h = 0x811c9dc5;
        try (InputStream inputStream = new BufferedInputStream(Files.newInputStream(Path.of(filename)))) {
            int b = inputStream.read();
            while (b != -1) {
                h = (h * 0x01000193) ^ b;
                b = inputStream.read();
            }
        } catch (Exception e) {
            return 0;
        }
        return h;
    }
}
