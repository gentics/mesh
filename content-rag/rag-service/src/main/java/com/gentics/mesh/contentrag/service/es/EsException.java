package com.gentics.mesh.contentrag.service.es;

/**
 * Unchecked-Wrapper fuer Fehler bei der Elasticsearch-Kommunikation.
 */
public class EsException extends RuntimeException {

    /**
     * @param message Fehlerbeschreibung
     * @param cause   urspruengliche Ursache
     */
    public EsException(String message, Throwable cause) {
        super(message, cause);
    }

    /**
     * @param message Fehlerbeschreibung
     */
    public EsException(String message) {
        super(message);
    }
}
