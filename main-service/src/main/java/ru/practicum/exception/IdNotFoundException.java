package ru.practicum.exception;

public class IdNotFoundException extends RuntimeException {
    public IdNotFoundException(final String message) {
        super(message);
    }
}