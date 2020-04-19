package ru.ifmo.rain.varfolomeev.walk;

import java.io.*;
import java.nio.file.*;

public class RecursiveWalk {
    private final Path input;
    private final Path output;

    private RecursiveWalk(String inputName, String outputName) throws RecursiveWalkException {
        try {
            this.input = Path.of(inputName);
        } catch (NullPointerException e) {
            throw new RecursiveWalkException("Input path can't be null", e);
        } catch (InvalidPathException e) {
            throw new RecursiveWalkException("Invalid input path", e);
        }

        try {
            this.output = Path.of(outputName);
        } catch (NullPointerException e) {
            throw new RecursiveWalkException("Output path can't be null", e);
        } catch (InvalidPathException e) {
            throw new RecursiveWalkException("Invalid output path", e);
        }
    }

    private void createOutputParent() throws RecursiveWalkException {
        Path outputParent = output.getParent();
        if (outputParent != null) {
            try {
                Files.createDirectories(outputParent);
            } catch (IOException e) {
                throw new RecursiveWalkException("Can't create output file directory", e);
            }
        }
    }

    void run() throws RecursiveWalkException{
        createOutputParent();

        try (BufferedReader inputReader = Files.newBufferedReader(input)) {
            try (Writer outputWriter = Files.newBufferedWriter(output)) {
                try {
                    walk(inputReader, outputWriter);
                } catch (IOException e) {
                    throw new RecursiveWalkException("Exception was thrown during the file tree walk", e);
                }
            } catch (IOException e) {
                throw new RecursiveWalkException("Can't open output file for writing", e);
            }
        } catch (IOException e) {
            throw new RecursiveWalkException("Can't open input file for reading", e);
        }
    }

    private void walk(BufferedReader inputReader, Writer outputWriter) throws IOException {
        HashFileVisitor hashFileVisitor = new HashFileVisitor(outputWriter);
        String filename;
        while ((filename = inputReader.readLine()) != null) {
            try {
                Files.walkFileTree(Path.of(filename), hashFileVisitor);
            } catch (InvalidPathException e) {
                hashFileVisitor.writeHash(filename, HashFileVisitor.ZERO);
            }
        }
    }

    public static void main(String[] args) {
        try {
            if (args == null || args.length != 2) {
                throw new RecursiveWalkException("Usage: java Walk <input file> <output file>");
            }
            new RecursiveWalk(args[0], args[1]).run();
        } catch (RecursiveWalkException e) {
            System.out.println(e.getMessage());
        }
    }
}
