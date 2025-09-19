package org.example.audio_ecommerce.exception;

public class ExternalApiException extends RuntimeException {
    public ExternalApiException(String message) {
        super(message);
    }
    public ExternalApiException(String message, Throwable cause) {
        super(message, cause);
    }
}
