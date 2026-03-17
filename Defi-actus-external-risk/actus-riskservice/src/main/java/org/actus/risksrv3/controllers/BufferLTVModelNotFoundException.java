package org.actus.risksrv3.controllers;

/**
 * BufferLTVModelNotFoundException
 *
 * Custom exception thrown when a requested BufferLTVModel
 * cannot be found in the repository.
 */
public class BufferLTVModelNotFoundException extends RuntimeException {
    
    public BufferLTVModelNotFoundException(String message) {
        super(message);
    }
    
    public BufferLTVModelNotFoundException(String message, Throwable cause) {
        super(message, cause);
    }
}
