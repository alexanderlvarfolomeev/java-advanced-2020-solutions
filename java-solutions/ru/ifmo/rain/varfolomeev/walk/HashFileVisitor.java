package ru.ifmo.rain.varfolomeev.walk;

import java.io.*;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

public class HashFileVisitor extends SimpleFileVisitor<Path> {
    private final static String HASH_OUTPUT_FORMAT = "%08x %s%n";
    final static int ZERO = 0;
    private final static int FNV0_HASH_VALUE = 0x811c9dc5;
    private final static int FNV_32_PRIME = 0x01000193;
    private final static int BITMAP = 0xff;
    private byte[] buff = new byte[1024];

    private final Writer outputWriter;

    HashFileVisitor(Writer outputWriter) {
        this.outputWriter = outputWriter;
    }

    @Override
    public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
        return writeHash(file, calculateHash(file, buff));
    }

    @Override
    public FileVisitResult visitFileFailed(Path file, IOException exc) throws IOException {
        return writeHash(file, ZERO);
    }

    private FileVisitResult writeHash(Path file, int hash) throws IOException {
        return writeHash(file.toString(), hash);
    }

    FileVisitResult writeHash(String file, int hash) throws IOException {
        try {
            outputWriter.write(String.format(HASH_OUTPUT_FORMAT, hash, file));
        } catch (IOException e) {
            throw new HashCalculationException(file, e);
        }
        return FileVisitResult.CONTINUE;
    }

    private static int calculateHash(Path file, byte[] buff) {
        int h = FNV0_HASH_VALUE;
        int bufferSize;
        try (InputStream inputStream = Files.newInputStream(file)) {
            while ((bufferSize = inputStream.read(buff)) != -1) {
                for (int i = 0; i < bufferSize; ++i) {
                    h = (h * FNV_32_PRIME) ^ (buff[i] & BITMAP);
                }
            }
        } catch (IOException e) {
            return ZERO;
        }
        return h;
    }
}
