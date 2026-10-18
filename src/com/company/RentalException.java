package com.company;

/** A single application-level error understood by the client. */
public class RentalException extends RuntimeException {
    private static final long serialVersionUID = 1L;

    public RentalException(String message, Throwable cause) {
        super(message, cause);
    }
}
