package com.gentics.mesh.contentrag.service.security;

/**
 * Mesh war fuer die Rollenaufloesung nicht erreichbar. Fuehrt zu HTTP 503.
 *
 * <p>Bewusst kein Rueckfall auf „keine Rollen": eine Antwort ohne geprueften Rollenkontext
 * waere zwar harmlos (sie faende nichts Geschuetztes), wuerde aber einen Ausfall als leeres
 * Suchergebnis tarnen. Ein 503 ist ehrlicher.
 */
public class MeshUnavailableException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message Beschreibung
     * @param cause   Ursache
     */
    public MeshUnavailableException(String message, Throwable cause) {
        super(message, cause);
    }
}
