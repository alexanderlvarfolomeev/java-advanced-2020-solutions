package ru.ifmo.rain.varfolomeev.walk;

import java.io.IOException;
import java.nio.file.Path;

public class HashCalculationException extends IOException {

    HashCalculationException(String file, IOException e) {
        super(String.format("Attempt to calculate hash of file \"%s\" is ended with failure", file), e.getCause());
    }
}
