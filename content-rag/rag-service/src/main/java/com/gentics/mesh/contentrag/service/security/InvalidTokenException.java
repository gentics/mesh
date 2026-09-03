package com.gentics.mesh.contentrag.service.security;

/** Mesh hat das mitgelieferte Token abgelehnt. Fuehrt zu HTTP 401. */
public class InvalidTokenException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** @param message Beschreibung */
    public InvalidTokenException(String message) {
        super(message);
    }
}
