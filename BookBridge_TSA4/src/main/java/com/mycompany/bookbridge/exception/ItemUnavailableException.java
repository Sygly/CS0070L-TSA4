package com.mycompany.bookbridge.exception;

public class ItemUnavailableException
        extends RuntimeException {

    public ItemUnavailableException(String message) {
        super(message);
    }

    public ItemUnavailableException(
            String message, Throwable cause) {
        super(message, cause);
    }
}