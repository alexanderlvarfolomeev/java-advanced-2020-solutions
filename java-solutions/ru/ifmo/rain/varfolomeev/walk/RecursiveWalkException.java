package ru.ifmo.rain.varfolomeev.walk;

public class RecursiveWalkException extends Exception {
    public RecursiveWalkException(String message, Throwable cause) {
        super(message, cause);
    }

    public RecursiveWalkException(String message) {
        super(message);
    }
}
