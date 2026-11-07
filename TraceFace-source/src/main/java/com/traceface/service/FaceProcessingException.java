package com.traceface.service;

/** Thrown when a face cannot be detected or encoded. */
public class FaceProcessingException extends Exception {
    private static final long serialVersionUID = 1L;

    public FaceProcessingException(String message) {
        super(message);
    }
}
